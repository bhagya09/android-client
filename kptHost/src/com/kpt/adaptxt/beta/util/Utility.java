package com.kpt.adaptxt.beta.util;

import java.util.Hashtable;

import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Utility {
	private Context mContext;
	private String TAG = "Utility";
	public static Hashtable<String, Typeface> sTypfaces = new Hashtable<String, Typeface>();
	
	public Utility(Context context){
		mContext = context;
	}

	public boolean checkNetworkConnection()
	{
	    boolean HaveConnectedWifi = false;
	    boolean HaveConnectedMobile = false;

	    ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo[] netInfo = cm.getAllNetworkInfo();
	    for (NetworkInfo ni : netInfo)
	    {
	        if (ni.getTypeName().equalsIgnoreCase("WIFI"))
	            if (ni.isConnected())
	                HaveConnectedWifi = true;
	        if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
	            if (ni.isConnected())
	                HaveConnectedMobile = true;
	    }
	    return HaveConnectedWifi || HaveConnectedMobile;
	}
	
	public static Typeface getTypeface(String type,Context context){
		synchronized (sTypfaces) {
			if (!sTypfaces.containsKey(type)) {
				try {
					Typeface t = Typeface.createFromAsset(context.getResources().getAssets(),KPTConstants.ATX_ASSETS_FOLDER+type);
					sTypfaces.put(type, t);
				} catch (Exception e) {
					//Log.e("KPT", "Could not get typeface '" + sPATH_FOR_ROBOTO_BOLD_TTF + "' because " + e.getMessage());
					return Typeface.DEFAULT;
				}
			}
			return sTypfaces.get(type);
		}

	}
}

