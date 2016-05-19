package com.bsb.hike.platform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.nativecards.NativeCardManager;
import com.bsb.hike.platform.nativecards.NativeCardUtils;
import com.bsb.hike.smartImageLoader.ImageWorker;
import com.bsb.hike.smartImageLoader.HikeDailyCardImageLoader;
import com.bsb.hike.smartImageLoader.NativeCardImageLoader;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pushkargupta on 15/04/16. This class gets the JsonObject and returns the view
 */
public class ViewHolderFactory
{
	private static final String TAG = ViewHolderFactory.class.getSimpleName();
	private Context mContext;
	public ViewHolderFactory(Context context)
	{
		mContext = context;

	}

	private NativeCardManager.NativeCardType[] cardTypes = NativeCardManager.NativeCardType.values();

	public abstract class ActionViewHolder extends MessagesAdapter.DetailViewHolder
	{
		public View actionContainer;
		protected ConvMessage convMessage;
		protected void showActionContainer(final View view, final View containerView, final List<CardComponent.ActionComponent> actionComponents)
		{
			clearViewHolder(view);
			final int noOfAction = actionComponents.size();
			View cta1 = null;
			View cta2 = null;
			switch (noOfAction){
				case 2:
					showActionContainer(view);
					cta1 = actionContainer.findViewById(R.id.cta1);
					cta2 = actionContainer.findViewById(R.id.cta2);
					break;
				case 1:
					showActionContainer(view);
					cta1 = actionContainer.findViewById(R.id.cta1);
					break;
			}
			if(cta1 != null){
				cta1.setVisibility(View.VISIBLE);
				cta1.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						try{
							NativeCardUtils.performAction(mContext, containerView, actionComponents.get(0), convMessage);
						}catch (JSONException ex){
							Logger.e(TAG,ex.getMessage());
						}
					}
				});
				TextView cta1Text = (TextView)view.findViewById(R.id.cta1Text);
				cta1Text.setText(actionComponents.get(0).getActionText());
				cta1Text.setCompoundDrawablesWithIntrinsicBounds(NativeCardUtils.getDrawable(actionComponents.get(0)), 0, 0, 0);
			}
            if(cta2 != null){
				cta2.setVisibility(View.VISIBLE);
				cta2.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						try{
							NativeCardUtils.performAction(mContext, containerView, actionComponents.get(1), convMessage);
						}catch (JSONException ex){
							Logger.e(TAG,ex.getMessage());
						}
					}
				});
				TextView cta2Text = (TextView)view.findViewById(R.id.cta2Text);
				cta2Text.setText(actionComponents.get(1).getActionText());
				cta2Text.setCompoundDrawablesWithIntrinsicBounds(NativeCardUtils.getDrawable(actionComponents.get(1)), 0, 0, 0);
			}
		}
		protected void showActionContainer(View view){
			if (actionContainer == null)
			{
				ViewStub actionContainerStub = (ViewStub) view.findViewById(R.id.share_stub);
				actionContainer = actionContainerStub.inflate();
			}
			actionContainer.setVisibility(View.VISIBLE);
		}

		public void clearViewHolder(View view){
			if(actionContainer != null){
				actionContainer.findViewById(R.id.cta1).setVisibility(View.GONE);
				actionContainer.findViewById(R.id.cta2).setVisibility(View.GONE);
				actionContainer.setVisibility(View.GONE);
			}
		}
	}

	public abstract class ViewHolder extends ActionViewHolder
	{
		protected HashMap<String, View> viewHashMap;
		protected View cardContainer;

		public void initializeHolder(View view, final ConvMessage convMessage)
		{
			List<CardComponent.TextComponent> textComponents = convMessage.platformMessageMetadata.textComponents;
			List<CardComponent.MediaComponent> mediaComponents = convMessage.platformMessageMetadata.mediaComponents;
			List<CardComponent.ActionComponent> actionComponents = convMessage.platformMessageMetadata.actionComponents;
			List<CardComponent.ImageComponent> imageComponents = convMessage.platformMessageMetadata.imageComponents;
			ViewHolderFactory.ViewHolder viewHolder;
			boolean showShare = convMessage.platformMessageMetadata.showShare;
			boolean isSent = convMessage.isSent();
			viewHashMap = new HashMap<String, View>();
			time = (TextView) view.findViewById(R.id.time);
			status = (ImageView) view.findViewById(R.id.status);
			timeStatus = (View) view.findViewById(R.id.time_status);
			selectedStateOverlay = view.findViewById(R.id.selected_state_overlay);
			messageContainer = (ViewGroup) view.findViewById(R.id.message_container);
			dayStub = (ViewStub) view.findViewById(R.id.day_stub);
			messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);
			this.convMessage = convMessage;
			if(convMessage.platformMessageMetadata.isWideCard()){
				messageContainer.getLayoutParams().width = (int)mContext.getResources().getDimension(R.dimen.native_card_message_container_wide_width);
			}else{
				messageContainer.getLayoutParams().width = (int)mContext.getResources().getDimension(R.dimen.native_card_message_container_narror_width);
			}

			for (CardComponent.TextComponent textComponent : textComponents)
			{
				String tag = textComponent.getTag();
				if (!TextUtils.isEmpty(tag))
					viewHashMap.put(tag, view.findViewWithTag(tag));
			}

			for (int i=0;i<mediaComponents.size();i++)
			{
				CardComponent.MediaComponent mediaComponent = mediaComponents.get(i);
				String tag = mediaComponent.getTag();
				if (!TextUtils.isEmpty(tag))
					viewHashMap.put(tag, view.findViewWithTag(tag));

			}
			for (int i=0;i<imageComponents.size();i++)
			{
				CardComponent.ImageComponent mediaComponent = imageComponents.get(i);
				String tag = mediaComponent.getTag();
				if (!TextUtils.isEmpty(tag))
					viewHashMap.put(tag, view.findViewWithTag(tag));
			}

			if (isSent)
			{
				initializeHolderForSender(view);
			}
			else
			{
				initializeHolderForReceiver(view);
			}
			cardContainer = view.findViewById(R.id.card_container);
			showActionContainer(view, cardContainer, actionComponents);
			final CardComponent.ActionComponent cardAction = convMessage.platformMessageMetadata.cardAction;
			if(cardAction != null){
                cardContainer.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						try {
							NativeCardUtils.performAction(mContext,cardContainer,cardAction,convMessage);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});
			}else {
				cardContainer.setOnClickListener(null);
			}
		}

		public void initializeHolderForSender(View view)
		{
			messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);
		}

		public void initializeHolderForReceiver(View view)
		{

			senderDetails = view.findViewById(R.id.sender_details);
			senderName = (TextView) view.findViewById(R.id.sender_name);
			senderNameUnsaved = (TextView) view.findViewById(R.id.sender_unsaved_name);
			avatarImage = (ImageView) view.findViewById(R.id.avatar);
			avatarContainer = (ViewGroup) view.findViewById(R.id.avatar_container);

		}


		public abstract void processViewHolder(View view);

	}

	public class HikeDailyViewHolder extends ViewHolder
	{
		private HikeDailyCardImageLoader hikeDailyCardImageLoader;
		public HikeDailyViewHolder(){

		}
		public void initializeHolder(View view, ConvMessage convMessage)
		{
			super.initializeHolder(view, convMessage);

		}

		public void clearViewHolder(View view)
		{
			super.clearViewHolder(view);
			TextView t1Text = (TextView) view.findViewWithTag("T1");
			t1Text.setVisibility(View.GONE);
			TextView t2Text = (TextView) view.findViewWithTag("T2");
			t2Text.setVisibility(View.GONE);

		}

		@Override
		public void processViewHolder(final View view)
		{
			String foregraoundColor = convMessage.platformMessageMetadata.backgroundColor != null?convMessage.platformMessageMetadata.backgroundColor:"#ffffffff";
			Drawable backgroundDrawable = null;
			final View containerView = view.findViewById(R.id.card_container);


			if (convMessage.platformMessageMetadata.background != null)
			{
				ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor(foregraoundColor));
				backgroundDrawable = new LayerDrawable(new Drawable[]{
						colorDrawable
				});
				containerView.setBackground(backgroundDrawable);
				hikeDailyCardImageLoader = new HikeDailyCardImageLoader();
				hikeDailyCardImageLoader.setResource(mContext);
				hikeDailyCardImageLoader.setImageFadeIn(false);
				hikeDailyCardImageLoader.setDefaultDrawableNull(false);
				hikeDailyCardImageLoader.setDefaultDrawable(backgroundDrawable);
				hikeDailyCardImageLoader.setDontSetBackground(true);
				hikeDailyCardImageLoader.setForeGroundColor(convMessage.platformMessageMetadata.backgroundColor != null ? convMessage.platformMessageMetadata.backgroundColor : null);
				hikeDailyCardImageLoader.loadImage(convMessage.platformMessageMetadata.background, containerView, false, false, null);
			}else
			{
				backgroundDrawable = new LayerDrawable(new Drawable[] { ContextCompat.getDrawable(mContext, R.drawable.hike_daily_bg),
						new ColorDrawable(Color.parseColor(foregraoundColor)) });
				containerView.setBackground(backgroundDrawable);
			}
		}

	}

	public class JFLViewHolder extends ViewHolder
	{
		public void initializeHolder(View view, ConvMessage convMessage)
		{
			super.initializeHolder(view, convMessage);
		}
		public void clearViewHolder(View view)
		{
			super.clearViewHolder(view);
			TextView t1Text = (TextView) view.findViewWithTag("T1");
			t1Text.setTextColor(Color.parseColor("#ffffff"));
			t1Text.setVisibility(View.GONE);
			t1Text.setTextSize(18);
			TextView t2Text = (TextView) view.findViewWithTag("T2");
			t2Text.setTextColor(Color.parseColor("#ffffff"));
			t2Text.setVisibility(View.GONE);
			t2Text.setTextSize(12);
		}

		@Override
		public void processViewHolder(View view)
		{
			if(actionContainer != null){
				ViewGroup.LayoutParams actionContainerParams = actionContainer.getLayoutParams();
				actionContainerParams.width = view.findViewById(R.id.file_thumb).getLayoutParams().width;
				actionContainer.setLayoutParams(actionContainerParams);
			}
		}
	}
	public class ImageCardHolder extends ViewHolder
	{
		private NativeCardImageLoader nativeCardImageLoader;

		public void initializeHolder(View view, ConvMessage convMessage)
		{
			super.initializeHolder(view, convMessage);
		}
		public void clearViewHolder(View view)
		{
			super.clearViewHolder(view);
			TextView t1Text = (TextView) view.findViewWithTag("T1");
			t1Text.setTextColor(Color.parseColor("#000000"));
			t1Text.setVisibility(View.GONE);
			t1Text.setTextSize(14);
			TextView t2Text = (TextView) view.findViewWithTag("T2");
			t2Text.setVisibility(View.GONE);
			t2Text.setTextColor(Color.parseColor("#000000"));
			t2Text.setTextSize(11);
			ImageView i1Image = (ImageView) view.findViewWithTag("i1");
			i1Image.setVisibility(View.GONE);
			ImageView placeholderImage = (ImageView)view.findViewById(R.id.placeholder);
			if(placeholderImage != null){
				placeholderImage.setVisibility(View.VISIBLE);
			}
		}

		@Override
		public void processViewHolder(final View view)
		{

		}
	}
	public ViewHolder getViewHolder(int type)
	{
		switch (cardTypes[type])
		{
		case HIKE_DAILY:
			return new HikeDailyViewHolder();
		case JFL:
			return new JFLViewHolder();
		case IMAGE_CARD:
			return new ImageCardHolder();
		default:
			return null;
		}
	}



}
