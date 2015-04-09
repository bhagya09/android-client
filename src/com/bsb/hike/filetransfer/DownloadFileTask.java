package com.bsb.hike.filetransfer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FileTransferManager.NetworkType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class DownloadFileTask extends FileTransferBase
{
	private File tempDownloadedFile;

	private boolean showToast;

	protected DownloadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, File destinationFile, String fileKey, long msgId,
			HikeFileType hikeFileType, Object userContext, boolean showToast, String token, String uId)
	{
		super(handler, fileTaskMap, ctx, destinationFile, msgId, hikeFileType, token, uId);
		this.fileKey = fileKey;
		this.showToast = showToast;
		this.userContext = userContext;
		_state = FTState.INITIALIZED;
	}

	@Override
	public FTResult call()
	{
		if (_state == FTState.CANCELLED)
			return FTResult.CANCELLED;
		
		mThread  = Thread.currentThread();
		
		try
		{
			tempDownloadedFile = new File(FileTransferManager.getInstance(context).getHikeTempDir(), mFile.getName() + ".part");
			stateFile = new File(FileTransferManager.getInstance(context).getHikeTempDir(), mFile.getName() + ".bin." + msgId);
		}
		catch(NullPointerException e)
		{
			return FTResult.NO_SD_CARD;
		}

		Logger.d(getClass().getSimpleName(), "Instantiating download ....");
		RandomAccessFile raf = null;
		try
		{
			HikeFile hikeFile = ((ConvMessage)userContext).getMetadata().getHikeFiles().get(0);
			String downLoadUrl = hikeFile.getDownloadURL();
			boolean isCloudFrontURL = false;
			if(TextUtils.isEmpty(downLoadUrl))
				downLoadUrl = (AccountUtils.fileTransferBaseDownloadUrl + fileKey);
			else
				isCloudFrontURL = true;
				
			mUrl = new URL(downLoadUrl);
			if(isCloudFrontURL && AccountUtils.ssl)
				mUrl = new URL("https", mUrl.getHost(), mUrl.getPort(), mUrl.getFile());

			this.analyticEvents =  FTAnalyticEvents.getAnalyticEvents(FileTransferManager.getInstance(context).getAnalyticFile(hikeFile.getFile(), msgId));
			FileSavedState fst = FileTransferManager.getInstance(context).getDownloadFileState(mFile, msgId);
			/* represents this file is either not started or unrecovered error has happened */
			if (fst.getFTState().equals(FTState.NOT_STARTED) || fst.getFTState().equals(FTState.CANCELLED))
			{
				this.analyticEvents.mAttachementType = FTAnalyticEvents.DOWNLOAD_ATTACHEMENT;
				this.analyticEvents.mNetwork = FileTransferManager.getInstance(context).getNetworkTypeString();
				Logger.d(getClass().getSimpleName(), "File state : " + fst.getFTState());
				raf = new RandomAccessFile(tempDownloadedFile, "rw");
				// TransferredBytes should always be set. It might be need for calculating percentage
				setBytesTransferred(0);
				return downloadFile(0, raf, AccountUtils.ssl);
			}
			else if (fst.getFTState().equals(FTState.PAUSED) || fst.getFTState().equals(FTState.ERROR))
			{
				Logger.d(getClass().getSimpleName(), "File state : " + fst.getFTState());
				this.analyticEvents.mRetryCount += 1;
				raf = new RandomAccessFile(tempDownloadedFile, "rw");
				// Restoring the bytes transferred(downloaded) previously.
				setBytesTransferred((int) raf.length());
				// Bug Fix: 13029
				setFileTotalSize(fst.getTotalSize());
				if (_totalSize > 0)
					progressPercentage = (int) ((_bytesTransferred * 100) / _totalSize);
				return downloadFile(raf.length(), raf, AccountUtils.ssl);
			}
		}
		catch (MalformedURLException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid URL", e);
			return FTResult.DOWNLOAD_FAILED;
		}
		catch (FileNotFoundException e)
		{
			Logger.e(getClass().getSimpleName(), "No SD Card", e);
			return FTResult.NO_SD_CARD;
		}
		catch (IOException e)
		{
			Logger.e(getClass().getSimpleName(), "Error while downloding file", e);
			return FTResult.DOWNLOAD_FAILED;
		}
		return FTResult.DOWNLOAD_FAILED;
	}

	// we can extend this later to multiple download threads (if required)
	private FTResult downloadFile(long mStartByte, RandomAccessFile raf, boolean ssl)
	{
		long mStart = mStartByte;
		FTResult res = FTResult.SUCCESS;
		BufferedInputStream in = null;
		URLConnection conn = null;
		NetworkType networkType = FileTransferManager.getInstance(context).getNetworkType();
		chunkSize = networkType.getMinChunkSize();
		while (shouldRetry())
		{
			try
			{
				conn = initConn();
				// set the range of byte to download
				String byteRange = mStart + "-";
				try
				{
					conn.setRequestProperty("Cookie", "user=" + token + ";UID=" + uId);
					conn.setRequestProperty("Range", "bytes=" + byteRange);
					conn.setConnectTimeout(10000);
				}
				catch (Exception e)
				{

				}
				conn.connect();
				int resCode = ssl ? ((HttpsURLConnection) conn).getResponseCode() : ((HttpURLConnection) conn).getResponseCode();
				// Make sure the response code is in the 200 range.
				if (resCode == RESPONSE_BAD_REQUEST || resCode == RESPONSE_NOT_FOUND)
				{
					Logger.d(getClass().getSimpleName(), "Server response code is not in 200 range: " + resCode + "; fk:" + fileKey);
					error();
					return FTResult.FILE_EXPIRED;
				}
				else if (resCode / 100 != 2)
				{
					Logger.d(getClass().getSimpleName(), "Server response code is not in 200 range: " + resCode + "; fk:" + fileKey);
					error();
					res = FTResult.SERVER_ERROR;
				}
				else
				// everything is fine till this point
				{
					retry = true;
					reconnectTime = 0;
					retryAttempts = 0;
					// Check for valid content length.
					int contentLength = conn.getContentLength();
					String md5Hash = conn.getHeaderField(ETAG);
					if ((contentLength - raf.length()) > Utils.getFreeSpace())
					{
						closeStreams(raf, in);
						return FTResult.FILE_TOO_LARGE;
					}
					Logger.d(getClass().getSimpleName(), "bytes=" + byteRange);
					setFileTotalSize(contentLength + (int) mStart);

					long temp = _bytesTransferred;
					temp *= 100;
					temp /= _totalSize;
					progressPercentage = (int) temp;

					// get the input stream
					in = new BufferedInputStream(conn.getInputStream());

					// open the output file and seek to the start location
					raf.seek(mStart);
					setChunkSize();

					/*
					 * Safe check for the case where chunk size equals zero while calculating based on network and device memory.
					 * https://hike.fogbugz.com/default.asp?42482
					 */
					if(chunkSize <= 0)
					{
						FTAnalyticEvents.sendFTDevEvent(FTAnalyticEvents.DOWNLOAD_FILE_TASK, "Chunk size is less than or equal to 0, so setting it to default i.e. 100kb");
						chunkSize = DEFAULT_CHUNK_SIZE;
					}

					byte data[] = new byte[chunkSize];
					analyticEvents.saveAnalyticEvent(FileTransferManager.getInstance(context).getAnalyticFile(mFile, msgId));
					// while ((numRead = in.read(data, 0, chunkSize)) != -1)
					int numRead = 0;
					_state = FTState.IN_PROGRESS;
					sendBroadcast();
					do
					{
						int byteRead = 0;
						if (numRead == -1)
							break;

						while (byteRead < chunkSize)
						{
							numRead = in.read(data, byteRead, chunkSize - byteRead);
							if (numRead == -1)
								break;
							byteRead += numRead;
						}

						// write to buffer
						try
						{
							raf.write(data, 0, byteRead);
						}
						catch (IOException e)
						{
							Logger.e(getClass().getSimpleName(), "Exception", e);
							return FTResult.CARD_UNMOUNT;
						}
						Logger.d(getClass().getSimpleName(), "ChunkSize : " + chunkSize + "Bytes");
						setChunkSize();
						// ChunkSize is increased within the limits
//						chunkSize *= 2;
//						if (chunkSize > networkType.getMaxChunkSize())
//							chunkSize = networkType.getMaxChunkSize();
//						else if (chunkSize < networkType.getMinChunkSize())
//							chunkSize = networkType.getMinChunkSize();

						/*
						 * This chunk size should ideally be no more than 1/8 of the total memory available.
						 */
//						try
//						{
//							int maxMemory = (int) Runtime.getRuntime().maxMemory();
//							if (chunkSize > (maxMemory / 8))
//								chunkSize = maxMemory / 8;
//						}
//						catch (Exception e)
//						{
//							e.printStackTrace();
//						}
						// change buffer size
						data = new byte[chunkSize];
						// increase the startByte for resume later
						mStart += byteRead;
						// increase the downloaded size
						incrementBytesTransferred(byteRead);
						saveIntermediateProgress(null);
						progressPercentage = (int) ((_bytesTransferred * 100) / _totalSize);
						// showButton();
						if(_state != FTState.PAUSED)
							sendBroadcast();
					}
					while (_state == FTState.IN_PROGRESS);

					switch (_state)
					{
					case CANCELLED:
						Logger.d(getClass().getSimpleName(), "FT Cancelled");
						deleteTempFile();
						deleteStateFile();
						closeStreams(raf, in);
						return FTResult.CANCELLED;
					case IN_PROGRESS:
						//When downloading file from cloudfront, then we are getting extra quotes at start and end of md5. So need to remove the quotes
						md5Hash = removeExtraQuotes(md5Hash);
						Logger.d(getClass().getSimpleName(), "Server md5 : " + md5Hash);
						String file_md5Hash = Utils.fileToMD5(tempDownloadedFile.getPath());
						if (md5Hash != null)
						{
							Logger.d(getClass().getSimpleName(), "Phone's md5 : " + file_md5Hash);
							if (!md5Hash.equals(file_md5Hash))
							{
								Logger.d(getClass().getSimpleName(), "The md5's are not equal...Deleting the files...");
								sendCrcLog(file_md5Hash);
//								deleteTempFile();
//								deleteStateFile();
//								return FTResult.FAILED_UNRECOVERABLE;
							}

						}
						else
						{
							sendCrcLog(file_md5Hash);
//							deleteTempFile();
//							deleteStateFile();
//							return FTResult.FAILED_UNRECOVERABLE;
						}
						if (!tempDownloadedFile.renameTo(mFile)) // if failed
						{
							Logger.d(getClass().getSimpleName(), "FT failed");
							error();
							closeStreams(raf, in);
							return FTResult.READ_FAIL;
						}
						else
						{
							Logger.d(getClass().getSimpleName(), "FT Completed");
							// Added sleep to complete the progress.
							//TODO Need to remove sleep and implement in a better way to achieve the progress UX.
							Thread.sleep(300);
							// temp file is already deleted
							_state = FTState.COMPLETED;
							deleteStateFile();
							retry = false;
						}
						break;
					case PAUSED:
						_state = FTState.PAUSED;
						Logger.d(getClass().getSimpleName(), "FT PAUSED");
						saveFileState(null);
						analyticEvents.saveAnalyticEvent(FileTransferManager.getInstance(context).getAnalyticFile(mFile, msgId));
						retry = false;
						break;
					default:
						break;
					}
				}
			}
			catch (Exception e)
			{
				Logger.e(getClass().getSimpleName(), "FT Download error : " + e.getMessage());
				// here we should retry
				mStart = _bytesTransferred;
				// Is case id the task quits after making MAX attempts
				// the file state is saved
				if (retryAttempts >= MAX_RETRY_ATTEMPTS)
				{
					error();
					res = FTResult.DOWNLOAD_FAILED;
					retry = false;
				}
			}
//			catch (IOException e)
//			{
//				Logger.e(getClass().getSimpleName(), "FT error : " + e.getMessage());
//				if (e.getMessage() != null && (e.getMessage().contains(NETWORK_ERROR_1) || e.getMessage().contains(NETWORK_ERROR_2)))
//				{
//					// here we should retry
//					mStart = _bytesTransferred;
//					// Is case id the task quits after making MAX attempts
//					// the file state is saved
//					if (retryAttempts >= MAX_RETRY_ATTEMPTS)
//					{
//						error();
//						res = FTResult.DOWNLOAD_FAILED;
//					}
//				}
//				else
//				{
//					error();
//					res = FTResult.DOWNLOAD_FAILED;
//					retry = false;
//				}
//			}
//			catch (Exception e)
//			{
//				Logger.e(getClass().getSimpleName(), "FT error : " + e.getMessage());
//			}
		}
		if (res == FTResult.SUCCESS)
			res = closeStreams(raf, in);
		else
			closeStreams(raf, in);
		return res;
	}

	private FTResult closeStreams(RandomAccessFile raf, BufferedInputStream in)
	{
		if (raf != null)
		{
			try
			{
				raf.close();
			}
			catch (Exception e)
			{
				deleteTempFile();
				deleteStateFile();
				Logger.e(getClass().getSimpleName(), "Error while closing file", e);
				return FTResult.DOWNLOAD_FAILED;
			}
		}

		if (in != null)
		{
			try
			{
				in.close();
			}
			catch (Exception e)
			{
			}
		}
		return FTResult.SUCCESS;
	}

	private void sendBroadcast()
	{
		Logger.d(getClass().getSimpleName(), "sending progress to publish...");
		Intent intent = new Intent(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	private void error()
	{
		_state = FTState.ERROR;
		saveFileState(null);
	}

	private void deleteTempFile()
	{
		if (tempDownloadedFile != null && tempDownloadedFile.exists())
			tempDownloadedFile.delete();
	}
	
	private void sendCrcLog(String md5)
	{
		Utils.sendMd5MismatchEvent(mFile.getName(), fileKey, md5, _bytesTransferred, true);
	}

	protected void postExecute(FTResult result)
	{
		FileTransferManager.getInstance(context).removeTask(msgId);
		if (result == FTResult.SUCCESS)
		{
			if (mFile != null)
			{
				if (mFile.exists() && hikeFileType != HikeFileType.AUDIO_RECORDING)
				{
					// this is to refresh media library
					context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFile)));
				}
				if (HikeFileType.IMAGE == hikeFileType)
					HikeMessengerApp.getPubSub().publish(HikePubSub.PUSH_FILE_DOWNLOADED, (ConvMessage) userContext);
			}
		}
		else if (result != FTResult.PAUSED) // if no PAUSE
		{
			final int errorStringId = result == FTResult.FILE_TOO_LARGE ? R.string.not_enough_space : result == FTResult.CANCELLED ? R.string.download_cancelled
					: (result == FTResult.FILE_EXPIRED || result == FTResult.SERVER_ERROR) ? R.string.file_expire
							: result == FTResult.FAILED_UNRECOVERABLE ? R.string.download_failed_fatal : result == FTResult.CARD_UNMOUNT ? R.string.card_unmount
									: result == FTResult.NO_SD_CARD ? R.string.no_sd_card : R.string.download_failed;
			if (showToast)
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						Toast.makeText(context, errorStringId, Toast.LENGTH_SHORT).show();
					}
				});
			}
			if (mFile != null)
			{
				mFile.delete();
			}
		}
		// showButton();
		this.pausedProgress = -1;
		if(_state != FTState.PAUSED)
			sendBroadcast();
	}
	
	private String removeExtraQuotes(String mText){
		mText = mText.replace("\"", "");
		return mText;
	}
}
