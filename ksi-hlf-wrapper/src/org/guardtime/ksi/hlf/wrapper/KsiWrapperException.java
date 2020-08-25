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