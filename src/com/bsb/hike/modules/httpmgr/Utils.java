package com.bsb.hike.modules.httpmgr;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import com.bsb.hike.modules.httpmgr.engine.RequestProcessor;
import com.coremedia.iso.Hex;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class Utils
{
	private static final int BUFFER_SIZE = 4096;
	
	public static ThreadFactory threadFactory(final String name, final boolean daemon)
	{
		return new ThreadFactory()
		{
			private AtomicInteger i = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable runnable)
			{
				int threadCount = i.getAndIncrement();
				Thread result = new Thread(runnable);
				result.setName(name + "-" + threadCount);
				result.setDaemon(daemon);
				return result;
			}
		};
	}

	public static RejectedExecutionHandler rejectedExecutionHandler()
	{
		return new RejectedExecutionHandler()
		{
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
			{
				
			}
		};
	}
	
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
			if(header.getName().equalsIgnoreCase(headerString))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Closes {@code closeable}, ignoring any checked exceptions. Does nothing if {@code closeable} is null.
	 */
	public static void closeQuietly(Closeable closeable)
	{
		if (closeable != null)
		{
			try
			{
				closeable.close();
			}
			catch (RuntimeException rethrown)
			{
				throw rethrown;
			}
			catch (Exception ignored)
			{
			}
		}
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
			request.finish();
		}
		if (null != response)
		{
			response.finish();
		}
		RequestProcessor.removeRequest(request);
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
}
