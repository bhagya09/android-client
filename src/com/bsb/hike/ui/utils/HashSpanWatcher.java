package com.bsb.hike.ui.utils;

import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;

public class HashSpanWatcher implements TextWatcher
{
	EditText editText;

	boolean spanAdded;

	String hash;

	Object span;

	int color;

	int length;

	boolean applyRegex;

	public HashSpanWatcher(EditText editText, String hash, int color)
	{
		this.editText = editText;
		this.hash = hash;
		this.color = color;
		this.length = hash.length();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		Logger.i("hash", "start "+start +" count "+count);
		applyRegex = start >= 0 && start < length;
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		if (applyRegex)
		{
			Logger.i("hash", "applying regex");
			String text = s.toString();
			if (text.matches("(?i)" + hash + ".*"))
			{
				if (!spanAdded)
				{
					spanAdded = true;
					span = SpanUtil.getImageSpanFromTextView(editText.getContext(), R.layout.hash_text, R.id.text, hash);
					s.setSpan(span, 0, hash.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
			else
			{
				if (spanAdded)
				{
					spanAdded = false;
					s.removeSpan(span);
				}
			}

		}
	}

}
