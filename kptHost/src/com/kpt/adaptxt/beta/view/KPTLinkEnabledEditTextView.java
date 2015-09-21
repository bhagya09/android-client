package com.kpt.adaptxt.beta.view;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.kpt.adaptxt.beta.MainKeyboardView;

public class KPTLinkEnabledEditTextView  extends TextView
{
	// The String Containing the Text that we have to gather links from private SpannableString linkableText;
	// Populating and gathering all the links that are present in the Text
	private ArrayList<Hyperlink> listOfLinks; 

	// A Listener Class for generally sending the Clicks to the one which requires it
	TextLinkClickListener mListener;

	// Pattern for gathering @usernames from the Text
	Pattern screenNamePattern = Pattern.compile("([a-zA-Z])");

	// Pattern for gathering #hasttags from the Text
	Pattern hashTagsPattern = Pattern.compile("([a-zA-Z0-9_-])");

	// Pattern for gathering http:// links from the Text
	Pattern hyperLinksPattern = Pattern.compile("([Hh][tT][tT][pP][sS]?:\\/\\/[^ ,'\">\\]\\)]*[^\\. ,'\">\\]\\)])");

	private SpannableString linkableText;

	private StringBuffer offset;

	public KPTLinkEnabledEditTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		listOfLinks = new ArrayList<Hyperlink>();

	}

	public void gatherLinksForText(String text,MainKeyboardView view,String[] lastToken)
	{  
		
		String underlineString = "" ;

		if(lastToken.length > 0){
			underlineString = lastToken[lastToken.length-1];

		}
		linkableText = new SpannableString(text);
		/*
		 *  gatherLinks basically collects the Links depending upon the Pattern that we supply
		 *  and add the links to the ArrayList of the links
		 */
		gatherLinks(listOfLinks, linkableText, screenNamePattern);

		for(int i = 0; i< listOfLinks.size(); i++)
		{
			Hyperlink linkSpec = listOfLinks.get(i);
			
			
			/*
			 * this process here makes the Clickable Links from the text
			 */
			if(i>=text.length()-underlineString.length()-(lastToken.length-1)){
				
				linkableText.setSpan(linkSpec.span, linkSpec.start, linkSpec.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}

		/*
		 * sets the text for the TextView with enabled links
		 */
		
		view.getFocusedEditText().setText(linkableText);
	}

	/*
	 * sets the Listener for later click propagation purpose
	 */
	public void setOnTextLinkClickListener(TextLinkClickListener newListener)
	{
		mListener = newListener;
	}

	/*
	 * The Method mainly performs the Regex Comparison for the Pattern and adds them to
	 * listOfLinks array list
	 */

	private final void gatherLinks(ArrayList<Hyperlink> links,
			Spannable s, Pattern pattern)
	{
		// Matcher matching the pattern
		Matcher m = pattern.matcher(s);

		while (m.find())
		{
			int start = m.start();
			int end = m.end();

			/*
			 *  Hyperlink is basically used like a structure for storing the information about
			 *  where the link was found.
			 */
			Hyperlink spec = new Hyperlink();

			spec.textSpan = s.subSequence(start, end);
			spec.span = new InternalURLSpan(spec.textSpan.toString());
			spec.start = start;
			spec.end = end;

			links.add(spec);
		}
	}

	/*
	 * This is class which gives us the clicks on the links which we then can use.
	 */
	public class InternalURLSpan extends UnderlineSpan
	{
		private String clickedSpan;

		public InternalURLSpan (String clickedString)
		{
			clickedSpan = clickedString;
		}

		/*@Override
		public void onClick(View textView)
		{
			mListener.onTextLinkClick(textView, clickedSpan);
			
		}*/
	}

	/*
	 * Class for storing the information about the Link Location
	 */

	class Hyperlink
	{
		CharSequence textSpan;
		InternalURLSpan span;
		int start;
		int end;
	}
	public interface TextLinkClickListener
	{
		/*
		 *  This method is called when the TextLink is clicked from LinkEnabledTextView
		 */
		public void onTextLinkClick(View textView, String clickedString);
	}
}
