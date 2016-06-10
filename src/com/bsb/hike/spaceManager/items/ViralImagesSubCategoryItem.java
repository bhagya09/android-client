package com.bsb.hike.spaceManager.items;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.spaceManager.models.SubCategoryItem;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.bsb.hike.spaceManager.SpaceManagerUtils.SUB_CATEGORY_TAG;

/**
 * @author paramshah
 */
public class ViralImagesSubCategoryItem extends SubCategoryItem
{
	private ArrayList<String> srcFileList;

	public ViralImagesSubCategoryItem(String header)
	{
		super(header);
		srcFileList = new ArrayList<>();
		setSize(computeSize());
	}

	@Override
	public long computeSize()
	{
		HikeFile.HikeFileType[] type = {HikeFile.HikeFileType.IMAGE};
		List<HikeSharedFile> list = HikeConversationsDatabase.getInstance().getSharedMedia(HikePlatformConstants.HIKE_VIRAL_MSISDN, type, 0);

		HashSet<String> set = new HashSet<String>();
		long size = 0;
		for (int i = 0; i < list.size(); i++)
		{
			if(set.add(list.get(i).getExactFilePath()))
			{
				size = size + list.get(i).getFileSize();
			}
		}
		srcFileList = new ArrayList<String>(set);
		return size;
	}

	@Override
	public void onDelete() {
		Logger.d(SUB_CATEGORY_TAG, "deleting viral humor images");
		Context context = HikeMessengerApp.getInstance().getApplicationContext();
		Utils.deleteFiles(context, srcFileList, HikeFile.HikeFileType.IMAGE, false);
		setSize(computeSize());
	}

	public ArrayList<String> getSrcFileList()
	{
		return srcFileList;
	}

	public void setSrcFileList(ArrayList<String> srcFileList)
	{
		this.srcFileList = srcFileList;
	}
}
