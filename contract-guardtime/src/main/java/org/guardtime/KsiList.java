package org.guardtime;

import org.example.ledgerapi.State;
import org.example.ledgerapi.StateList;
import org.hyperledger.fabric.contract.Context;

import com.guardtime.ksi.unisignature.KSISignature;
import org.hyperledger.fabric.shim.ChaincodeException;
/*
SPDX-License-Identifier: Apache-2.0
*/

public class KsiList {
    private StateList stateList;

    public KsiList(Context ctx) {
        this.stateList = StateList.getStateList(ctx, "blocksig.ksi", KsiSignatureWrapper::deserialize);
    }

    public KsiList addKsiSignature(KsiSignatureWrapper ksi) {
        System.out.println("adding KSI signature:");
        // String key = State.makeKey(new String[]{org, Long.toString(blockNr)});
        
        // System.out.println("Getting state from: " + ksi.getBlockNumber());
        // State s = null;
        // try {
        //     s = stateList.getState(key);
        //     System.out.println("State retrieved: " + s);
        // } catch (Exception e) {;}
        
        // if (s != null) {
        //     System.out.println("Ah Du liebe - state ist nonsence: " + s);
        //     throw new ChaincodeException("KSI signature for this block already exists!");
        // }

        stateList.addState(ksi);
        return this;
    }


    public KsiSignatureWrapper getKsiSignature(long blockNr, String org) {
        String key = KsiSignatureWrapper.getKey(blockNr, org);
        return (KsiSignatureWrapper) this.stateList.getState(key);
    }

    public KsiList updateExtended(long blockNr, String org, KsiSignatureWrapper extended) {
        KSISignature prev = getKsiSignature(blockNr, org).getKsi();
        KSISignature ext = extended.getKsi();
        
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

        this.stateList.updateState(extended);
        return this;
    }
}
