package com.bsb.hike.media;

import java.util.List;

import com.bsb.hike.R;
import com.bsb.hike.view.TagEditText;
import com.bsb.hike.view.TagEditText.Tag;
import com.bsb.hike.view.TagEditText.TagEditorListener;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public class TagPicker implements TagEditorListener
{
	public static interface TagOnClickListener
	{
		public void onTagClicked(Tag tag);
	}
	
	private KeyboardPopupLayout popupLayout;

	private View viewToDisplay;

	private Context context;
	
	private TagOnClickListener listener;
	
	private TagEditText tagEditText;
	/**
	 * use this constructor when you want to show tagPicker above Soft Keyboard
	 * 
	 * @param mainView
	 * @param context
	 * @param listener
	 */
	public TagPicker(View mainView, Context context, PopupListener listener,TagOnClickListener tagClickListener)
	{
		popupLayout = new KeyboardPopupLayout(mainView, 200, context, listener);
		init(context,tagClickListener);
	}

	/**
	 * Use this constructor when you want to get View of TagPicker and want to show yourself basically you do not need to show it above Keyboard
	 * 
	 * @param context
	 */
	public TagPicker(Context context,TagOnClickListener listener)
	{
		init(context,listener);
	}
	
	
	private void init(Context context,TagOnClickListener listener)
	{
		this.context = context;
		this.listener = listener;
	}


	public void resetView(List<Tag> tags)
	{
		getView(tags);
	}
	
	public View getView(List<Tag> tags)
	{
		initView();
		fillTags(tags);
		return viewToDisplay;
	}

	private void initView()
	{
		if (viewToDisplay != null)
		{
			return;
		}
		viewToDisplay  = LayoutInflater.from(context).inflate(R.layout.tag_picker, null);
		tagEditText = (TagEditText) viewToDisplay.findViewById(R.id.tagEditText);
		tagEditText.setListener(this);
	}
	
	
	public void show(List<Tag> tags)
	{
		if(popupLayout == null)
		{
			throw new IllegalStateException("You did not call proper constructor before showing"); 
		}
		View view = getView(tags);
		popupLayout.showKeyboardPopup(viewToDisplay);
	}
	
	private void fillTags(List<Tag> tags)
	{
		tagEditText.resetTags(tags);
	}

	@Override
	public void tagRemoved(Tag tag)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tagAdded(Tag tag)
	{
	}

	@Override
	public void characterAddedAfterSeparator(String characters)
	{
	}

	@Override
	public void charResetAfterSeperator()
	{
	}

	@Override
	public void tagClicked(Tag tag)
	{
		this.listener.onTagClicked(tag);
	}
}
