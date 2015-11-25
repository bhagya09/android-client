/**
 * 
 */
package com.bsb.hike;

import android.content.Context;

/**
 * @author anubansal
 *
 */
public class StringUtils {

	public static String getYouFormattedString(Context context, boolean isYou, int youStringID, int normalId, String name){
		if(isYou){
			return context.getString(youStringID);
		}else{
			return String.format(context.getString(normalId), name);
		}
	}
	
}
