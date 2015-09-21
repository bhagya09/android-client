/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.kpt.adaptxt.beta.keyboard;

import java.util.Arrays;

import android.os.Build;

public final class ProximityInfo {
	/** MAX_PROXIMITY_CHARS_SIZE must be the same as MAX_PROXIMITY_CHARS_SIZE_INTERNAL
	 * in defines.h */
	public static final int MAX_PROXIMITY_CHARS_SIZE = 16;
	/** Number of key widths from current touch point to search for nearest keys. */
	private static float SEARCH_DISTANCE = 1.2f;
	private static final Key[] EMPTY_KEY_ARRAY = new Key[0];

	private final int mGridWidth;
	private final int mGridHeight;
	private final int mGridSize;
	private final int mCellWidth;
	private final int mCellHeight;
	private final int mKeyboardMinWidth;
	private final int mKeyboardHeight;
	private final int mMostCommonKeyWidth;
	private final int mMostCommonKeyHeight;
	private Key[] mKeys;
	private final Key[][] mGridNeighbors;

	public ProximityInfo(final int gridWidth, final int gridHeight,
			final int minWidth, final int height, final int mostCommonKeyWidth,
			final int mostCommonKeyHeight, final Key[] keys) {

		mGridWidth = gridWidth;
		mGridHeight = gridHeight;
		mGridSize = mGridWidth * mGridHeight;
		mCellWidth = (minWidth + mGridWidth - 1) / mGridWidth;
		mCellHeight = (height + mGridHeight - 1) / mGridHeight;
		mKeyboardMinWidth = minWidth;
		mKeyboardHeight = height;
		mMostCommonKeyHeight = mostCommonKeyHeight;
		mMostCommonKeyWidth = mostCommonKeyWidth;
		mKeys = keys;
		mGridNeighbors = new Key[mGridSize][];
		if (minWidth == 0 || height == 0) {
			// No proximity required. Keyboard might be more keys keyboard.
			return;
		}
		computeNearestNeighbors();

	}

	public static ProximityInfo createDummyProximityInfo() {
		return new ProximityInfo( 1, 1, 1, 1, 1, 1, EMPTY_KEY_ARRAY);
	}


	public int getMostCommonKeyWidth(){
		return mMostCommonKeyWidth;
	}

	public int getMostCommonKeyHeight(){
		return mMostCommonKeyHeight;
	}


	private void computeNearestNeighbors() {
		final int defaultWidth = mMostCommonKeyWidth;
		final Key[] keys = mKeys;
		final int thresholdBase = (int) (defaultWidth * SEARCH_DISTANCE);
		final int threshold = thresholdBase * thresholdBase;
		// Round-up so we don't have any pixels outside the grid
		final Key[] neighborKeys = new Key[keys.length];
		final int gridWidth = mGridWidth * mCellWidth;
		final int gridHeight = mGridHeight * mCellHeight;
		
		for (int x = 0; x < gridWidth; x += mCellWidth) {
			for (int y = 0; y < gridHeight; y += mCellHeight) {
				final int centerX = x + mCellWidth / 2;
				final int centerY = y + mCellHeight / 2;
				int count = 0;
				for (final Key key : keys) {
					if (key.squaredDistanceToEdge(centerX, centerY) < threshold) {
						neighborKeys[count++] = key;
					}
				}
				if(Build.VERSION.SDK_INT < 9) {
					final Key[] cell = new Key[count];
	                System.arraycopy(neighborKeys, 0, cell, 0, count);	
	                mGridNeighbors[(y / mCellHeight) * mGridWidth + (x / mCellWidth)] = cell;
				} else {
					mGridNeighbors[(y / mCellHeight) * mGridWidth + (x / mCellWidth)] =
							Arrays.copyOfRange(neighborKeys, 0, count);
				}
			}
		}
	}

	public Key[] getNearestKeys(final int x, final int y) {
		if (mGridNeighbors == null) {
			return EMPTY_KEY_ARRAY;
		}
		if (x >= 0 && x < mKeyboardMinWidth && y >= 0 && y < mKeyboardHeight) {
			int index = (y / mCellHeight) * mGridWidth + (x / mCellWidth);
			if (index < mGridSize) {
				return mGridNeighbors[index];
			}
		}
		return EMPTY_KEY_ARRAY;
	}
	
	public String [] getIgonoredLeftKey(Key key, final int mStartX,final int mStartY ){
		String [] sendKeys = new String[4];
		if (mKeys != null && Arrays.asList(mKeys).contains(key)) {
			// means no more keys on left side
			Key rightKey = mKeys[Arrays.asList(mKeys).indexOf(key)+1];
			sendKeys[0] = key.label.toString() != null ? key.label.toString() : "null";
			sendKeys[1] = rightKey.label.toString() != null ? rightKey.label.toString() : "null";
			sendKeys[2] = Integer.toString(mStartX);
			sendKeys[3] = Integer.toString(mStartY);	
		}
		return sendKeys;
	}
	
	
	public String [] getIgonoredRightKey(Key key, final int mStartX,final int mStartY){
		String [] sendKeys = new String[4];
		if (mKeys != null && Arrays.asList(mKeys).contains(key)) {
			// means no more keys on right side
			Key leftKey = mKeys[Arrays.asList(mKeys).indexOf(key) - 1];
			sendKeys[0] = key.label.toString()!= null ? key.label.toString() : "null";
			sendKeys[1] = leftKey.label.toString() != null ? leftKey.label.toString() : "null";
			sendKeys[2] = Integer.toString(mStartX);
			sendKeys[3] = Integer.toString(mStartY);
		}
		return sendKeys;
	}
	
	public String [] getAllKey(Key key, final int mStartX,final int mStartY){
		String [] sendKeys = new String[5];
		if (mKeys != null && Arrays.asList(mKeys).contains(key)) {
			Key leftKey = mKeys[Arrays.asList(mKeys).indexOf(key) - 1];
			Key rightKey = mKeys[Arrays.asList(mKeys).indexOf(key) + 1];
			sendKeys[0] = leftKey.label.toString()!= null ? leftKey.label.toString() : "null";
			sendKeys[1] = key.label.toString()!= null ? key.label.toString() : "null";
			sendKeys[2] = rightKey.label.toString()!= null ? rightKey.label.toString() : "null";
			sendKeys[3] = Integer.toString(mStartX);
			sendKeys[4] = Integer.toString(mStartY);
		}
		return sendKeys;
	}

	/**
	 * this method is called when swap keys feature is used in keyboard customization.
	 * so all the nearest keys has to be updated 
	 * 
	 * @param keys the updated keys after swap key feature
	 */
	public void updateComputeNearestNeighbors(final Key[] keys) {
		mKeys = keys;
		computeNearestNeighbors();
	}
}
