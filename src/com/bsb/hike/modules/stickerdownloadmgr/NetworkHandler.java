package com.bsb.hike.modules.stickerdownloadmgr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

class NetworkHandler extends BroadcastReceiver
{

	private Context context;

	private RequestQueue queue;

	public enum NetworkType
	{
		WIFI
		{
			@Override
			public int getStickerDownloadSize()
			{
				return 10;
			}
		},
		FOUR_G
		{
			@Override
			public int getStickerDownloadSize()
			{
				return 10;
			}
		},
		THREE_G
		{
			@Override
			public int getStickerDownloadSize()
			{
				return 10;
			}
		},
		TWO_G
		{
			@Override
			public int getStickerDownloadSize()
			{
				return 10;
			}
		},
		NO_NETWORK
		{
			@Override
			public int getStickerDownloadSize()
			{
				return 10;
			}
		};

		public abstract int getStickerDownloadSize();

	};

	NetworkHandler(Context context, RequestQueue queue)
	{
		// TODO Auto-generated constructor stub
		this.context = context;
		this.queue = queue;
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		context.registerReceiver(this, filter);
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
		{
			Logger.d(getClass().getSimpleName(), "Connectivity Change Occured");
			// if network available then proceed
			if (Utils.isUserOnline(context))
			{
				// TODO
			}
		}
	}

	public NetworkType getNetworkType() 
	{
		int networkType = Utils.getNetworkType(context);

		switch (networkType) {
		case -1:
			return NetworkType.NO_NETWORK;
		case 0:
			return NetworkType.TWO_G;
		case 1:
			return NetworkType.WIFI;
		case 2:
			return NetworkType.TWO_G;
		case 3:
			return NetworkType.THREE_G;
		case 4:
			return NetworkType.FOUR_G;
		default:
			return NetworkType.TWO_G;
		}
	}

}
