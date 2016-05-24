package com.bsb.hike.utils;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.widget.EditText;

import com.bsb.hike.R;

/**
 * A class for annotating a CharSequence with spans to convert textual emoticons to graphical ones.
 */
public class SmileyParser
{
	private static int MAX_EMOTICONS = 100;

	public static final int[] EMOTICONS_SUBCATEGORIES = { 80, 30, 39, 25, 189, 116, 230, 102, 209 };

	public static final int[] HIKE_SUBCATEGORIES = { 80, 30, 39, 25 };

	public static final int[] EMOJI_SUBCATEGORIES = { 189, 116, 230, 102, 209 };

	public static final int MAX_EMOTICON_TEXT_LENGTH = 17;

	// Singleton stuff
	private static SmileyParser sInstance = null;

	private static final float SCALE_FACTOR = 0.85f;

	public static SmileyParser getInstance()
	{
		return sInstance;
	}

	public static void init(Context context)
	{
		// GH - added a null check so instances will get reused
		if (sInstance == null)
			sInstance = new SmileyParser(context);
	}

	private final Context mContext;

	private final String[] mSmileyTexts;

	private final Pattern mPattern;

	private final Pattern mEmojiPattern;

	private final HashMap<String, Integer> mSmileyToRes;

	private SmileyParser(Context context)
	{
		mContext = context;
		mSmileyTexts = mContext.getResources().getStringArray(DEFAULT_SMILEY_TEXTS);
		mSmileyToRes = buildSmileyToRes();
		mPattern = buildPattern();
		mEmojiPattern = buildEmojiPattern();
	}

	public static final int DEFAULT_SMILEY_TEXTS = R.array.default_smiley_texts;

	/**
	 * Builds the hashtable we use for mapping the string version of a smiley (e.g. ":-)") to a resource ID for the icon version.
	 */
	private HashMap<String, Integer> buildSmileyToRes()
	{
		if (EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length != (mSmileyTexts.length + EmoticonConstants.mEmojiUnicodes.length))
		{
			// Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
			// and failed to update arrays.xml
			throw new IllegalStateException("Smiley resource ID/text mismatch");
		}

		HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>();
		for (int i = 0; i < mSmileyTexts.length; i++)
		{
			smileyToRes.put(mSmileyTexts[i], EmoticonConstants.DEFAULT_SMILEY_RES_IDS[i]);
		}
		for (int i = mSmileyTexts.length; i < (mSmileyTexts.length + EmoticonConstants.mEmojiUnicodes.length); i++)
		{
			smileyToRes.put(EmoticonConstants.mEmojiUnicodes[i - mSmileyTexts.length], EmoticonConstants.DEFAULT_SMILEY_RES_IDS[i]);
		}

		return smileyToRes;
	}

	/**
	 * Builds the regular expression we use to find smileys in {@link #addSmileySpans}.
	 */
	private Pattern buildPattern()
	{
		StringBuilder patternString = new StringBuilder();

		// Build a regex that looks like (:-)|:-(|...), but escaping the smilies
		// properly so they will be interpreted literally by the regex matcher.
		patternString.append('(');
		for (String s : mSmileyTexts)
		{
			patternString.append(Pattern.quote(s));
			patternString.append('|');
		}
		for (String s : EmoticonConstants.mEmojiUnicodes)
		{
			patternString.append(s);
			patternString.append('|');
		}
		// Replace the extra '|' with a ')'
		patternString.replace(patternString.length() - 1, patternString.length(), ")");

		return Pattern.compile(patternString.toString(), Pattern.UNICODE_CASE);
	}

	private Pattern buildEmojiPattern()
	{
		StringBuilder patternString = new StringBuilder();

		patternString.append('(');
		for (String s : EmoticonConstants.mEmojiUnicodes)
		{
			patternString.append(s);
			patternString.append('|');
		}
		// Replace the extra '|' with a ')'
		patternString.replace(patternString.length() - 1, patternString.length(), ")");

		return Pattern.compile(patternString.toString(), Pattern.UNICODE_CASE);
	}

	/**
	 * Adds ImageSpans to a CharSequence that replace textual emoticons such as :-) with a graphical version.
	 * 
	 * @param text
	 *            A CharSequence possibly containing emoticons
	 * @param showSmallIcon
	 *            A boolean value which indicates whether to display the large or the small icon
	 * @return A CharSequence annotated with ImageSpans covering any recognized emoticons.
	 */
	public CharSequence addSmileySpans(CharSequence text, boolean showSmallIcon)
	{
		SpannableStringBuilder builder = new SpannableStringBuilder(text);
		addSmileyToEditable(builder, showSmallIcon);
		return builder;
	}

	/**
	 * Relaces all occurences of the emoji with a replacement string
	 * 
	 * @param text
	 * @param replacement
	 * @return
	 */
	public String replaceEmojiWithCharacter(CharSequence text, String replacement)
	{
		SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
		Matcher matcher = mEmojiPattern.matcher(spannableStringBuilder);

		return matcher.replaceAll(replacement);
	}

	/**
	 * Adds an emoticon image to the compose box
	 * 
	 * @param composeBox
	 *            : A reference to the text box in which the emoticon will be shown.
	 * @param whichEmoticon
	 *            : Integer value of the emoticon which is to be shown.
	 */
	public String addSmiley(EditText composeBox, int whichEmoticon)
	{
		int cursorStart = composeBox.getSelectionStart();
		Editable text = composeBox.getText();

        String emoji = null;

		if (whichEmoticon <= mSmileyTexts.length - 1)
		{
			emoji = mSmileyTexts[whichEmoticon];
		}
		else
		{
			whichEmoticon = whichEmoticon - mSmileyTexts.length;
			emoji = EmoticonConstants.mEmojiUnicodes[whichEmoticon];
		}

        text.insert(cursorStart,emoji);

        return emoji;
    }

	/**
	 * Used for adding smileys to the compose box while the user is typing.
	 * 
	 * @param editable
	 *            : this should be the same editable passed to us in the afterTextChanged method of the TextWatcher.
	 */
	public void addSmileyToEditable(Editable editable, boolean showSmallIcon)
	{
		addSmileyToEditable(editable, showSmallIcon, 0, editable.length());
	}

	public void addSmileyToEditable(Editable editable, boolean showSmallIcon, int startIndex, int length)
	{
		Matcher matcher = mPattern.matcher(editable);
		int count = 0;
		while (matcher.find() && (count < MAX_EMOTICONS))
		{
			if (matcher.start() < startIndex || matcher.end() > startIndex + length)
			{
				continue;
			}
			count++;
			int resId = mSmileyToRes.get(matcher.group());
			for (ImageSpan span : editable.getSpans(matcher.start(), matcher.end(), ImageSpan.class))
			{  //We have to remove previous span otherwise more smiley will be there for same simley code
				editable.removeSpan(span);
			}
			Drawable smiley = mContext.getResources().getDrawable(resId);
			smiley.setBounds(0, 0, showSmallIcon ? smiley.getIntrinsicWidth() / 2 : (int) (SCALE_FACTOR * smiley.getIntrinsicWidth()),
					showSmallIcon ? smiley.getIntrinsicHeight() / 2 : (int) (SCALE_FACTOR * smiley.getIntrinsicHeight()));
			editable.setSpan(new ImageSpan(smiley), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	public boolean containsEmoticon(String message)
	{
		Matcher matcher = mPattern.matcher(message);
		return matcher.find();
	}
}
