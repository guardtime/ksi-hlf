/*
 * Copyright 2021 Guardtime, Inc.
 *
 * This file is part of the KSI-HLF integration toolkit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES, CONDITIONS, OR OTHER LICENSES OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * "Guardtime" and "KSI" are trademarks or registered trademarks of
 * Guardtime, Inc., and no license to trademarks is granted; Guardtime
 * reserves and retains all trademark rights.
 */

package org.guardtime.ksi.hlf.ledgerapi;

import org.json.JSONObject;
import org.json.JSONPropertyName;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class State {
    protected String key;

    @JSONPropertyName("key")
    public String getKey() {
        return key;
    }

    public String[] getSplitKey() {
        return key.split(".");
    }

    public abstract State parse(byte[] data) throws RuntimeException;
    
    public boolean isInit() {
        return !key.isEmpty();
    }

    public byte[] serialize() {
        System.out.println("Serializing: " + this.getClass().getName());
        String jsonStr = new JSONObject(this).toString();
        return jsonStr.getBytes(UTF_8);
    }

}

