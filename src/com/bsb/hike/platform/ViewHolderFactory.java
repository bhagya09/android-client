package com.bsb.hike.platform;

import java.util.HashMap;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.nativecards.NativeCardManager;
import com.bsb.hike.platform.nativecards.NativeCardUtils;
import com.bsb.hike.smartImageLoader.HikeDailyCardImageLoader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

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
			clearShareViewHolder(view);
			final int noOfAction = actionComponents.size();
			View cta1 = null;
			View cta2 = null;

			if (noOfAction >= 2)
			{
				showActionContainer(view);
				cta1 = actionContainer.findViewById(R.id.cta1);
				cta2 = actionContainer.findViewById(R.id.cta2);

			}
			else if (noOfAction == 1)
			{
				showActionContainer(view);
				cta1 = actionContainer.findViewById(R.id.cta1);
			}
			if (cta1 != null)
			{
				actionContainer.findViewById(R.id.divider).setVisibility(View.GONE);
				cta1.setVisibility(View.VISIBLE);
				cta1.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						try {
							NativeCardUtils.performAction(mContext, containerView, actionComponents.get(0), convMessage);
						} catch (JSONException ex) {
							Logger.e(TAG, ex.getMessage());
						}
					}
				});
				TextView cta1Text = (TextView) view.findViewById(R.id.cta1Text);
				cta1Text.setText(actionComponents.get(0).getActionText());
				cta1Text.setCompoundDrawablePadding((int)(6 * Utils.densityMultiplier));
				cta1Text.setCompoundDrawablesWithIntrinsicBounds(NativeCardUtils.getDrawable(actionComponents.get(0)), 0, 0, 0);

			}
			if (cta2 != null)
			{
				actionContainer.findViewById(R.id.divider).setVisibility(View.VISIBLE);
				cta2.setVisibility(View.VISIBLE);
				cta2.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						try
						{
							NativeCardUtils.performAction(mContext, containerView, actionComponents.get(1), convMessage);
						}
						catch (JSONException ex)
						{
							Logger.e(TAG, ex.getMessage());
						}
					}
				});
				TextView cta2Text = (TextView) view.findViewById(R.id.cta2Text);
				cta2Text.setText(actionComponents.get(1).getActionText());
				cta2Text.setCompoundDrawablePadding((int) (6 * Utils.densityMultiplier));
				cta2Text.setCompoundDrawablesWithIntrinsicBounds(NativeCardUtils.getDrawable(actionComponents.get(1)), 0, 0, 0);
			}
		}

		protected void showActionContainer(View view)
		{
			if (actionContainer == null)
			{
				ViewStub actionContainerStub = (ViewStub) view.findViewById(R.id.share_stub);
				actionContainer = actionContainerStub.inflate();
			}
			actionContainer.setVisibility(View.VISIBLE);
		}

		public void clearShareViewHolder(View view)
		{
			if (actionContainer != null)
			{
				actionContainer.findViewById(R.id.cta1).setVisibility(View.GONE);
				actionContainer.findViewById(R.id.cta2).setVisibility(View.GONE);
				actionContainer.setVisibility(View.GONE);
				actionContainer.findViewById(R.id.divider).setVisibility(View.GONE);
			}
		}
	}

	public abstract class ViewHolder extends ActionViewHolder
	{
		protected HashMap<String, View> viewHashMap;

		protected View cardContainer;

		public void initializeHolder(View view, final ConvMessage convMessage)
		{
			List<CardComponent.TextComponent> textComponents = convMessage.platformMessageMetadata.cards.get(0).textComponents;
			List<CardComponent.MediaComponent> mediaComponents = convMessage.platformMessageMetadata.cards.get(0).mediaComponents;
			List<CardComponent.ActionComponent> actionComponents = convMessage.platformMessageMetadata.cards.get(0).actionComponents;
			List<CardComponent.ImageComponent> imageComponents = convMessage.platformMessageMetadata.cards.get(0).imageComponents;

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
			if (convMessage.platformMessageMetadata.isWideCard())
			{
				messageContainer.getLayoutParams().width = (int) mContext.getResources().getDimension(R.dimen.native_card_message_container_wide_width);
			}
			else
			{
				messageContainer.getLayoutParams().width = (int) mContext.getResources().getDimension(R.dimen.native_card_message_container_narror_width);
			}

			for (CardComponent.TextComponent textComponent : textComponents)
			{
				String tag = textComponent.getTag();
				if (!TextUtils.isEmpty(tag))
					viewHashMap.put(tag, view.findViewWithTag(tag));
			}

			for (int i = 0; i < mediaComponents.size(); i++)
			{
				CardComponent.MediaComponent mediaComponent = mediaComponents.get(i);
				String tag = mediaComponent.getTag();
				if (!TextUtils.isEmpty(tag))
					viewHashMap.put(tag, view.findViewWithTag(tag));

			}
			for (int i = 0; i < imageComponents.size(); i++)
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
		public void clearViewHolder(View view) {}
	}

	public class HikeDailyViewHolder extends ViewHolder
	{
		private HikeDailyCardImageLoader hikeDailyCardImageLoader;

		public void initializeHolder(View view, ConvMessage convMessage)
		{
			super.initializeHolder(view, convMessage);

		}

		public void clearViewHolder(View view)
		{
			TextView t1Text = (TextView) view.findViewWithTag("T1");
			t1Text.setTextColor(ContextCompat.getColor(mContext, R.color.white));
			t1Text.setVisibility(View.GONE);
			t1Text.setTextSize(20);
			TextView t2Text = (TextView) view.findViewWithTag("T2");
			t2Text.setTextColor(ContextCompat.getColor(mContext, R.color.white));
			t2Text.setVisibility(View.GONE);
			t2Text.setTextSize(12);

		}

		@Override
		public void processViewHolder(final View view)
		{
			String foregraoundColor = convMessage.platformMessageMetadata.cards.get(0).backgroundColor != null ? convMessage.platformMessageMetadata.cards.get(0).backgroundColor
					: "#ffffffff";
			Drawable backgroundDrawable;
			if (!TextUtils.isEmpty(foregraoundColor))
			{
				ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor(foregraoundColor));
				backgroundDrawable = new LayerDrawable(new Drawable[] { colorDrawable });
			}
			else
			{
				backgroundDrawable = ContextCompat.getDrawable(mContext, R.drawable.hike_daily_bg);
			}
			ImageView background_image = (ImageView) view.findViewById(R.id.bg_image);
			background_image.setImageDrawable(backgroundDrawable);
			if (convMessage.platformMessageMetadata.cards.get(0).background != null)
			{
				hikeDailyCardImageLoader = new HikeDailyCardImageLoader();
				hikeDailyCardImageLoader.setImageFadeIn(false);
				hikeDailyCardImageLoader.setDontSetBackground(true);
				hikeDailyCardImageLoader.setDefaultDrawableNull(false);
				hikeDailyCardImageLoader.setForeGroundColor(convMessage.platformMessageMetadata.cards.get(0).backgroundColor);
				hikeDailyCardImageLoader.loadImage(convMessage.platformMessageMetadata.cards.get(0).background, background_image);
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
		}

		@Override
		public void processViewHolder(View view)
		{
			if (actionContainer != null)
			{
				ViewGroup.LayoutParams actionContainerParams = actionContainer.getLayoutParams();
				actionContainerParams.width = view.findViewById(R.id.file_thumb).getLayoutParams().width;
				actionContainer.setLayoutParams(actionContainerParams);
			}
		}
	}

	public class ImageCardHolder extends ViewHolder
	{

		public void initializeHolder(View view, ConvMessage convMessage)
		{
			super.initializeHolder(view, convMessage);
		}

		public void clearViewHolder(View view)
		{
			TextView t1Text = (TextView) view.findViewWithTag("T1");
			t1Text.setTextColor(ContextCompat.getColor(mContext, R.color.black));
			t1Text.setVisibility(View.GONE);
			t1Text.setTextSize(14);
			TextView t2Text = (TextView) view.findViewWithTag("T2");
			t2Text.setVisibility(View.GONE);
			t1Text.setTextColor(ContextCompat.getColor(mContext, R.color.black));
			t2Text.setTextSize(11);
			ImageView i1Image = (ImageView) view.findViewWithTag("i1");
			i1Image.setVisibility(View.GONE);
			ImageView placeholderImage = (ImageView) view.findViewById(R.id.placeholder);
			if (placeholderImage != null)
			{
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
