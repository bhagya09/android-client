/**
 * File   : StickerSearchUtility.java
 * Content: It is a utility class, especially designed for sticker search transitive operations.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;

import android.text.TextUtils;

import com.bsb.hike.utils.Logger;

public class StickerSearchUtility {

	private static final String TAG = StickerSearchUtility.class.getSimpleName();

	/* Determine if given character is special character */
	public static boolean isSpecialCharacter(char c) {

		return ((c < 'A') || (c > 'Z'));
	}

	/* Check if given word is of smiley's type */
	public static boolean isSmiley(String str) {

		boolean result = false;
		if (!TextUtils.isEmpty(str)) {
			result = !str.equals(formCorrectWord(null, str));
		}

		return result;
	}

	/* Eliminate special characters from the given word and form a new word without them */
	public static String formCorrectWord(StringBuilder sb, String str) {

		if (sb == null) {
			sb = new StringBuilder();
		}
		sb.setLength(0);
		char [] letters = str.toCharArray();
		// First, check if word is starting from special character
		if ((letters.length > 0) && !(StickerSearchUtility.isSpecialCharacter(letters [0]))) {
			for (char c : letters) {
				if (!StickerSearchUtility.isSpecialCharacter(c)) {
					sb.append(c);
				}
			}
		}

		return sb.toString();
	}

	/* Find maximum number from given list of numbers */
	public static int getMax(ArrayList<Integer> arrayList) {
		Logger.v(TAG, "getMax()");

		int max = Integer.MIN_VALUE;
		if (arrayList != null) {
			for (int i : arrayList) {
				if (i > max) {
					max = i;
				}
			}
		}

		return max;
	}

	/* Split charSequence in regular manner with indexing */
	public static Object [] [] splitAndDoIndexing(String parent, String regExpression) {
		
		return splitAndDoIndexing(parent, regExpression, 0);
	}

	/* Split charSequence in regular manner with indexing along with limit on splitting */
	public static Object [] [] splitAndDoIndexing(String parent, String regExpression, int limit) {
		
		Object [] [] result = null;
		if ((parent != null) && (regExpression != null)) {
			String [] words = parent.split(regExpression, limit);
			result = new Object [words.length] [3]; // value, start, length
			int start = 0;
			int len;
			int i = 0;

			for (String word : words) {
				result [i] [0] = word;
				result [i] [1] = start;
				len = word.length();
				result [i++] [2] = len;
				start += len + 1;
			}
		}

		return result;
	}
}