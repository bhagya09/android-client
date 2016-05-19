package com.bsb.hike.platform;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.filetransfer.FTUtils;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.platform.nativecards.NativeCardManager;
import com.bsb.hike.smartImageLoader.HighQualityThumbLoader;
import com.bsb.hike.smartImageLoader.ImageWorker;
import com.bsb.hike.smartImageLoader.NativeCardImageLoader;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontTextView;
import com.bsb.hike.view.HoloCircularProgress;

/**
 * Created by pushkargupta on 13/04/16. This class is a delegation class which performs the reponsibility of rendering of native cards.
 */
public class NativeCardRenderer implements View.OnLongClickListener, View.OnClickListener
{

	@Override
	public boolean onLongClick(View v)
	{
		return false;
	}

	private BaseAdapter mBaseAdapter;

	private Context context;

	private Conversation conversation;

	private ViewHolderFactory viewHolderFactory;

	private HighQualityThumbLoader hqThumbLoader;

	private boolean isListFlinging;

	private NativeCardImageLoader nativeCardImageLoader;

	public NativeCardRenderer(Context context, Conversation conversation, BaseAdapter baseAdapter, HighQualityThumbLoader hqThumbLoader, boolean isListFlinging)
	{
		this.context = context;
		this.conversation = conversation;
		viewHolderFactory = new ViewHolderFactory(context);
		this.mBaseAdapter = baseAdapter;
		this.hqThumbLoader = hqThumbLoader;
		this.isListFlinging = isListFlinging;

	}

