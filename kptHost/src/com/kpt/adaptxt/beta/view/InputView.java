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

package com.kpt.adaptxt.beta.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class InputView extends LinearLayout {
	
	
    public InputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    
    @Override
    public boolean dispatchTouchEvent(MotionEvent me) {
    //	final int x = (int)me.getX();
     //   final int y = (int)me.getY();
     //   KPTLog.e("InputView:","dispatchTouchEvent():::::TOUCH X="+x+  "TOUCH Y"+y);
       return super.dispatchTouchEvent(me);
     
    }
    
}

