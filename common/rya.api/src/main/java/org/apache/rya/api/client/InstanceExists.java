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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Checks if an instance of Rya has been installed.
 */
@DefaultAnnotation(NonNull.class)
public interface InstanceExists {

    /**
     * Checks if an instance of Rya has been installed.
     *
     * @param instanceName - The name to check. (not null)
     * @return {@code true} If an instance of Rya exists with the provided name; otherwise {@code false}.
     * @throws RyaClientException Something caused the command to fail.
     */
    public boolean exists(String instanceName) throws RyaClientException;
}