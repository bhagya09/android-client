package com.bsb.hike.spaceManager.items;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.spaceManager.models.SubCategoryItem;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.bsb.hike.spaceManager.SpaceManagerUtils.SUB_CATEGORY_TAG;

/**
 * @author paramshah
 */
public class ViralImagesSubCategoryItem extends SubCategoryItem
{
	private ArrayList<String> srcFileList;

	private static final String VIRAL_MSISDN = "+hikeviral+";

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
		List<HikeSharedFile> list = HikeConversationsDatabase.getInstance().getSharedMedia(VIRAL_MSISDN, type, 0);
		long size = 0;
		for (int i = 0; i < list.size(); i++)
		{
			size = size + list.get(i).getFileSize();
			srcFileList.add(list.get(i).getExactFilePath());
		}
		return size;
	}

	@Override
	public void onDelete()
	{
		Logger.d(SUB_CATEGORY_TAG, "deleting viral humor images");
		Context context = HikeMessengerApp.getInstance().getApplicationContext();
		Utils.deleteFiles(context, srcFileList, HikeFile.HikeFileType.IMAGE, false);
		setSize(0);
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
