package com.bsb.hike.filetransfer;

import com.bsb.hike.models.HikeHandlerUtil;

public class FTHandler extends HikeHandlerUtil
{
	/*
	 * Created separate handler thread for handling the FT message creation.
	 */
	public FTHandler() {
		super("FTHAndler");
	}
}
