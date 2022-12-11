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
package org.apache.rya.prospector.utils;

/**
 * Constants used by the Prospector project.
 */
public class ProspectorConstants {
    /**
     * The name of the Count index.
     */
    public static final String COUNT = "count";

    /**
     * The Row ID within Accumulo for any metadata entry related to a Prospect run.
     */
    public static final String METADATA = "metadata";

    /**
     * This is the name of a Column Family within Accumulo that represents when
     * a Prospect run was performed.
     */
    public static final String PROSPECT_TIME = "prospectTime";

    public static final String DEFAULT_VIS = "U&FOUO";
    public static final byte[] EMPTY = new byte [0];

    //config properties
    public static final String PERFORMANT = "performant";

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String INSTANCE = "instance";
    public static final String ZOOKEEPERS = "zookeepers";
    public static final String MOCK = "mock";
}