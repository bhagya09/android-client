package com.bsb.hike.modules.stickerdownloadmgr;

public class StickerException extends Exception
{

	public static final short DIRECTORY_NOT_EXISTS = 0x01;
	
	public static final short NULL_OR_INVALID_RESPONSE = 0x02;
	
	public static final short OUT_OF_SPACE = 0x03;
	
	public static final short DIRECTORY_NOT_CREATED = 0x04;
	
	public static final short ERROR_CLOSING_FILE = 0x05;
	
	public static final short NULL_DATA = 0x06;
	
	public static final short EMPTY_CATEGORY_LIST = 0x07;
	
	public static final short NO_NETWORK = 0x08;

	
    private int errorCode;
    
    public StickerException(short errorCode)
    {
        super();
    	this.errorCode = errorCode;
    }
    
    public StickerException(String message)
    {
        this(0,message);
    }

    public StickerException(Throwable thr)
    {
        this(0,thr);
    }

    public StickerException(String errorMsg, Throwable thr)
    {
        this(0,errorMsg, thr);
    }
    
    public StickerException(int errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public StickerException(int errorCode, Throwable thr)
    {
        super(thr);
        this.errorCode = errorCode;
    }

    public StickerException(int errorCode, String errorMsg, Throwable thr)
    {
        super(errorMsg, thr);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode()
    {
        return errorCode;
    }    
}