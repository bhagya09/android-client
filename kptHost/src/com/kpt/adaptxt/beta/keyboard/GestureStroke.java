/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.kpt.adaptxt.beta.keyboard;

import java.util.ArrayList;

import android.content.res.Resources;

import com.kpt.adaptxt.beta.R;

public class GestureStroke {
	public static final int DEFAULT_CAPACITY = 128;
	private final int mPointerId;
	private final ResizableIntArray mEventTimes = new ResizableIntArray(DEFAULT_CAPACITY);
	private final ResizableIntArray mXCoordinates = new ResizableIntArray(DEFAULT_CAPACITY);
	private final ResizableIntArray mYCoordinates = new ResizableIntArray(DEFAULT_CAPACITY);

	private ArrayList<Float> mCoreXCoords = new ArrayList<Float>();
	private ArrayList<Float> mCoreYCoords = new ArrayList<Float>();
	private final GestureStrokeParams mParams;

	// Static threshold for starting gesture detection
	private int mDetectFastMoveSpeedThreshold; // pixel /sec
	private int mDetectFastMoveTime;
	private int mDetectFastMoveX;
	private int mDetectFastMoveY;
	// Dynamic threshold for gesture after fast typing
	private boolean mAfterFastTyping;
	private int mGestureDynamicDistanceThresholdFrom; // pixel
	private int mGestureDynamicDistanceThresholdTo; // pixel
	// Variables for gesture sampling
	private int mGestureSamplingMinimumDistance; // pixel
	private long mLastMajorEventTime;
	private int mLastMajorEventX;
	private int mLastMajorEventY;
	// Variables for gesture recognition
	private int mGestureRecognitionSpeedThreshold; // pixel / sec
	private int mIncrementalRecognitionSize;
	private int mLastIncrementalBatchSize;

	public static final class GestureStrokeParams {
		// Static threshold for gesture after fast typing
		public final int mStaticTimeThresholdAfterFastTyping; // msec
		// Static threshold for starting gesture detection
		public final float mDetectFastMoveSpeedThreshold; // keyWidth/sec
		// Dynamic threshold for gesture after fast typing
		public final int mDynamicThresholdDecayDuration; // msec
		// Time based threshold values
		public final int mDynamicTimeThresholdFrom; // msec
		public final int mDynamicTimeThresholdTo; // msec
		// Distance based threshold values
		public final float mDynamicDistanceThresholdFrom; // keyWidth
		public final float mDynamicDistanceThresholdTo; // keyWidth
		// Parameters for gesture sampling
		public final float mSamplingMinimumDistance; // keyWidth
		// Parameters for gesture recognition
		public final int mRecognitionMinimumTime; // msec
		public final float mRecognitionSpeedThreshold; // keyWidth/sec

		// Default GestureStroke parameters for test.
		public static final GestureStrokeParams FOR_TEST = new GestureStrokeParams();
		public static final GestureStrokeParams DEFAULT = FOR_TEST;

		private GestureStrokeParams() {
			// These parameter values are default and intended for testing.
			mStaticTimeThresholdAfterFastTyping = 350; // msec
			mDetectFastMoveSpeedThreshold = 1.5f; // keyWidth / sec
			mDynamicThresholdDecayDuration = 450; // msec
			mDynamicTimeThresholdFrom = 300; // msec
			mDynamicTimeThresholdTo = 20; // msec
			mDynamicDistanceThresholdFrom = 6.0f; // keyWidth
			mDynamicDistanceThresholdTo = 0.35f; // keyWidth
			// The following parameters' change will affect the result of regression test.
			mSamplingMinimumDistance = 1.0f / 6.0f; // keyWidth
			mRecognitionMinimumTime = 100; // msec
			mRecognitionSpeedThreshold = 5.5f; // keyWidth / sec
		}

		public GestureStrokeParams(final Resources res) {
			final int base = 1;
			final int pbase = 100;

			mStaticTimeThresholdAfterFastTyping = res.getInteger(R.integer.kpt_config_gesture_static_time_threshold_after_fast_typing);

			mDetectFastMoveSpeedThreshold = res.getFraction(R.fraction.kpt_config_gesture_detect_fast_move_speed_threshold, base, pbase);
			mDynamicDistanceThresholdFrom = res.getFraction(R.fraction.kpt_config_gesture_dynamic_distance_threshold_from, base, pbase);
			mDynamicDistanceThresholdTo = res.getFraction(R.fraction.kpt_config_gesture_dynamic_distance_threshold_to, base, pbase);
			mSamplingMinimumDistance = res.getFraction(R.fraction.kpt_config_gesture_sampling_minimum_distance, base, pbase);
			mRecognitionSpeedThreshold = res.getFraction(R.fraction.kpt_config_gesture_recognition_speed_threshold, base, pbase);

			mDynamicThresholdDecayDuration = res.getInteger(R.integer.kpt_config_gesture_dynamic_threshold_decay_duration);
			mDynamicTimeThresholdFrom = res.getInteger(R.integer.kpt_config_gesture_dynamic_time_threshold_from);
			mDynamicTimeThresholdTo = res.getInteger(R.integer.kpt_config_gesture_dynamic_time_threshold_to);
			mRecognitionMinimumTime = res.getInteger(R.integer.kpt_config_gesture_recognition_minimum_time);

		}
	}

