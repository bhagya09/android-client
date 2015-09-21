/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.kpt.adaptxt.beta.keyboard;

import java.util.List;

import com.kpt.adaptxt.beta.MainKeyboardView;

public final class MoreKeysDetector extends KeyDetector {
	private final int mSlideAllowanceSquare;
	private final int mSlideAllowanceSquareTop;

	public MoreKeysDetector(float slideAllowance) {
		super(/* keyHysteresisDistance */0);
		mSlideAllowanceSquare = (int)(slideAllowance * slideAllowance);
		// Top slide allowance is slightly longer (sqrt(2) times) than other edges.
		mSlideAllowanceSquareTop = mSlideAllowanceSquare * 2;
	}

	@Override
	public boolean alwaysAllowsSlidingInput() {
		return true;
	}

	@Override
	public Key detectHitKey(int x, int y) {

		final int touchX = getTouchX(x);
		final int touchY = getTouchY(y);

		Key nearestKey = null;
		int nearestDist = (y < 0) ? mSlideAllowanceSquareTop : mSlideAllowanceSquare;
		final List<Key> keys = getKeyboard().getKeys();

		/*if(keys == null || keys.size() == 0) {
			return nearestKey;
		}*/
		// #16641. Secondary characters are not available on PNH. --case 2- highlight the only char in the bubble
		if(keys.size() == 1) {
			nearestKey = keys.get(0);
			return nearestKey;
		}
			
		//if condition is only when mini keyboard is displayed
		if(MainKeyboardView.mShowfirstbublechar)  {
			//if is in left half highlight the first key
			//else highlight the last key
			if(MainKeyboardView.isRightHalf)  {
				if(keys.size() > 1) {
					nearestKey = keys.get(keys.size() - 1);
				}
			}else {
				if(keys.size() > 1) { //TP 15403
					nearestKey = keys.get(0);
				}
			}
		} else {
			for (final Key key : keys) {
				final int dist = key.squaredDistanceToEdge(touchX, touchY);
				if (dist < nearestDist) {
					nearestKey = key;
					nearestDist = dist;
				}
			}
		}
		return nearestKey;
	}
}
