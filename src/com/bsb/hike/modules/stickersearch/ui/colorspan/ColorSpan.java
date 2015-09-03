package com.bsb.hike.modules.stickersearch.ui.colorspan;

import android.text.style.ForegroundColorSpan;

public class ColorSpan
{
	
	private ForegroundColorSpan colorSpan;
	
	private boolean marked;
	
	public ColorSpan(int color)
	{
		setColorSpan(new ForegroundColorSpan(color));
		setMarked(false);
	}

	public ForegroundColorSpan getColorSpan()
	{
		return colorSpan;
	}

	public void setColorSpan(ForegroundColorSpan colorSpan)
	{
		this.colorSpan = colorSpan;
	}

	public boolean isMarked()
	{
		return marked;
	}

	public void setMarked(boolean marked)
	{
		this.marked = marked;
	}
}
