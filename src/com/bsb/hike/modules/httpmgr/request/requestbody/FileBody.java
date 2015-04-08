/*
 * Copyright (C) 2010 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bsb.hike.modules.httpmgr.request.requestbody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

/**
 * File and its mime type.
 * 
 * @author anubhav & sidharth
 */
public class FileBody implements IRequestBody
{
	private static final int BUFFER_SIZE = 4096;

	private final String mimeType;

	private final File file;

	/**
	 * Constructs a new typed file.
	 * 
	 * @throws NullPointerException
	 *             if file or mimeType is null
	 */
	public FileBody(String mimeType, File file)
	{
		if (mimeType == null)
		{
			throw new NullPointerException("mimeType");
		}
		if (file == null)
		{
			throw new NullPointerException("file");
		}
		this.mimeType = mimeType;
		this.file = file;
	}

	/** Returns the file. */
	public File file()
	{
		return file;
	}

	@Override
	public String mimeType()
	{
		return mimeType;
	}

	@Override
	public long length()
	{
		return file.length();
	}

	@Override
	public String fileName()
	{
		return file.getName();
	}

	@Override
	public void writeTo(Request<?> request, OutputStream out) throws IOException
	{
		byte[] buffer = new byte[BUFFER_SIZE];
		FileInputStream in = new FileInputStream(file);
		float progress = 0;
		try
		{
			int read;
			while ((read = in.read(buffer)) != -1)
			{
				out.write(buffer, 0, read);
				progress += read;
				request.publishProgress(progress / length());
			}
		}
		finally
		{
			in.close();
		}
	}

	@Override
	public String toString()
	{
		return file.getAbsolutePath() + " (" + mimeType() + ")";
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o instanceof FileBody)
		{
			FileBody rhs = (FileBody) o;
			return file.equals(rhs.file);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return file.hashCode();
	}

	@Override
	public RequestBody getRequestBody()
	{
		return RequestBody.create(MediaType.parse(mimeType()), file());
	}
}
