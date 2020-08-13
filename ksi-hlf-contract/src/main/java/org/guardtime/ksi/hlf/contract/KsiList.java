package org.guardtime.ksi.hlf.contract;

import org.example.ledgerapi.StateList;
import org.hyperledger.fabric.contract.Context;

import com.guardtime.ksi.unisignature.KSISignature;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.guardtime.ksi.hlf.wrapper.KsiWrapper;
/*
SPDX-License-Identifier: Apache-2.0
*/

public class KsiList {
    private StateList stateList;

    public KsiList(Context ctx) {
        this.stateList = StateList.getStateList(ctx, "blocksig.ksi", KsiWrapper::deserialize);
    }

    public KsiList addKsiSignature(KsiWrapper ksi) {
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


    public KsiWrapper getKsiSignature(long blockNr, String org) {
        String key = KsiWrapper.getKey(blockNr, org);
        return (KsiWrapper) this.stateList.getState(key);
    }

    public KsiList updateExtended(long blockNr, String org, KsiWrapper extended) {
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

        this.stateList.updateState(tmp);
        return this;
    }

    public String getFullKey(String key) {
        return stateList.getKey(key);
    } 
}
