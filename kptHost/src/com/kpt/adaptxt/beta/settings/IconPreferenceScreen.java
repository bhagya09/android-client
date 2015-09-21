/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.kpt.adaptxt.beta.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kpt.adaptxt.beta.R;

public class IconPreferenceScreen extends Preference {

    private Drawable mIcon;

    public IconPreferenceScreen(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconPreferenceScreen(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.kpt_preference_icon);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.Kpt_IconPreferenceScreen, defStyle, 0);
        mIcon = a.getDrawable(R.styleable.Kpt_IconPreferenceScreen_kpt_kptIcon);
        
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        LinearLayout widgetFrameView = ((LinearLayout) view
                .findViewById(android.R.id.widget_frame));

        //This line fixed the visibility issue
        widgetFrameView.setVisibility(View.VISIBLE);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        if (imageView != null && mIcon != null) {
            imageView.setImageDrawable(mIcon);
        }
		//#14879. "Check for the latest version" string in Adaptxt settings view gets truncated.
        TextView titleTextView = (TextView)view.findViewById(android.R.id.title); 
        titleTextView.setMaxLines(2); 
        
        TextView summary= (TextView)view.findViewById(android.R.id.summary); 
      //Fix for TP item 13638
       // summary.setSelected(true);
      //  summary.setSingleLine(true);
      //  summary.setEllipsize(TruncateAt.MARQUEE);
      //  summary.setMarqueeRepeatLimit(100);
    }
}
