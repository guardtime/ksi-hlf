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