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

package org.guardtime.ksi.hlf.wrapper;

public enum ErrCodeEnum {
    ERR_UNKNOWN(-1, "Unknown error"),
    /* Just and error. */
    ERR_GENERAL(0x01, "General error"),
    
    /* Error originating from KSI sdk. */
    ERR_KSI(0x02, "Error from KSI sdk"),
    
    // /* Provided data is empty. */
    // ERR_NODATA(0x03, "Input data is empty"),
    
    /* KSI Wrapper object version is not supported. */
    ERR_INVALID_OBJECT_VERSION(0x04, "JSON object has invalid version"),
    
    /* Improper json Data. */
    ERR_INVALID_JSON_OBJECT(0x05, "JSON object has invalid format"),
    
    // ERR_INVALID_ARG(0x06, "Invalid argument"),
    
    ERR_WRAP_VERIFICATION_FAILURE(0x07, "KSI Wrapper internal verification failed"),

    /* Unexpected error. That should never be happen. */
    ERR_UNEXPECTED(0xff, "Unexpected error");

    private String desc;
    private long code;

    private ErrCodeEnum(long code, String desc) {
        this.desc = desc;
        this.code = code;
    }

    @Override
    public String toString(){
        // return "ERR=0x" + Long.toHexString(code) + " " + desc;
        return desc;
    }
}