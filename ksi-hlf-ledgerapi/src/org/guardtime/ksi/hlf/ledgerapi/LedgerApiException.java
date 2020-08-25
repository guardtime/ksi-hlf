package org.guardtime.ksi.hlf.ledgerapi;

public class LedgerApiException extends RuntimeException {
    public LedgerApiException(String message) {
        super(message);
    }

    public LedgerApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
