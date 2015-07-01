package com.bsb.hike.voip;

import com.bsb.hike.utils.Logger;

public class CircularByteBuffer {

	private final static int DEFAULT_SIZE = OpusWrapper.OPUS_FRAME_SIZE * 2 * 10;
	protected byte[] buffer;
	private volatile int readPosition = 0;
	private volatile int writePosition = 0;

	public CircularByteBuffer(){
        this (DEFAULT_SIZE);
    }
	
	public CircularByteBuffer(int size){
		buffer = new byte[size];
    }
	
	public void clear() {
		synchronized (this){
			readPosition = 0;
			writePosition = 0;
		}
	}

	public int getAvailable() {
		synchronized (this){
			int avail = available();
			return avail;
		}
	}

	public int getSpaceLeft() {
		synchronized (this){
			return spaceLeft();
		}
	}

	private int spaceLeft(){
		if (writePosition < readPosition){
			// any space between the first write and
			// the mark except one byte is available.
			// In this case it is all in one piece.
			return (readPosition - writePosition - 1);
		}
		// space at the beginning and end.
		return ((buffer.length - 1) - (writePosition - readPosition));
	}

	private int available(){
        if (readPosition <= writePosition){
            // any space between the first read and
            // the first write is available.  In this case i
            // is all in one piece.
            return (writePosition - readPosition);
        }
        // space at the beginning and end.
        return (buffer.length - (readPosition - writePosition));
    }
	
	public int read(byte[] cbuf) {
        return read(cbuf, 0, cbuf.length);
    }
	
	public int read(byte[] cbuf, int off, int len) {
		synchronized (CircularByteBuffer.this){
			int available = CircularByteBuffer.this.available();
			if (available > 0){
				int length = Math.min(len, available);
				int firstLen = Math.min(length, buffer.length - readPosition);
				int secondLen = length - firstLen;
				System.arraycopy(buffer, readPosition, cbuf, off, firstLen);
				if (secondLen > 0){
					System.arraycopy(buffer, 0, cbuf, off+firstLen,  secondLen);
					readPosition = secondLen;
				} else {
					readPosition += length;
				}
				if (readPosition == buffer.length) {
					readPosition = 0;
				}
				return length;
			} else {
				return -1;
			}
		}
	}
	
	public void write(byte[] cbuf) {
        write(cbuf, 0, cbuf.length);
    }
	
	public void write(byte[] cbuf, int off, int len) {
        while (len > 0){
            synchronized (CircularByteBuffer.this){
                int spaceLeft = spaceLeft();
                if (spaceLeft < len) {
                	Logger.e(VoIPConstants.TAG, "Buffer is full; cannot write.");
                	return;
                }
                int realLen = Math.min(len, spaceLeft);
                int firstLen = Math.min(realLen, buffer.length - writePosition);
                int secondLen = Math.min(realLen - firstLen, buffer.length - readPosition - 1);
                int written = firstLen + secondLen;
                if (firstLen > 0){
                    System.arraycopy(cbuf, off, buffer, writePosition, firstLen);
                }
                if (secondLen > 0){
                    System.arraycopy(cbuf, off+firstLen, buffer, 0, secondLen);
                    writePosition = secondLen;
                } else {
                    writePosition += written;
                }
                if (writePosition == buffer.length) {
                    writePosition = 0;
                }
                off += written;
                len -= written;
            }
        }
    }
	
}
