package com.bsb.hike.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;


/**
 * 
 * @author himanshu
 * 
 * This class contains all information regarding the footer shown in conversation list in case of NUX is there.
 *
 */
public class NUXChatReward
{

	private String rewardCardText;

	private String rewardCardSuccessText;

	private String statusText;

	private String chatWaitingText;

	private String detailsText;

	private String detailsLink;

	private String inviteMoreText;

	private String remindText;

	private String tapToClaimLink;

	private String tapToClaimText;

	private String selectFriends;

	private String pendingChatIcon;

	private Bitmap pendingChatIconBitmap;

	/**
	 * @return the rewardCardText
	 */
	public String getRewardCardText()
	{
		return rewardCardText;
	}

	/**
	 * @return the rewardCardSuccessText
	 */
	public String getRewardCardSuccessText()
	{
		return rewardCardSuccessText;
	}

	/**
	 * @return the statusText
	 */
	public String getStatusText()
	{
		return statusText;
	}

	/**
	 * @return the chatWaitingText
	 */
	public String getChatWaitingText()
	{
		return chatWaitingText;
	}

	/**
	 * @return the pendingChatIcon
	 */
	public Bitmap getPendingChatIcon()
	{

		return pendingChatIconBitmap;
	}

	/**
	 * @return the detailsText
	 */
	public String getDetailsText()
	{
		return detailsText;
	}

	/**
	 * @return the detailsLink
	 */
	public String getDetailsLink()
	{
		return detailsLink;
	}

	/**
	 * @return the button1Text
	 */
	public String getInviteMoreButtonText()
	{
		return inviteMoreText;
	}

	/**
	 * @return the button2Text
	 */
	public String getRemindButtonText()
	{
		return remindText;
	}

	/**
	 * @return the tapToClaimLink
	 */
	public String getTapToClaimLink()
	{
		return tapToClaimLink;
	}

	/**
	 * @param toggleModule
	 * @param rewardCardText
	 * @param rewardCardSuccessText
	 * @param statusText
	 * @param chatWaitingText
	 * @param pendingChatIcon
	 * @param detailsText
	 * @param detailsLink
	 * @param button1Text
	 * @param button2Text
	 * @param tapToClaimLink
	 */
	public NUXChatReward(String rewardCardText, String rewardCardSuccessText, String statusText, String chatWaitingText, String pendingChatIcon,
			String detailsText, String detailsLink, String button1Text, String button2Text, String tapToClaimLink, String tapToClaimText, String selectFriends)
	{
		this.rewardCardText = rewardCardText;
		this.rewardCardSuccessText = rewardCardSuccessText;
		this.statusText = statusText;
		this.chatWaitingText = chatWaitingText;
		this.pendingChatIcon = pendingChatIcon;
		this.detailsText = detailsText;
		this.detailsLink = detailsLink;
		this.inviteMoreText = button1Text;
		this.remindText = button2Text;
		this.tapToClaimLink = tapToClaimLink;
		this.tapToClaimText = tapToClaimText;
		this.selectFriends = selectFriends;

		if (!TextUtils.isEmpty(pendingChatIcon))
		{
			//StringToBitmap mmBitmap = new StringToBitmap(pendingChatIcon);
			//HikeHandlerUtil.getInstance().postRunnableWithDelay(mmBitmap, 0);
			pendingChatIconBitmap = HikeBitmapFactory.stringToBitmap(pendingChatIcon);
		}
		else
		{
			pendingChatIconBitmap = BitmapFactory.decodeResource(HikeMessengerApp.getInstance().getResources(), R.drawable.ic_pending_icon);
		}
	}

	public String getTapToClaimText()
	{
		return tapToClaimText;
	}

	public String getSelectFriendsText()
	{
		return selectFriends;
	}

	class StringToBitmap implements Runnable
	{
		private String base64image = null;

		public StringToBitmap(String base64)
		{
			base64image = base64;
		}

		@Override
		public void run()
		{
			pendingChatIconBitmap = HikeBitmapFactory.stringToBitmap(base64image);
		}

	}

}
