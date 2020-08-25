package org.guardtime.ksi.hlf.contract.api;

public class KsiContractException extends RuntimeException {
    public KsiContractException(String message) {
        super(message);
    }

    public KsiContractException(String message, Throwable cause) {
        super(message, cause);
    }
}

