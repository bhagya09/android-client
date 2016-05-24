package com.bsb.hike.video;

import java.io.File;
import java.util.List;
import java.util.Locale;

import com.bsb.hike.utils.Logger;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.util.Matrix;
import com.googlecode.mp4parser.util.Path;

public class VideoUtilities {

	private final static String TAG = "VideoUtilities";
	public static VideoEditedInfo processOpenVideo(String videoPath) {
		VideoEditedInfo videoEditedInfo = null;
		float videoDuration = 0;
		TrackBox trackBox = null;
		MediaBox mediaBox = null;
		try {
			videoEditedInfo = new VideoEditedInfo();
			videoEditedInfo.startTime = -1;
			videoEditedInfo.endTime = -1;
			videoEditedInfo.originalPath = videoPath;
			IsoFile isoFile = new IsoFile(videoPath);
			List<Box> boxes = Path.getPaths(isoFile, "/moov/trak/");
			TrackHeaderBox trackHeaderBox = null;
            boolean isMp4A = true;

            Box boxTest = Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/mp4a/");
            if (boxTest == null) {
                isMp4A = false;
            }

            if (!isMp4A) {
                return null;
            }
            boxTest = Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/avc1/");
            if (boxTest == null) {
            	return null;
            }

			for (Box box : boxes) {
				Logger.d("Suyashhh", "box value = " + box.toString());
				trackBox = (TrackBox) box;
				long sampleSizes = 0;
				long trackBitrate = 0;
				try {
					mediaBox = trackBox.getMediaBox();
					MediaHeaderBox mediaHeaderBox = mediaBox
							.getMediaHeaderBox();
					SampleSizeBox sampleSizeBox = mediaBox
							.getMediaInformationBox().getSampleTableBox()
							.getSampleSizeBox();
					for (long size : sampleSizeBox.getSampleSizes()) {
						sampleSizes += size;
					}
					videoDuration = (float) mediaHeaderBox.getDuration()
							/ (float) mediaHeaderBox.getTimescale();
					trackBitrate = (int) (sampleSizes * 8 / videoDuration);
				} catch (Exception e) {
					Logger.d(TAG, "Exception" + e);
				}
				TrackHeaderBox headerBox = trackBox.getTrackHeaderBox();
				if (headerBox.getWidth() != 0 && headerBox.getHeight() != 0) {
					trackHeaderBox = headerBox;
					videoEditedInfo.bitrate = (int) (trackBitrate / 100000 * 100000);
					if (videoEditedInfo.bitrate > 900000) {
						videoEditedInfo.bitrate = 900000;
					}
				}
			}
			if (trackHeaderBox == null) {
				return null;
			}

			Matrix matrix = trackHeaderBox.getMatrix();
			if (matrix.equals(Matrix.ROTATE_90)) {
				videoEditedInfo.rotationValue = 90;
			} else if (matrix.equals(Matrix.ROTATE_180)) {
				videoEditedInfo.rotationValue = 180;
			} else if (matrix.equals(Matrix.ROTATE_270)) {
				videoEditedInfo.rotationValue = 270;
			}
			videoEditedInfo.resultWidth = videoEditedInfo.originalWidth = (int) trackHeaderBox
					.getWidth();
			videoEditedInfo.resultHeight = videoEditedInfo.originalHeight = (int) trackHeaderBox
					.getHeight();

			if (videoEditedInfo.resultWidth > 640
					|| videoEditedInfo.resultHeight > 640) {
				float scale = videoEditedInfo.resultWidth > videoEditedInfo.resultHeight ? 640.0f / videoEditedInfo.resultWidth
						: 640.0f / videoEditedInfo.resultHeight;
				videoEditedInfo.resultWidth *= scale;
				videoEditedInfo.resultHeight *= scale;
				videoEditedInfo.resultHeight = videoEditedInfo.resultHeight - (videoEditedInfo.resultHeight % 10);
				videoEditedInfo.resultWidth = videoEditedInfo.resultWidth - (videoEditedInfo.resultWidth % 10);
				if (videoEditedInfo.bitrate != 0) {
					videoEditedInfo.bitrate *= Math.max(0.5f, scale);
				}
			}
			videoEditedInfo.isCompRequired = true;
		} catch (Exception e) {
			Logger.d(TAG, "Exception" + e);
			return null;
		} finally {
			trackBox = null;
			mediaBox = null;
		}

		Logger.d(TAG, "VideoEditInfo = " + videoEditedInfo.toString());
		return videoEditedInfo;
	}

	public static class VideoEditedInfo {
		public long startTime;
		public long endTime;
		public int rotationValue;
		public int originalWidth;
		public int originalHeight;
		public int resultWidth;
		public int resultHeight;
		public int bitrate;
		public String originalPath;
		public File destFile;
		public boolean isCompRequired;

		public String getString() {
			return String.format(Locale.US, "-1_%d_%d_%d_%d_%d_%d_%d_%d_%s",
					startTime, endTime, rotationValue, originalWidth,
					originalHeight, bitrate, resultWidth, resultHeight,
					originalPath);
		}

		public void parseString(String string) {
			if (string.length() < 6) {
				return;
			}
			String args[] = string.split("_");
			if (args.length >= 10) {
				startTime = Long.parseLong(args[1]);
				endTime = Long.parseLong(args[2]);
				rotationValue = Integer.parseInt(args[3]);
				originalWidth = Integer.parseInt(args[4]);
				originalHeight = Integer.parseInt(args[5]);
				bitrate = Integer.parseInt(args[6]);
				resultWidth = Integer.parseInt(args[7]);
				resultHeight = Integer.parseInt(args[8]);
				for (int a = 9; a < args.length; a++) {
					if (originalPath == null) {
						originalPath = args[a];
					} else {
						originalPath += "_" + args[a];
					}
				}
			}
		}

		public String toString()
		{
			return "VideoEditorInfo{ " + " ,startTime =" + startTime  + " ,endTime =" + endTime  + " ,rotationValue =" + rotationValue  + " ,originalWidth =" + originalWidth
					+ " ,originalHeight =" + originalHeight  + " ,bitrate =" + bitrate  + " ,resultWidth =" + resultWidth  + " ,resultHeight =" + resultHeight + "}";
		}
	}
}
