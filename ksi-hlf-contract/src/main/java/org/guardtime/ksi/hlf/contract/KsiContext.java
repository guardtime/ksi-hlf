package org.guardtime.ksi.hlf.contract;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;

class KSIContext extends Context {

    public KSIContext(ChaincodeStub stub) {
        super(stub);
        this.ksiList = new KsiList(this);
    }

    public KsiList ksiList;

}
