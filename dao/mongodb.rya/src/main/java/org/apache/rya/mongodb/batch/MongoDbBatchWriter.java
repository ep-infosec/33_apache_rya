/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.rya.mongodb.batch;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.apache.rya.mongodb.batch.collection.CollectionType;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoBulkWriteException;

/**
 * Handles batch writing MongoDB statement objects to the repository. It takes
 * in a configurable batch size and flush time. If the number of objects placed
 * in the queue reaches the batch size then the objects are bulk written to the
 * datastore. Or if the queue has not filled up after the batch time duration
 * has passed then the statements are flushed out and written to the datastore.
 * @param <T> the type of object that the batch writer's internal collection
 * type uses.
 */
public class MongoDbBatchWriter<T> {
    private static final Logger log = Logger.getLogger(MongoDbBatchWriter.class);

    private static final int CHECK_QUEUE_INTERVAL_MS = 10;

    private final CollectionType<T> collectionType;
    private final long batchFlushTimeMs;

    private final ArrayBlockingQueue<T> statementInsertionQueue;
    private final ScheduledThreadPoolExecutor scheduledExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(0);
    private ScheduledFuture<?> flushBatchFuture;
    private final Runnable flushBatchTask;
    private Thread queueFullCheckerThread;

    private final AtomicBoolean isInit = new AtomicBoolean();

    /**
     * Creates a new instance of {@link MongoDbBatchWriter}.
     * @param collectionType the {@link CollectionType}. (not {@code null})
     * @param mongoDbBatchWriterConfig the {@link MongoDbBatchWriterConfig}.
     * (not {@code null})
     */
    public MongoDbBatchWriter(final CollectionType<T> collectionType, final MongoDbBatchWriterConfig mongoDbBatchWriterConfig) {
        this.collectionType = checkNotNull(collectionType);
        this.batchFlushTimeMs = checkNotNull(mongoDbBatchWriterConfig).getBatchFlushTimeMs();

        statementInsertionQueue = new ArrayBlockingQueue<>(mongoDbBatchWriterConfig.getBatchSize());
        flushBatchTask = new BatchFlusher();
    }

    /**
     * Task used to flush statements if enough time has passed without an
     * insertion while there are objects enqueued.
     */
    private class BatchFlusher implements Runnable {
        @Override
        public void run() {
            try {
                if (!statementInsertionQueue.isEmpty()) {
                    log.trace("Running statement insertion flush task. Too much time has passed without any object insertions so all queued data is being flushed.");
                    flush();
                }
            } catch (final Exception e) {
                log.error("Error flush out the statements", e);
            }
        }
    }

    private static final ThreadFactory QUEUE_THREAD_FACTORY = new ThreadFactoryBuilder()
        .setNameFormat("Queue Full Checker Thread - %d")
        .build();

    /**
     * Checks the queue for statements to insert if the queue is full.
     */
    private class QueueFullChecker implements Runnable {
        @Override
        public void run() {
            try {
                while (isInit.get()) {
                    // Check if the queue is full and if it is then insert the
                    // statements. Otherwise reset the insertion timer.
                    if (statementInsertionQueue.remainingCapacity() == 0) {
                        log.trace("Statement queue is FULL -> going to empty it");
                        try {
                            flush();
                        } catch (final MongoDbBatchWriterException e) {
                            log.error("Error emptying queue", e);
                        }
                    }
                    Thread.sleep(CHECK_QUEUE_INTERVAL_MS);
                }
            } catch (final InterruptedException e) {
                log.error("Encountered an unexpected error while checking the batch queue.", e);
            }
        }
    }

    /**
     * Starts the batch writer queue and processes.
     */
    public void start() throws MongoDbBatchWriterException {
        if (!isInit.get()) {
            if (flushBatchFuture == null) {
                flushBatchFuture = startFlushTimer();
            }
            if (queueFullCheckerThread == null) {
                queueFullCheckerThread = QUEUE_THREAD_FACTORY.newThread(new QueueFullChecker());
            }
            isInit.set(true);
            queueFullCheckerThread.start();
        }
    }

    /**
     * Stops the batch writer processes.
     */
    public void shutdown() throws MongoDbBatchWriterException {
        isInit.set(false);
        if (flushBatchFuture != null) {
            flushBatchFuture.cancel(true);
            flushBatchFuture = null;
        }
        if (queueFullCheckerThread != null) {
            if (queueFullCheckerThread.isAlive()) {
                try {
                    queueFullCheckerThread.join(2 * CHECK_QUEUE_INTERVAL_MS);
                } catch (final InterruptedException e) {
                    log.error("Error waiting for thread to finish", e);
                }
                queueFullCheckerThread = null;
            }
        }
    }

    /**
     * Adds a MongoDB object to the queue which will not be written until one of
     * the following occur:<br>
     * <ul>
     *  <li>The queue fills up</li>
     *  <li>The flush time has been reached</li>
     *  <li>A direct call to the {@link MongoDbBatchWriter#flush()} method
     *  has been made</li>
     * </ul>
     * @param object the object to add to the queue.
     * @throws IOException
     */
    public void addObjectToQueue(final T object) throws MongoDbBatchWriterException {
        if (object != null) {
            try {
                // Place in the queue which will bulk write after the specified
                // "batchSize" number of items have filled the queue or if more
                // than "batchFlushTimeMs" milliseconds have passed since the
                // last insertion.
                resetFlushTimer();
                statementInsertionQueue.put(object);
            } catch (final Exception e) {
                throw new MongoDbBatchWriterException("Error adding object to batch queue.", e);
            }
        }
    }

    /**
     * Adds a list of MongoDB objects to the queue which will not be written
     * until one of the following occur:<br>
     * <ul>
     *  <li>The queue fills up</li>
     *  <li>The flush time has been reached</li>
     *  <li>A direct call to the {@link MongoDbBatchWriter#flush()} method
     *  has been made</li>
     * </ul>
     * @param objects a {@link List} of objects to add to the queue.
     * @throws IOException
     */
    public void addObjectsToQueue(final List<T> objects) throws MongoDbBatchWriterException {
        if (objects != null) {
            for (final T object : objects) {
                addObjectToQueue(object);
            }
        }
    }

    /**
     * Flushes out statements that are in the queue.
     */
    public void flush() throws MongoDbBatchWriterException {
        final List<T> batch = new ArrayList<>();
        try {
            statementInsertionQueue.drainTo(batch);
            if (!batch.isEmpty()) {
                collectionType.insertMany(batch);
            }
        } catch (final DuplicateKeyException e) {
            log.warn(e); // Suppress the stack trace so log doesn't get flooded.
        } catch (final MongoBulkWriteException e) {
            if (e.getMessage().contains("duplicate key error")) {
                log.warn(e); // Suppress the stack trace so log doesn't get flooded.
            } else {
                throw new MongoDbBatchWriterException("Error flushing statements", e);
            }
        } catch (final Exception e) {
            throw new MongoDbBatchWriterException("Error flushing statements", e);
        }
    }

    private void resetFlushTimer() throws MongoDbBatchWriterException {
        flushBatchFuture.cancel(false);
        flushBatchFuture = startFlushTimer();
    }

    private ScheduledFuture<?> startFlushTimer() throws MongoDbBatchWriterException {
        try {
            return scheduledExecutor.schedule(flushBatchTask, batchFlushTimeMs, TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            throw new MongoDbBatchWriterException("Error starting batch flusher", e);
        }
    }
}