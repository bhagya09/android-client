package com.bsb.hike.modules.stickersearch.ui.colorspan;

import java.util.ArrayList;
import java.util.List;

import android.text.style.ForegroundColorSpan;

public class ColorSpanPool
{	
	private List<ColorSpan> highlightColorSpans;
	
	private List<ColorSpan> unHighlightColorSpans;
	
	private int highlightColor, unHighlightColor;
	
	private final int INITIAL_POOL_SIZE = 10;
	
	public ColorSpanPool(int highlightColor, int unHighlightColor)
	{
		this(highlightColor, unHighlightColor, -1);
	}
	
	public ColorSpanPool(int highlightColor, int unHighlightColor, int poolSize)
	{
		if(poolSize == -1)
		{
			poolSize = INITIAL_POOL_SIZE;
		}
		
		highlightColorSpans = new ArrayList<>(poolSize);
		unHighlightColorSpans =  new ArrayList<>(poolSize);
		
		this.highlightColor = highlightColor;
		this.unHighlightColor = unHighlightColor;
		initialise(poolSize);
	}
	
	private void initialise(int poolSize)
	{
		for (int i = 0; i < poolSize; i++)
		{
			highlightColorSpans.add(new ColorSpan(highlightColor));
			unHighlightColorSpans.add(new ColorSpan(unHighlightColor));
		}
	}
	
	public void markAll()
	{
		markAllHighlightSpans();
		markAllUnHighlightSpans();
	}
	
	public void unMarkAll()
	{
		unMarkAllHighlightSpans();
		unMarkAllUnHighlightSpans();
	}
	
	public void markAllHighlightSpans()
	{
		for(ColorSpan highlightColorSpan : highlightColorSpans)
		{
			highlightColorSpan.setMarked(true);
		}
	}
	
	public void markAllUnHighlightSpans()
	{
		for(ColorSpan unHighlightColorSpan : unHighlightColorSpans)
		{
			unHighlightColorSpan.setMarked(true);
		}
	}
	
	public void unMarkAllHighlightSpans()
	{
		for(ColorSpan highlightColorSpan : highlightColorSpans)
		{
			highlightColorSpan.setMarked(false);
		}
	}
	
	public void unMarkAllUnHighlightSpans()
	{
		for(ColorSpan unHighlightColorSpan : unHighlightColorSpans)
		{
			unHighlightColorSpan.setMarked(false);
		}
	}
	
	public ForegroundColorSpan getHighlightSpan()
	{
		for(ColorSpan highlightColorSpan : highlightColorSpans)
		{
			if(!highlightColorSpan.isMarked())
			{
				highlightColorSpan.setMarked(true);
				return highlightColorSpan.getColorSpan();
			}
		}
		
		ColorSpan colorSpan = getNewColorSpan(highlightColor);
		highlightColorSpans.add(colorSpan);
		colorSpan.setMarked(true);
		return colorSpan.getColorSpan();
	}
	
	public ForegroundColorSpan getUnHighlightSpan()
	{
		for(ColorSpan unHighlightColorSpan : unHighlightColorSpans)
		{
			if(!unHighlightColorSpan.isMarked())
			{
				unHighlightColorSpan.setMarked(true);
				return unHighlightColorSpan.getColorSpan();
			}
		}
		
		ColorSpan colorSpan = getNewColorSpan(unHighlightColor);
		unHighlightColorSpans.add(colorSpan);
		colorSpan.setMarked(true);
		return colorSpan.getColorSpan();
		
	}
	
	private ColorSpan getNewColorSpan(int color)
	{
		ColorSpan colorSpan = new ColorSpan(color);
		return colorSpan;
	}
	
	public void releaseResources()
	{
		highlightColorSpans = null;
		unHighlightColorSpans = null;
	}
}