	public View getView(View view, final ConvMessage convMessage, ViewGroup parent)
	{
		int cardType = convMessage.platformMessageMetadata.layoutId;
		ViewHolderFactory.ViewHolder viewHolder;
		if (view == null)
		{
			view = NativeCardManager.getInflatedViewAsPerType(context, cardType, parent, convMessage.isSent());
			viewHolder = viewHolderFactory.getViewHolder(cardType);
			view.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolderFactory.ViewHolder) view.getTag();
		}
		viewHolder.initializeHolder(view, convMessage);
		viewHolder.clearViewHolder(view);
		cardDataFiller(convMessage, viewHolder);
		viewHolder.processViewHolder(view);
		return view;
	}

	public int getCardCount()
	{
		// Multiplying by 2 so as to consider both the sent and received types
		return NativeCardManager.NativeCardType.values().length * 2;
	}

	public int getItemViewType(ConvMessage convMessage)
	{
		// Add the length of NativeCardType enum for received card type.
		return convMessage.isSent() ? convMessage.platformMessageMetadata.layoutId : convMessage.platformMessageMetadata.layoutId
				+ NativeCardManager.NativeCardType.values().length;
	}

	private void cardDataFiller(final ConvMessage convMessage, final ViewHolderFactory.ViewHolder viewHolder)
	{
		for (CardComponent.TextComponent textComponent : convMessage.platformMessageMetadata.textComponents)
		{
			String tag = textComponent.getTag();
			if (!TextUtils.isEmpty(tag))
			{

				CustomFontTextView tv = (CustomFontTextView) viewHolder.viewHashMap.get(tag);
				if (null != tv)
				{
					tv.setVisibility(View.VISIBLE);
					tv.setText(textComponent.getText());
					try
					{
						if (!TextUtils.isEmpty(textComponent.color))
						{
							tv.setTextColor(Color.parseColor(textComponent.color));
						}
					}
					catch (IllegalArgumentException ex)
					{
						// Arises in case of color is niether of format #AARRGGBB nor #RRGGBB
					}

					if (textComponent.size > 0)
					{
						tv.setTextSize(textComponent.size);
					}
				}
			}

		}

		for (CardComponent.MediaComponent mediaComponent : convMessage.platformMessageMetadata.mediaComponents)
		{
			String tag = mediaComponent.getTag();

			if (!TextUtils.isEmpty(tag))
			{
				View mediaView = viewHolder.viewHashMap.get(tag);

				HikeFile hikeFile = mediaComponent.getHikeFile();
				if (hikeFile != null)
				{
					mediaView.setVisibility(View.VISIBLE);
					populateMediaComponent(mediaView, convMessage, hikeFile);
				}
				else
				{
					mediaView.setVisibility(View.GONE);
				}
			}
		}
		for (CardComponent.ImageComponent imageComponent : convMessage.platformMessageMetadata.imageComponents)
		{
			String tag = imageComponent.getTag();

			if (!TextUtils.isEmpty(tag))
			{
				ImageView imageView = (ImageView) viewHolder.viewHashMap.get(tag);
				imageView.setVisibility(View.VISIBLE);
				if (imageComponent.getUrl() != null)
				{
					populateImageWithUrl(imageView, imageComponent);
				}
			}
		}

	}

	private void populateImageWithUrl(ImageView imageView, CardComponent.ImageComponent imageComponent)
	{

		NativeCardImageLoader nativeCardImageLoader = new NativeCardImageLoader((int) context.getResources().getDimension(R.dimen.native_card_message_container_wide_width),
				(int) context.getResources().getDimension(R.dimen.native_card_image_height));
//		nativeCardImageLoader.setResource(context);
		nativeCardImageLoader.setImageFadeIn(false);
		nativeCardImageLoader.setDefaultDrawableNull(true);
		nativeCardImageLoader.setImageLoaderListener(new ImageWorker.ImageLoaderListener()
		{
			@Override
			public void onImageWorkSuccess(ImageView imageView)
			{
				imageView.setVisibility(View.VISIBLE);
			}

			@Override
			public void onImageWorkFailed(ImageView imageView)
			{
				// do nothing
			}
		});
		nativeCardImageLoader.setDontSetBackground(true);
		nativeCardImageLoader.loadImage(imageComponent.getUrl(), imageView, isListFlinging, false);
	}

	private void populateMediaComponent(View v, ConvMessage convMessage, HikeFile hikeFile)
	{
		FileSavedState fss = null;
		MessagesAdapter.FTViewHolder ftViewHolder = new MessagesAdapter.FTViewHolder();
		if (convMessage.isSent())
		{
			fss = FileTransferManager.getInstance(context).getUploadFileState(convMessage.getMsgID(), hikeFile.getFile());
		}
		else
		{
			fss = FileTransferManager.getInstance(context).getDownloadFileState(convMessage.getMsgID(), hikeFile);
		}
		ftViewHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
		ftViewHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
		ftViewHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
		ftViewHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
		ftViewHolder.circularProgress.setRelatedMsgId(convMessage.getMsgID());
		ftViewHolder.ftAction = (ImageView) v.findViewById(R.id.action);
		ftViewHolder.fileDetails = v.findViewById(R.id.file_details);
		ftViewHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
		ftViewHolder.fileName = (TextView) v.findViewById(R.id.file_name);

		ftViewHolder.fileThumb.setBackgroundResource(0);
		ftViewHolder.fileThumb.setImageResource(0);

		boolean isUnknown = ContactManager.getInstance().isUnknownContact(conversation.getMsisdn());
		boolean isBot = BotUtils.isBot(conversation.getMsisdn());
		boolean showThumbnail = ((convMessage.isSent()) || (conversation instanceof OneToNConversation) || !isUnknown || (hikeFile.wasFileDownloaded())
				|| convMessage.isOfflineMessage() || isBot);
		Drawable thumbnail = null;
		if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
		{
			thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
		}
		else
		{
			thumbnail = hikeFile.getThumbnail();
		}

		// Logger.d("OfflineThreadManager","Actual Thumbanil is  "+thumbnail.toString());
		if (showThumbnail)
		{
			ftViewHolder.fileThumb.setImageDrawable(thumbnail);
			hqThumbLoader.setLoadingImage(thumbnail);
			hqThumbLoader.loadImage(hikeFile.getFilePath(), ftViewHolder.fileThumb, isListFlinging);
		}
		else
		{
			createMediaThumb(ftViewHolder.fileThumb, convMessage.platformMessageMetadata.isWideCard());
		}

		RelativeLayout.LayoutParams fileThumbParams = (RelativeLayout.LayoutParams) ftViewHolder.fileThumb.getLayoutParams();

		if (showThumbnail && thumbnail != null)
		{
			ftViewHolder.fileThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
			if (convMessage.platformMessageMetadata.isWideCard())
			{
				fileThumbParams.width = (int) context.getResources().getDimension(R.dimen.native_card_message_container_wide_width);
			}
			else
			{
				fileThumbParams.width = (int) context.getResources().getDimension(R.dimen.native_card_message_container_narror_width);
			}

			fileThumbParams.height = (int) (thumbnail.getIntrinsicHeight() * fileThumbParams.width) / thumbnail.getIntrinsicWidth();

		}
		ftViewHolder.fileThumb.setLayoutParams(fileThumbParams);

		ftViewHolder.fileThumb.setVisibility(View.VISIBLE);

		FTUtils.setupFileState(context, ftViewHolder, fss, convMessage.getMsgID(), hikeFile, convMessage.isSent(), false);

		ftViewHolder.fileThumb.setTag(convMessage);
		ftViewHolder.fileThumb.setOnClickListener(this);
		ftViewHolder.fileThumb.setOnLongClickListener(this);

	}

	private void createMediaThumb(ImageView fileThumb, boolean isWide)
	{
		// TODO Auto-generated method stub
		Logger.d(getClass().getSimpleName(), "creating default thumb. . . ");
		int pixels;
		if (isWide)
		{
			pixels = context.getResources().getDimensionPixelSize(R.dimen.native_card_message_container_wide_width);
		}
		else
		{
			pixels = context.getResources().getDimensionPixelSize(R.dimen.native_card_message_container_wide_width);
		}

		// int pixels = (int) (250 * Utils.densityMultiplier);
		Logger.d(getClass().getSimpleName(), "density: " + Utils.scaledDensityMultiplier);
		fileThumb.getLayoutParams().height = pixels;
		fileThumb.getLayoutParams().width = pixels;
		// fileThumb.setBackgroundColor(context.getResources().getColor(R.color.file_message_item_bg))
		fileThumb.setBackgroundResource(R.drawable.bg_file_thumb);
		/*
		 * When setting default media thumb to image view, need to remove the previous drawable of that view in case of view is re-used by adapter. Fogbugz Id : 37212
		 */
		fileThumb.setImageDrawable(null);
	}

	@Override
	public void onClick(View v)
	{
		ConvMessage convMessage = (ConvMessage) v.getTag();
		if (convMessage == null)
		{
			return;
		}
		Logger.d(getClass().getSimpleName(), "OnCLICK" + convMessage.getMsgID());
		// if (convMessage.isFileTransferMessage())
		// {

		// @GM

		HikeFile hikeFile = convMessage.platformMessageMetadata.getHikeFiles().get(0);
		// HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		if (Utils.getExternalStorageState() == Utils.ExternalStorageState.NONE && hikeFile.getHikeFileType() != HikeFile.HikeFileType.CONTACT
				&& hikeFile.getHikeFileType() != HikeFile.HikeFileType.LOCATION)
		{
			Toast.makeText(context, R.string.no_external_storage, Toast.LENGTH_SHORT).show();
			return;
		}
		File receivedFile = hikeFile.getFile();

		FileSavedState fss = FileTransferManager.getInstance(context).getDownloadFileState(convMessage.getMsgID(), hikeFile);

		Logger.d(getClass().getSimpleName(), fss.getFTState().toString());

		if (fss.getFTState() == FileTransferBase.FTState.COMPLETED)
		{
			openFile(hikeFile, convMessage, v);
		}
		else if (fss.getFTState() == FileTransferBase.FTState.IN_PROGRESS)
		{
			FileTransferManager.getInstance(context).pauseTask(convMessage.getMsgID());
		}
		else if (fss.getFTState() != FileTransferBase.FTState.INITIALIZED)
		{
			if (hikeFile.getHikeFileType() == HikeFile.HikeFileType.VIDEO)
			{
				if (fss.getFTState() == FileTransferBase.FTState.NOT_STARTED)
				{
					sendImageVideoRelatedAnalytic(ChatAnalyticConstants.VIDEO_RECEIVER_DOWNLOAD_MANUALLY);
				}
				else if (fss.getFTState() == FileTransferBase.FTState.ERROR)
				{
					sendImageVideoRelatedAnalytic(ChatAnalyticConstants.MEDIA_UPLOAD_DOWNLOAD_RETRY, AnalyticsConstants.MessageType.VEDIO, ChatAnalyticConstants.DOWNLOAD_MEDIA);
				}
			}
			else if (hikeFile.getHikeFileType() == HikeFile.HikeFileType.IMAGE)
			{
				if (fss.getFTState() == FileTransferBase.FTState.ERROR)
				{
					sendImageVideoRelatedAnalytic(ChatAnalyticConstants.MEDIA_UPLOAD_DOWNLOAD_RETRY, AnalyticsConstants.MessageType.IMAGE, ChatAnalyticConstants.DOWNLOAD_MEDIA);
				}
			}
			FileTransferManager.getInstance(context).downloadFile(receivedFile, hikeFile.getFileKey(), convMessage.getMsgID(), hikeFile.getHikeFileType(), convMessage, true,
					hikeFile);
		}
		mBaseAdapter.notifyDataSetChanged();

	}

	private void openFile(HikeFile hikeFile, ConvMessage convMessage, View parent)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_OPENED, hikeFile.getHikeFileType());

		Logger.d(getClass().getSimpleName(), "Opening file");
		switch (hikeFile.getHikeFileType())
		{

		case IMAGE:
		case VIDEO:
			if (hikeFile.exactFilePathFileExists())
			{
				ArrayList<HikeSharedFile> hsf = new ArrayList<>();
				HikeSharedFile sharedFile = new HikeSharedFile(hikeFile.serialize(), hikeFile.isSent(), convMessage.getMsgID(), convMessage.getMsisdn(),
						convMessage.getTimestamp(), convMessage.getGroupParticipantMsisdn());
				if (!TextUtils.isEmpty(hikeFile.getCaption()))
				{
					sharedFile.setCaption(hikeFile.getCaption());
				}
				hsf.add(sharedFile);
				PhotoViewerFragment.openPhoto(R.id.ct_parent_rl, context, hsf, true, conversation);
			}
			else
			{
				Toast.makeText(context, R.string.unable_to_open, Toast.LENGTH_SHORT).show();
			}
			return;

		default:
			HikeFile.openFile(hikeFile, context);
			return;
		}

	}

	private void sendImageVideoRelatedAnalytic(String uniqueKey_Order)
	{
		sendImageVideoRelatedAnalytic(uniqueKey_Order, null, null);
	}

	private void sendImageVideoRelatedAnalytic(String uniqueKey_Order, String genus, String family)
	{
		if (context != null && context instanceof ChatThreadActivity)
		{
			((ChatThreadActivity) context).recordMediaShareEvent(uniqueKey_Order, genus, family);
		}
	}
}