	private static final int MSEC_PER_SEC = 1000;

	public GestureStroke(final int pointerId, final GestureStrokeParams params) {
		mPointerId = pointerId;
		mParams = params;
	}

	public void setKeyboardGeometry(final int keyWidth) {
		// TODO: Find an appropriate base metric for these length. Maybe diagonal length of the key?
		mDetectFastMoveSpeedThreshold = (int)(keyWidth * mParams.mDetectFastMoveSpeedThreshold);
		mGestureDynamicDistanceThresholdFrom =(int)(keyWidth * mParams.mDynamicDistanceThresholdFrom);
		mGestureDynamicDistanceThresholdTo = (int)(keyWidth * mParams.mDynamicDistanceThresholdTo);
		mGestureSamplingMinimumDistance = (int)(keyWidth * mParams.mSamplingMinimumDistance);
		mGestureRecognitionSpeedThreshold = (int)(keyWidth * mParams.mRecognitionSpeedThreshold);

	}

	public void onDownEvent(final int x, final int y, final long downTime,
			final long gestureFirstDownTime, final long lastTypingTime) {
		reset();
		final long elapsedTimeAfterTyping = downTime - lastTypingTime;
		if (elapsedTimeAfterTyping < mParams.mStaticTimeThresholdAfterFastTyping) {
			mAfterFastTyping = true;
		}

		final int elapsedTimeFromFirstDown = (int)(downTime - gestureFirstDownTime);
		addPoint(x, y, elapsedTimeFromFirstDown, true /* isMajorEvent */);
	}

	private int getGestureDynamicDistanceThreshold(final int deltaTime) {
		if (!mAfterFastTyping || deltaTime >= mParams.mDynamicThresholdDecayDuration) {
			return mGestureDynamicDistanceThresholdTo;
		}
		final int decayedThreshold =
				(mGestureDynamicDistanceThresholdFrom - mGestureDynamicDistanceThresholdTo)
				* deltaTime / mParams.mDynamicThresholdDecayDuration;
		return mGestureDynamicDistanceThresholdFrom - decayedThreshold;
	}

	private int getGestureDynamicTimeThreshold(final int deltaTime) {
		if (!mAfterFastTyping || deltaTime >= mParams.mDynamicThresholdDecayDuration) {
			return mParams.mDynamicTimeThresholdTo;
		}
		final int decayedThreshold =
				(mParams.mDynamicTimeThresholdFrom - mParams.mDynamicTimeThresholdTo)
				* deltaTime / mParams.mDynamicThresholdDecayDuration;
		return mParams.mDynamicTimeThresholdFrom - decayedThreshold;
	}

	public final boolean isStartOfAGesture() {
		if (!hasDetectedFastMove()) {
			return false;
		}
		final int size = mEventTimes.getLength();
		if (size <= 0) {
			return false;
		}
		final int lastIndex = size - 1;
		final int deltaTime = mEventTimes.get(lastIndex) - mDetectFastMoveTime;
		if (deltaTime < 0) {
			return false;
		}
		final int deltaDistance = getDistance(mXCoordinates.get(lastIndex), mYCoordinates.get(lastIndex),
				mDetectFastMoveX, mDetectFastMoveY);
		final int distanceThreshold = getGestureDynamicDistanceThreshold(deltaTime);
		final int timeThreshold = getGestureDynamicTimeThreshold(deltaTime);
		final boolean isStartOfAGesture = deltaTime >= timeThreshold && deltaDistance >= distanceThreshold;

		return isStartOfAGesture;
	}

	protected void reset() {
		mIncrementalRecognitionSize = 0;
		mLastIncrementalBatchSize = 0;
		mEventTimes.setLength(0);
		mXCoordinates.setLength(0);
		mYCoordinates.setLength(0);
		mLastMajorEventTime = 0;
		mDetectFastMoveTime = 0;
		mAfterFastTyping = false;

		mCoreXCoords.clear();
		mCoreYCoords.clear();
	}

