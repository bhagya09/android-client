/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.actionbarsherlock.internal.nineoldandroids.animation;

import java.util.ArrayList;
import android.view.animation.Interpolator;

import com.actionbarsherlock.internal.nineoldandroids.animation.Keyframe.FloatKeyframe;

/**
 * This class holds a collection of FloatKeyframe objects and is called by ValueAnimator to calculate
 * values between those keyframes for a given animation. The class internal to the animation
 * package because it is an implementation detail of how Keyframes are stored and used.
 *
 * <p>This type-specific subclass of KeyframeSet, along with the other type-specific subclass for
 * int, exists to speed up the getValue() method when there is no custom
 * TypeEvaluator set for the animation, so that values can be calculated without autoboxing to the
 * Object equivalents of these primitive types.</p>
 */
@SuppressWarnings("unchecked")
class FloatKeyframeSet extends KeyframeSet {
    private float firstValue;
    private float lastValue;
    private float deltaValue;
    private boolean firstTime = true;

    public FloatKeyframeSet(FloatKeyframe... keyframes) {
        super(keyframes);
    }

    @Override
    public Object getValue(float fraction) {
        return getFloatValue(fraction);
    }

    @Override
    public FloatKeyframeSet clone() {
        ArrayList<Keyframe> keyframes = mKeyframes;
        int numKeyframes = mKeyframes.size();
        FloatKeyframe[] newKeyframes = new FloatKeyframe[numKeyframes];
        for (int i = 0; i < numKeyframes; ++i) {
            newKeyframes[i] = (FloatKeyframe) keyframes.get(i).clone();
        }
        FloatKeyframeSet newSet = new FloatKeyframeSet(newKeyframes);
        return newSet;
    }

    public float getFloatValue(float fraction) {
        if (mNumKeyframes == 2) {
            if (firstTime) {
                firstTime = false;
                firstValue = ((FloatKeyframe) mKeyframes.get(0)).getFloatValue();
                lastValue = ((FloatKeyframe) mKeyframes.get(1)).getFloatValue();
                deltaValue = lastValue - firstValue;
            }
            if (mInterpolator != null) {
                fraction = mInterpolator.getInterpolation(fraction);
            }
            if (mEvaluator == null) {
                return firstValue + fraction * deltaValue;
            } else {
                return ((Number)mEvaluator.evaluate(fraction, firstValue, lastValue)).floatValue();
            }
        }
        if (fraction <= 0f) {
            final FloatKeyframe prevKeyframe = (FloatKeyframe) mKeyframes.get(0);
            final FloatKeyframe nextKeyframe = (FloatKeyframe) mKeyframes.get(1);
            float prevValue = prevKeyframe.getFloatValue();
            float nextValue = nextKeyframe.getFloatValue();
            float prevFraction = prevKeyframe.getFraction();
            float nextFraction = nextKeyframe.getFraction();
            final /*Time*/Interpolator interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            float intervalFraction = (fraction - prevFraction) / (nextFraction - prevFraction);
            return mEvaluator == null ?
                    prevValue + intervalFraction * (nextValue - prevValue) :
                    ((Number)mEvaluator.evaluate(intervalFraction, prevValue, nextValue)).
                            floatValue();
        } else if (fraction >= 1f) {
            final FloatKeyframe prevKeyframe = (FloatKeyframe) mKeyframes.get(mNumKeyframes - 2);
            final FloatKeyframe nextKeyframe = (FloatKeyframe) mKeyframes.get(mNumKeyframes - 1);
            float prevValue = prevKeyframe.getFloatValue();
            float nextValue = nextKeyframe.getFloatValue();
            float prevFraction = prevKeyframe.getFraction();
            float nextFraction = nextKeyframe.getFraction();
            final /*Time*/Interpolator interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            float intervalFraction = (fraction - prevFraction) / (nextFraction - prevFraction);
            return mEvaluator == null ?
                    prevValue + intervalFraction * (nextValue - prevValue) :
                    ((Number)mEvaluator.evaluate(intervalFraction, prevValue, nextValue)).
                            floatValue();
        }
        FloatKeyframe prevKeyframe = (FloatKeyframe) mKeyframes.get(0);
        for (int i = 1; i < mNumKeyframes; ++i) {
            FloatKeyframe nextKeyframe = (FloatKeyframe) mKeyframes.get(i);
            if (fraction < nextKeyframe.getFraction()) {
                final /*Time*/Interpolator interpolator = nextKeyframe.getInterpolator();
                if (interpolator != null) {
                    fraction = interpolator.getInterpolation(fraction);
                }
                float intervalFraction = (fraction - prevKeyframe.getFraction()) /
                    (nextKeyframe.getFraction() - prevKeyframe.getFraction());
                float prevValue = prevKeyframe.getFloatValue();
                float nextValue = nextKeyframe.getFloatValue();
                return mEvaluator == null ?
                        prevValue + intervalFraction * (nextValue - prevValue) :
                        ((Number)mEvaluator.evaluate(intervalFraction, prevValue, nextValue)).
                            floatValue();
            }
            prevKeyframe = nextKeyframe;
        }
        // shouldn't get here
        return ((Number)mKeyframes.get(mNumKeyframes - 1).getValue()).floatValue();
    }

}

