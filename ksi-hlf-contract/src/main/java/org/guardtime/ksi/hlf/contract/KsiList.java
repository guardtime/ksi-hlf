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

package org.guardtime.ksi.hlf.contract;

import org.guardtime.ksi.hlf.ledgerapi.StateList;
import org.guardtime.ksi.hlf.ledgerapi.State;
import org.guardtime.ksi.hlf.ledgerapi.LedgerApiException;
import org.guardtime.ksi.hlf.ledgerapi.LedgerApiNoDataException;
import org.hyperledger.fabric.contract.Context;

import java.io.InvalidObjectException;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.unisignature.KSISignature;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.guardtime.ksi.hlf.wrapper.KsiWrapper;

public class KsiList {
    private StateList stateList;

    public KsiList (Context ctx) {
        this.stateList = new StateList(ctx, "blocksig.ksi", KsiWrapper::new);
    }

    public KsiList addKsiSignature(KsiWrapper ksi) throws LedgerApiException {
        System.out.println("adding KSI signature:");
        stateList.setState(ksi);
        return this;
    }


    public KsiWrapper getKsiSignature(long blockNr, String org) throws LedgerApiException, LedgerApiNoDataException {
        String key = KsiWrapper.getKey(blockNr, org);
        return (KsiWrapper) this.stateList.getState(key);
    }

    public KsiList updateExtended(long blockNr, String org, KsiWrapper extended)  throws LedgerApiException, LedgerApiNoDataException {
        KsiWrapper prevSigwrap = getKsiSignature(blockNr, org);
        KSISignature prev = prevSigwrap.getKsi();
        KSISignature ext = extended.getKsi();
        KsiWrapper tmp = extended;

        // Check that input hashes do match.
        if (!prev.getInputHash().equals(ext.getInputHash())) {
            throw new ChaincodeException("Input hashes are not equal!");
        }
        
        // Check that aggregation time do match.
        if (!prev.getAggregationTime().equals(ext.getAggregationTime())) {
            throw new ChaincodeException("Aggregation times do not match!");
        }
        
        // Check if previous signature is already extended.
        // if (prev.isExtended()) {
        //     throw new ChaincodeException("Signature is already extended!");
        // }
        
        // Check that new signature is really extended.
        if (!ext.isExtended()) {
            throw new ChaincodeException("Signature supposed to be extended is not!");
        }

        if (tmp.getRecHash() == null || tmp.getRecHash().length == 0) {
            tmp = KsiWrapper.newFromBase64(tmp.getKsiBase64(), prevSigwrap.getRecHash(), blockNr, org);
        }

        this.stateList.setState(tmp);
        return this;
    }

    public String getFullKey(String key) {
        return stateList.getFullKey(key);
    } 
}
