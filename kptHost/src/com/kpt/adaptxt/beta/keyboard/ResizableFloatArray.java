/*
 * Copyright (C) 2012 The Android Open Source Project
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

// TODO: This class is not thread-safe.
public final class ResizableFloatArray {
	private float[] mArray;
	private int mLength;

	public ResizableFloatArray(final int capacity) {
		reset(capacity);
	}

	public float get(final int index) {
		if (index < mLength) {
			return mArray[index];
		}
		throw new ArrayIndexOutOfBoundsException("length=" + mLength + "; index=" + index);
	}

	public void add(final int index, final float val) {
		if (index < mLength) {
			mArray[index] = val;
		} else {
			mLength = index;
			add(val);
		}
	}

	public void add(final float val) {
		final int currentLength = mLength;
		ensureCapacity(currentLength + 1);
		mArray[currentLength] = val;
		mLength = currentLength + 1;
	}

	/**
	 * Calculate the new capacity of {@code mArray}.
	 * @param minimumCapacity the minimum capacity that the {@code mArray} should have.
	 * @return the new capacity that the {@code mArray} should have. Returns zero when there is no
	 * need to expand {@code mArray}.
	 */
	private int calculateCapacity(final int minimumCapacity) {
		final int currentCapcity = mArray.length;
		if (currentCapcity < minimumCapacity) {
			final int nextCapacity = currentCapcity * 2;
			// The following is the same as return Math.max(minimumCapacity, nextCapacity);
			return minimumCapacity > nextCapacity ? minimumCapacity : nextCapacity;
		}
		return 0;
	}

	private void ensureCapacity(final int minimumCapacity) {
		final int newCapacity = calculateCapacity(minimumCapacity);
		if (newCapacity > 0) {
			mArray = Arrays.copyOf(mArray, newCapacity);
		}
	}

	public int getLength() {
		return mLength;
	}

	public void setLength(final int newLength) {
		ensureCapacity(newLength);
		mLength = newLength;
	}

	public void reset(final int capacity) {
		mArray = new float[capacity];
		mLength = 0;
	}

	public float[] getPrimitiveArray() {
		return mArray;
	}

	public void set(final ResizableFloatArray ip) {
		mArray = ip.mArray;
		mLength = ip.mLength;
	}

	public void copy(final ResizableFloatArray ip) {
		final int newCapacity = calculateCapacity(ip.mLength);
		if (newCapacity > 0) {
			mArray = new float[newCapacity];
		}
		System.arraycopy(ip.mArray, 0, mArray, 0, ip.mLength);
		mLength = ip.mLength;
	}

	public void append(final ResizableFloatArray src, final int startPos, final int length) {
		if (length == 0) {
			return;
		}
		final int currentLength = mLength;
		final int newLength = currentLength + length;
		ensureCapacity(newLength);
		System.arraycopy(src.mArray, startPos, mArray, currentLength, length);
		mLength = newLength;
	}

	public void fill(final int value, final int startPos, final int length) {
		if (startPos < 0 || length < 0) {
			throw new IllegalArgumentException("startPos=" + startPos + "; length=" + length);
		}
		final int endPos = startPos + length;
		ensureCapacity(endPos);
		Arrays.fill(mArray, startPos, endPos, value);
		if (mLength < endPos) {
			mLength = endPos;
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mLength; i++) {
			if (i != 0) {
				sb.append(",");
			}
			sb.append(mArray[i]);
		}
		return "[" + sb + "]";
	}
}
