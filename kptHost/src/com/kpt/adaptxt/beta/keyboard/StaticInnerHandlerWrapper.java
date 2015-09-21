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

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Looper;

public class StaticInnerHandlerWrapper<T> extends Handler {
	final private WeakReference<T> mOuterInstanceRef;

	public StaticInnerHandlerWrapper(T outerInstance) {
		super();
		if (outerInstance == null) throw new NullPointerException("outerInstance is null");
		mOuterInstanceRef = new WeakReference<T>(outerInstance);
	}

	public StaticInnerHandlerWrapper(T outerInstance, Looper looper) {
		super(looper);
		if (outerInstance == null) throw new NullPointerException("outerInstance is null");
		mOuterInstanceRef = new WeakReference<T>(outerInstance);
	}

	public T getOuterInstance() {
		return mOuterInstanceRef.get();
	}
}
