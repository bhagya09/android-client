package com.hike.abtest.dataparser;

/**
 * Created by abhijithkrishnappa on 03/04/16.
 */
public class ParserException extends Exception {

    byte mErrorCode = ERROR_INVALID_PACKET;

    public static final byte ERROR_INVALID_PACKET = 1;
    public static final byte ERROR_FIELDS_MISSING = 2;
    public static final byte ERROR_INVALID_EXPERIMENT = 3;
    public static final byte ERROR_INVALID_VARIABLE = 4;
    public static final byte ERROR_INVALID_REQUEST = 5;

    public ParserException(String errorLog) {
        super(errorLog);
    }

    public ParserException(String errorLog, byte errorCode) {
        super(errorLog);
        mErrorCode = errorCode;
    }


    public byte getErrorCode() {
        return mErrorCode;
    }

    public void setErrorCode(byte errorCode) {
        this.mErrorCode = errorCode;
    }

}
