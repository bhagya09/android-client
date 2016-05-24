package com.bsb.hike.modules.httpmgr;

import android.text.TextUtils;

import com.bsb.hike.modules.httpmgr.engine.RequestProcessor;
import com.coremedia.iso.Hex;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

public class HttpUtils
{
	private static final int BUFFER_SIZE = 4096;
	
	public static byte[] streamToBytes(InputStream stream) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		long time = System.currentTimeMillis();
		if (stream != null)
		{
			byte[] buf = new byte[BUFFER_SIZE];
			int r, count = 0;

			while ((r = stream.read(buf)) != -1)
			{
				count++;
				baos.write(buf, 0, r);
			}
			System.out.println(" stream to bytes while loop count : " + count + "   time : " + (System.currentTimeMillis() - time));
		}
		System.out.println(" stream to bytes method time : " + (System.currentTimeMillis() - time));
		return baos.toByteArray();
	}
	
	public static boolean containsHeader(List<Header> headers, String headerString)
	{
		for(Header header : headers)
		{
			if (!TextUtils.isEmpty(header.getName()) && header.getName().equalsIgnoreCase(headerString))
			{
				return true;
			}
		}
		return false;
	}

	public static Header getHeader(List<Header> headers, String headerString)
	{
		for(Header header : headers)
		{
			if (!TextUtils.isEmpty(header.getName()) && header.getName().equalsIgnoreCase(headerString))
			{
				return header;
			}
		}
		return null;
	}

    public static String requestToString(Request request)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("{" + "\n");
		builder.append("url : " + request.urlString() + "\n");
		builder.append("method : " + request.method() + "\n");
		builder.append("headers : " + request.headers().toString() + "\n");
		builder.append("}");
		return builder.toString();
	}
	
	public static String responseToString(Response response)
	{
		StringBuilder builder = new StringBuilder(); 
		builder.append("{" + "\n");
		builder.append("Url : " + response.request().urlString() + "\n");
		builder.append("protocol : " + response.protocol() + " \n");
		builder.append("code : " + response.code() + " \n");
		builder.append("message : " + response.message() + " \n");
		builder.append("headers : " + response.headers().toString());
		
		long time =  Long.parseLong(response.header("OkHttp-Received-Millis")) - Long.parseLong(response.header("OkHttp-Sent-Millis"));
		
		builder.append("total time : " + time + " milliseconds \n");
		builder.append("}");
		return builder.toString();
	}

	public static void finish(com.bsb.hike.modules.httpmgr.request.Request<?> request, com.bsb.hike.modules.httpmgr.response.Response response)
	{
		if (null != request)
		{
			RequestProcessor.removeRequest(request);
			request.finish();
		}
		if (null != response)
		{
			response.finish();
		}
	}
	
	public static String calculateMD5hash(String input)
	{
		String output = null;
		try
		{
			byte[] bytesOfMessage = input.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] thedigest = md.digest(bytesOfMessage);
			output = new String(Hex.encodeHex(thedigest));
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return output;
	}
	
	public static String calculateMD5hash(byte[] input)
	{
		String output = null;
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] thedigest = md.digest(input);
			output = new String(Hex.encodeHex(thedigest));
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return output;
	}
	
	public static boolean removeHeader(List<Header> headers, String name, String value)
	{
		if(headers == null)
		{
			return false;
		}
		
		for (Iterator<Header> iterator = headers.iterator(); iterator.hasNext();) {
		    Header header = iterator.next();
		    if (header.getName().equals(name) && header.getValue().equals(value)) {
		        // Remove the current element from the iterator and the list.
		        iterator.remove();
		        return true;
		    }
		}
		return false;
	}
}
