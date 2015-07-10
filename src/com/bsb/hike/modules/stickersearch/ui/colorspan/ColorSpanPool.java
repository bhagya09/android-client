package com.bsb.hike.modules.stickersearch.ui.colorspan;

import java.util.ArrayList;

import android.text.style.ForegroundColorSpan;

public class ColorSpanPool
{	
	private ArrayList<ColorSpan> highlightColorSpans;
	
	private ArrayList<ColorSpan> unHighlightColorSpans;
	
	private int highlightColor, unHighlightColor;
	
	private static int INITIAL_COLOR_SPAN_LIST_SIZE = 10;
	
	public ColorSpanPool(int highlightColor, int unHighlightColor)
	{
		highlightColorSpans = new ArrayList<>(INITIAL_COLOR_SPAN_LIST_SIZE);
		unHighlightColorSpans =  new ArrayList<>(INITIAL_COLOR_SPAN_LIST_SIZE);
		
		this.highlightColor = highlightColor;
		this.unHighlightColor = unHighlightColor;
		initialise();
	}
	
	private void initialise()
	{
		for (int i = 0; i < INITIAL_COLOR_SPAN_LIST_SIZE; i++)
		{
			highlightColorSpans.add(new ColorSpan(highlightColor));
			unHighlightColorSpans.add(new ColorSpan(unHighlightColor));
		}
	}
	
	public void markAll()
	{
		for(ColorSpan highlightColorSpan : highlightColorSpans)
		{
			highlightColorSpan.setMarked(true);
		}
		for(ColorSpan unHighlightColorSpan : unHighlightColorSpans)
		{
			unHighlightColorSpan.setMarked(true);
		}
	}
	
	public void unMarkAll()
	{
		for(ColorSpan highlightColorSpan : highlightColorSpans)
		{
			highlightColorSpan.setMarked(false);
		}
		for(ColorSpan unHighlightColorSpan : unHighlightColorSpans)
		{
			unHighlightColorSpan.setMarked(false);
		}
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
