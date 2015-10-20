package com.bsb.hike.models;

import java.io.File;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Utils;

public class FileListItem
{
	private int icon;

	private String title;

	private String subtitle = "";

	private String extension = "";

	private String mimeType;

	private boolean showThumbnail;

	private File file;
	
	private HikeSharedFile hikeSharedFile;
	
	public FileListItem(int icon, String title, String subtitle, String extension, String mimeType, boolean showThumbnail, File file)
	{
		this.icon = icon;
		
		this.title = title;
		
		this.subtitle = subtitle;
		
		this.extension = extension;
		
		this.mimeType = mimeType;
		
		this.showThumbnail = showThumbnail;
		
		this.file = file;
	}
	
	public FileListItem(HikeSharedFile hikeSharedFile)
	{
		this.hikeSharedFile = hikeSharedFile;
		
		setListItemAttributesFromFile(this, hikeSharedFile.getFileFromExactFilePath());
	}
	
	public FileListItem()
	{
		// TODO Auto-generated constructor stub
	}

	public int getIcon()
	{
		return icon;
	}

	public void setIcon(int icon)
	{
		this.icon = icon;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getSubtitle()
	{
		return subtitle;
	}

	public void setSubtitle(String subtitle)
	{
		this.subtitle = subtitle;
	}

	public String getExtension()
	{
		return extension;
	}

	public void setExtension(String extension)
	{
		this.extension = extension;
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
	}

	public boolean isShowThumbnail()
	{
		return showThumbnail;
	}

	public void setShowThumbnail(boolean showThumbnail)
	{
		this.showThumbnail = showThumbnail;
	}

	public File getFile()
	{
		return hikeSharedFile == null ? file : hikeSharedFile.getFileFromExactFilePath();
	}

	public void setFile(File file)
	{
		if(hikeSharedFile == null)
		{
			this.file = file;
		}
		else
		{
			hikeSharedFile.setFile(file);
		}
	}

	public HikeSharedFile getHikeSharedFile()
	{
		return hikeSharedFile;
	}

	public void setHikeSharedFile(HikeSharedFile hikeSharedFile)
	{
		this.hikeSharedFile = hikeSharedFile;
	}
	
	public void setListItemAttributesFromFile(FileListItem item, File file)
	{
		item.setFile(file);
		if(file == null)
		{
			return;
		}
		
	    HikeFileType hikeFileType = HikeFileType.fromFilePath(file.getAbsolutePath(),false );
	    
	    String fileName = file.getName();
	    
		if (hikeFileType == HikeFileType.APK)
		{
			if (item.getHikeSharedFile()!=null && !TextUtils.isEmpty((item.getHikeSharedFile().getFileName())))
			{
				fileName = item.getHikeSharedFile().getFileName();
			}
			else
			{
				fileName = file.getName();
			}
		}
	    
		item.setTitle(fileName);
		if (file.isDirectory())
		{
			item.setIcon(R.drawable.ic_folder);
		}
		else
		{
			String extension = Utils.getFileExtension(file.getName());
			item.setExtension(TextUtils.isEmpty(extension) ? "?" : extension);
			item.setSubtitle(Utils.formatFileSize(file.length()));
			item.setMimeType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(item.getExtension()));

			if (!TextUtils.isEmpty(item.getMimeType()) && HikeFileType.IMAGE == HikeFileType.fromString(item.getMimeType()))
			{
				item.setShowThumbnail(true);
			}
		}
	}

}