	private void appendPoint(final int x, final int y, final int time) {
		mEventTimes.add(time);
		mXCoordinates.add(x);
		mYCoordinates.add(y);
	}

	private void updateMajorEvent(final int x, final int y, final int time) {
		mLastMajorEventTime = time;
		mLastMajorEventX = x;
		mLastMajorEventY = y;
	}

	private final boolean hasDetectedFastMove() {
		return mDetectFastMoveTime > 0;
	}

	private int detectFastMove(final int x, final int y, final int time) {
		final int size = mEventTimes.getLength();
		final int lastIndex = size - 1;
		final int lastX = mXCoordinates.get(lastIndex);
		final int lastY = mYCoordinates.get(lastIndex);
		final int dist = getDistance(lastX, lastY, x, y);
		final int msecs = time - mEventTimes.get(lastIndex);
		if (msecs > 0) {
			final int pixels = getDistance(lastX, lastY, x, y);
			final int pixelsPerSec = pixels * MSEC_PER_SEC;

			// Equivalent to (pixels / msecs < mStartSpeedThreshold / MSEC_PER_SEC)
			if (!hasDetectedFastMove() && pixelsPerSec > mDetectFastMoveSpeedThreshold * msecs) {
				mDetectFastMoveTime = time;
				mDetectFastMoveX = x;
				mDetectFastMoveY = y;
			}
		}
		return dist;
	}

	public void addPoint(final int x, final int y, final int time, final boolean isMajorEvent) {
		final int size = mEventTimes.getLength();
		if (size <= 0) {
			// Down event
			appendPoint(x, y, time);
			updateMajorEvent(x, y, time);
		} else {
			final int distance = detectFastMove(x, y, time);
			if (distance > mGestureSamplingMinimumDistance) {
				appendPoint(x, y, time);
			}
		}
		if (isMajorEvent) {
			updateIncrementalRecognitionSize(x, y, time);
			updateMajorEvent(x, y, time);
		}
	}

	public void addPoint(final float x, final float y) {
		mCoreXCoords.add(x);
		mCoreYCoords.add(y);
	}

	public float[] getmFloatXCoords() {
		float[] arr = new float[mCoreXCoords.size()];
		for (int i = 0; i < mCoreXCoords.size(); i++) {
			arr[i] = mCoreXCoords.get(i);
		}
		return arr;
	}

	public float[] getmFloatYCoords() {
		float[] arr = new float[mCoreYCoords.size()];
		for (int i = 0; i < mCoreYCoords.size(); i++) {
			arr[i] = mCoreYCoords.get(i);
		}
		return arr;
	}

	private void updateIncrementalRecognitionSize(final int x, final int y, final int time) {
		final int msecs = (int)(time - mLastMajorEventTime);
		if (msecs <= 0) {
			return;
		}
		final int pixels = getDistance(mLastMajorEventX, mLastMajorEventY, x, y);
		final int pixelsPerSec = pixels * MSEC_PER_SEC;
		// Equivalent to (pixels / msecs < mGestureRecognitionThreshold / MSEC_PER_SEC)
		if (pixelsPerSec < mGestureRecognitionSpeedThreshold * msecs) {
			mIncrementalRecognitionSize = mEventTimes.getLength();
		}
	}

	public final boolean hasRecognitionTimePast(
			final long currentTime, final long lastRecognitionTime) {
		return currentTime > lastRecognitionTime + mParams.mRecognitionMinimumTime;
	}

	public final void appendAllBatchPoints(final InputPointers out) {
		appendBatchPoints(out, mEventTimes.getLength());
	}

	public final void appendIncrementalBatchPoints(final InputPointers out) {
		appendBatchPoints(out, mIncrementalRecognitionSize);
	}

	private void appendBatchPoints(final InputPointers out, final int size) {
		final int length = size - mLastIncrementalBatchSize;
		if (length <= 0) {
			return;
		}
		out.append(mPointerId, mEventTimes, mXCoordinates, mYCoordinates,
				mLastIncrementalBatchSize, length);
		mLastIncrementalBatchSize = size;
	}

	private static int getDistance(final int x1, final int y1, final int x2, final int y2) {
		final int dx = x1 - x2;
		final int dy = y1 - y2;
		// Note that, in recent versions of Android, FloatMath is actually slower than
		// java.lang.Math due to the way the JIT optimizes java.lang.Math.
		return (int)Math.sqrt(dx * dx + dy * dy);
	}
}
