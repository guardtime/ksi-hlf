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

public class KsiWrapperException extends RuntimeException {
    private ErrCodeEnum err;

    public KsiWrapperException(String message) {
        super(message);
        this.err = ErrCodeEnum.ERR_GENERAL;
    }
    
    public KsiWrapperException(String message, ErrCodeEnum err) {
        super(message);
        this.err = err;
    }
    

    public KsiWrapperException(String message, Throwable cause) {
        super(message, cause);
        this.err = ErrCodeEnum.ERR_GENERAL;
    }    

    public KsiWrapperException(String message, ErrCodeEnum err, Throwable cause) {
        super(message, cause);
        this.err = err;
    }    

    @Override
    public String getMessage() {
        return "(" + this.err  + "): " + super.getMessage();
    }

    public ErrCodeEnum getErr() {
        return this.err;
    }
}