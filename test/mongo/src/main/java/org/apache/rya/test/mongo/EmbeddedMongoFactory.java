/**
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
package org.apache.rya.test.mongo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;

public class EmbeddedMongoFactory {
    private static Logger logger = LoggerFactory.getLogger(EmbeddedMongoFactory.class.getName());

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 27017;

    public static EmbeddedMongoFactory newFactory() throws IOException {
        return EmbeddedMongoFactory.with(Version.Main.PRODUCTION);
    }

    public static EmbeddedMongoFactory with(final IFeatureAwareVersion version) throws IOException {
        return new EmbeddedMongoFactory(version);
    }

    private final MongodExecutable mongodExecutable;
    private final MongodProcess mongodProcess;

    /**
     * Create the testing utility using the specified version of MongoDB.
     *
     * @param version
     *            version of MongoDB.
     */
    private EmbeddedMongoFactory(final IFeatureAwareVersion version) throws IOException {
        final MongodStarter runtime = MongodStarter.getInstance(new RuntimeConfigBuilder().defaultsWithLogger(Command.MongoD, logger).build());
        mongodExecutable = runtime.prepare(newMongodConfig(version));
        mongodProcess = mongodExecutable.start();
    }

    private IMongodConfig newMongodConfig(final IFeatureAwareVersion version) throws UnknownHostException, IOException {
        final Net net = new Net(setPortToDefaultOrRandomOpen(), false);
        return new MongodConfigBuilder().version(version).net(net).build();
    }

    private static int setPortToDefaultOrRandomOpen() throws IOException {
        return setPortToDefaultOrRandomOpen(DEFAULT_PORT);
    }

    private static int setPortToDefaultOrRandomOpen(final int defaultPort) throws IOException {
        if (isPortAvailable(defaultPort)) {
            return defaultPort;
        } else {
            return findRandomOpenPortOnAllLocalInterfaces();
        }
    }

    private static boolean isPortAvailable(final int port) {
        try (final ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName(DEFAULT_HOST), port), 1);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    private static int findRandomOpenPortOnAllLocalInterfaces() throws IOException {
        try (final ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * @return A new Mongo client that is connected to the embedded MongoDB Server.
     * @throws UnknownHostException The hostname used was unknown.
     * @throws MongoException Couldn't create the client.
     */
    public MongoClient newMongoClient() throws UnknownHostException, MongoException {
        return new MongoClient(new ServerAddress(mongodProcess.getConfig().net().getServerAddress(), mongodProcess.getConfig().net().getPort()));
    }

    /**
     * @return The process configuration.
     */
    public IMongodConfig getMongoServerDetails() {
        return mongodProcess.getConfig();
    }

    /**
     * Cleans up the resources created by the utility.
     */
    public void shutdown() {
        mongodProcess.stop();
        mongodExecutable.stop();
    }
}