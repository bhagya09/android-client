package com.bsb.hike.utils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipUtil 
{
	/**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Extracts a file from a zip to specified destination directory.
     * The path of the file inside the zip is discarded, the file is
     * copied directly to the destDirectory.
     * @param zipFilePath - path and file name of a zip file
     * @param inZipFilePath - path and file name inside the zip
     * @param destDirectory - directory to which the file from zip should be extracted, the path part is discarded.
     * @throws java.io.IOException
     */
    public static void extractFile(String zipFilePath, String inZipFilePath, String destDirectory) throws IOException  
    {
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) 
        {
        	if (!entry.isDirectory() && inZipFilePath.equals(entry.getName())) 
        	{
                String filePath = entry.getName();
                int separatorIndex = filePath.lastIndexOf(File.separator);
                if (separatorIndex > -1)
                    filePath = filePath.substring(separatorIndex + 1, filePath.length());
                filePath = destDirectory + File.separator + filePath;
                extractFile(zipIn, filePath);
                break;
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws java.io.IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException 
    {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) 
        {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}
