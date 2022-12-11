/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.rya.api.client;

/**
 * Deletes and instance of a Periodic PCJ from Rya
 */
public interface DeletePeriodicPCJ {

    /**
     * Deletes a PCJ from an instance of Rya.
     *
     * @param instanceName - Indicates which Rya instance is maintaining the Periodic PCJ. (not null)
     * @param pcjId - The ID of the Periodic PCJ that will be deleted. (not null)
     * @param topic - Kafka topic for deleteing PeriodicNotifications
     * @param brokers - Comma delimited host/port pairs for connecting to Kafka brokers.
     * @throws InstanceDoesNotExistException No instance of Rya exists for the provided name.
     * @throws RyaClientException Something caused the command to fail.
     */
    public void deletePeriodicPCJ(String instanceName, final String pcjId, String topic, String brokers) throws InstanceDoesNotExistException, RyaClientException;
    
}
