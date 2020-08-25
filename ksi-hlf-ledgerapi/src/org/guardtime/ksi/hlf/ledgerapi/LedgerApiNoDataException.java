package org.guardtime.ksi.hlf.ledgerapi;

public class LedgerApiNoDataException extends RuntimeException {
    public LedgerApiNoDataException(String message) {
        super(message);
    }

    public LedgerApiNoDataException(String message, Throwable cause) {
        super(message, cause);
    }
}