package com.bsb.hike.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.ExplandingCells.ExpandableListItem;
import com.bsb.hike.ExplandingCells.ExpandingLayout;
import com.bsb.hike.ExplandingCells.ExpandingListView;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.Conversation.OneToNConversationMetadata;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.ProfileItem.ProfileGroupItem;
import com.bsb.hike.models.ProfileItem.ProfileSharedContent;
import com.bsb.hike.models.ProfileItem.ProfileSharedMedia;
import com.bsb.hike.models.ProfileItem.ProfileStatusItem;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.ProfilePicImageLoader;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;
import com.bsb.hike.smartImageLoader.TimelineUpdatesImageLoader;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;

import java.util.List;

public class ProfileAdapter extends ArrayAdapter<ProfileItem> implements View.OnClickListener, CompoundButton.OnCheckedChangeListener
{		
	public static final String OPEN_GALLERY = "OpenGallery";
	
	public static final String IMAGE_TAG = "image";

	private static enum ViewType
	{
		HEADER, SHARED_MEDIA, SHARED_CONTENT, STATUS, PROFILE_PIC_UPDATE, GROUP_PARTICIPANT, EMPTY_STATUS, REQUEST, MEMBERS, ADD_MEMBERS, PHONE_NUMBER, GROUP_SETTINGS, GROUP_RIGHTS_INFO, IMAGE_POST, TEXT_IMAGE_POST, PRIVACY_SECTION
	}

	private Context context;

	private ProfileActivity profileActivity;

	private OneToNConversation groupConversation;

	private ContactInfo mContactInfo;

	private Bitmap profilePreview;

	private boolean groupProfile;

	private boolean myProfile;

	private boolean isContactBlocked;
	
	private IconLoader iconLoader;

	private TimelineUpdatesImageLoader bigPicImageLoader;

	private ProfilePicImageLoader profileImageLoader;
	
	private SharedFileImageLoader thumbnailLoader;

	private int mIconImageSize;
	
	private boolean hasCustomPhoto;
	
	private int sizeOfThumbnail;

	public ListView listView;

	public ProfileAdapter(ProfileActivity profileActivity, List<ProfileItem> itemList, OneToNConversation groupConversation, ContactInfo contactInfo, boolean myProfile)
	{
		this(profileActivity, itemList, groupConversation, contactInfo, myProfile, false);
	}
	
	public ProfileAdapter(ProfileActivity profileActivity, List<ProfileItem> itemList, OneToNConversation groupConversation, ContactInfo contactInfo, boolean myProfile, boolean isContactBlocked, int sizeOfThumbNail)
	{
		this(profileActivity, itemList, groupConversation, contactInfo, myProfile, false);
		this.sizeOfThumbnail = sizeOfThumbNail;
		thumbnailLoader = new SharedFileImageLoader(context, sizeOfThumbnail);
		thumbnailLoader.setDefaultDrawable(context.getResources().getDrawable(R.drawable.ic_file_thumbnail_missing));
		this.groupConversation = groupConversation;
	}
	
	public ProfileAdapter(ProfileActivity profileActivity, List<ProfileItem> itemList, OneToNConversation groupConversation, ContactInfo contactInfo, boolean myProfile,
			boolean isContactBlocked)
	{
		super(profileActivity, -1, itemList);
		this.context = profileActivity;
		this.profileActivity = profileActivity;
		this.groupProfile = groupConversation != null;
		this.mContactInfo = contactInfo;
		this.groupConversation = groupConversation;
		this.myProfile = myProfile;
		this.isContactBlocked = isContactBlocked;
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		int mBigImageSize = context.getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);
		this.bigPicImageLoader = new TimelineUpdatesImageLoader(context, mBigImageSize);
		this.profileImageLoader = new ProfilePicImageLoader(context, mBigImageSize);
		profileImageLoader.setDefaultAvatarIfNoCustomIcon(true);
		profileImageLoader.setHiResDefaultAvatar(true);
		this.iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		hasCustomPhoto = getHasCustomPhoto(); 
	}
	
	@Override
	public int getItemViewType(int position)
	{
		ViewType viewType;
		ProfileItem profileItem = getItem(position);
		int itemId = profileItem.getItemId();
		if (ProfileItem.HEADER_ID == itemId)
		{
			viewType = ViewType.HEADER;
		}
		else if (ProfileItem.SHARED_MEDIA == itemId)
		{
			viewType = ViewType.SHARED_MEDIA;
		}
		else if (ProfileItem.SHARED_CONTENT == itemId)
		{
			viewType = ViewType.SHARED_CONTENT;
		}
		else if (ProfileItem.MEMBERS == itemId)
		{
			viewType = ViewType.MEMBERS;
		}
		else if (ProfileItem.GROUP_SETTINGS == itemId)
		{
			viewType = ViewType.GROUP_SETTINGS;
		}
		else if (ProfileItem.GROUP_RIGHTS_INFO == itemId)
		{
			viewType = ViewType.GROUP_RIGHTS_INFO;
		}
		else if (ProfileItem.GROUP_MEMBER == itemId)
		{
			viewType = ViewType.GROUP_PARTICIPANT;
		}
		else if (ProfileItem.ADD_MEMBERS == itemId)
		{
			viewType = ViewType.ADD_MEMBERS;
		}
		else if (ProfileItem.EMPTY_ID == itemId)
		{
			viewType = ViewType.EMPTY_STATUS;
		}
		else if (ProfileItem.REQUEST_ID == itemId)
		{
			viewType = ViewType.REQUEST;
		}
		else if (ProfileItem.PHONE_NUMBER == itemId)
		{
			viewType = ViewType.PHONE_NUMBER;
		}

		else if (ProfileItem.PRIVACY_SECTION == itemId)
		{
			viewType = ViewType.PRIVACY_SECTION;
		}

		else
		{
			StatusMessage statusMessage = ((ProfileStatusItem) profileItem).getStatusMessage();
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
			{
				viewType = ViewType.PROFILE_PIC_UPDATE;
			}
			else if (statusMessage.getStatusMessageType() == StatusMessageType.IMAGE)
			{
				viewType = ViewType.IMAGE_POST;
			}
			else if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT_IMAGE)
			{
				viewType = ViewType.TEXT_IMAGE_POST;
			}
			else
			{
				viewType = ViewType.STATUS;
			}
		}
		return viewType.ordinal();
	}

	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	@Override
	public boolean isEnabled(int position)
	{
		ViewType viewType = ViewType.values()[getItemViewType(position)];
		if (viewType == ViewType.HEADER || viewType == ViewType.SHARED_MEDIA || viewType == ViewType.PHONE_NUMBER)
		{
			return false;
		}
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ViewType viewType = ViewType.values()[getItemViewType(position)];

		ProfileItem profileItem = getItem(position);

		View v = convertView;
		
		ViewHolder viewHolder = null;

		if (v == null)
		{
			viewHolder = new ViewHolder();

			switch (viewType)
			{
			case HEADER:
				v = inflater.inflate(R.layout.profile_header, null);
				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.info);
				viewHolder.image = (ImageView) v.findViewById(R.id.profile);
				viewHolder.icon = (ImageView) v.findViewById(R.id.change_profile);
				break;

			case SHARED_MEDIA:
				v = inflater.inflate(R.layout.shared_media, null);
				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.count);
				viewHolder.infoContainer = v.findViewById(R.id.shared_media_items);
				viewHolder.parent =  v.findViewById(R.id.sm_emptystate);
				viewHolder.sharedFiles = v.findViewById(R.id.shared_media);
				viewHolder.icon = (ImageView) v.findViewById(R.id.arrow_icon);
				viewHolder.mediaLayout = v.findViewById(R.id.media_layout);
				List<HikeSharedFile> sharedMedia = (List<HikeSharedFile>) ((ProfileSharedMedia) profileItem).getSharedFilesList();
				LinearLayout layout = (LinearLayout) viewHolder.infoContainer;
				layout.removeAllViews();
				int smSize = ((ProfileSharedMedia) profileItem).getSharedMediaCount();
				int maxMediaToShow = ((ProfileSharedMedia) profileItem).getMaxMediaToShow();
				viewHolder.subText.setText(Integer.toString(smSize));
				if(sharedMedia != null)
				{
					LinearLayout.LayoutParams lp;
						for(int i=0;i<Math.min(smSize, maxMediaToShow);i++)
						{
							
							View image_thumb = inflater.inflate(R.layout.thumbnail_layout, layout, false);
							lp = (LinearLayout.LayoutParams) image_thumb.getLayoutParams();
							lp.width = sizeOfThumbnail;
							lp.height = sizeOfThumbnail;
							lp.weight = 1.0f;
							image_thumb.setLayoutParams(lp);
							layout.addView(image_thumb);
						}
					
				}
				
				break;

			case SHARED_CONTENT:
				v = inflater.inflate(R.layout.shared_content, null);
				viewHolder.parent = v.findViewById(R.id.shared_content);
				viewHolder.sharedFiles = v.findViewById(R.id.shared_content_layout);
				viewHolder.text = (TextView) viewHolder.parent.findViewById(R.id.name);
				viewHolder.subText = (TextView) viewHolder.parent.findViewById(R.id.count);
				viewHolder.extraInfo = (TextView) v.findViewById(R.id.count_pin);
				viewHolder.sharedFilesText = (TextView) v.findViewById(R.id.shared_files);
				viewHolder.groupOrPins = (TextView) v.findViewById(R.id.shared_pins);
				viewHolder.sharedFilesCount = (TextView) v.findViewById(R.id.count_sf);
				viewHolder.icon = (ImageView) v.findViewById(R.id.shared_pin_icon);
				viewHolder.timeStamp = (TextView) v.findViewById(R.id.sm_emptystate);
				viewHolder.files = v.findViewById(R.id.shared_files_rl);
				viewHolder.pins = v.findViewById(R.id.shared_pins_rl);
				if(!groupProfile)
				{
					LayoutParams ll = (LayoutParams) viewHolder.timeStamp.getLayoutParams();
					ll.topMargin -= context.getResources().getDimensionPixelSize(R.dimen.top_margin_shared_content);
					viewHolder.timeStamp.setLayoutParams(ll); // Hack for top margin in case of one to one profile for empty state of content
				}
				break;

			case MEMBERS:
				v = inflater.inflate(R.layout.friends_group_view, null);
				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.count);
				break;
				
			case GROUP_SETTINGS:
				v = inflater.inflate(R.layout.group_settings_item, null);
				viewHolder.checkbox = (CheckBox) v.findViewById(R.id.checkBox);
			
				break;

			case GROUP_PARTICIPANT:
				v = new LinearLayout(context);
				viewHolder.parent = inflater.inflate(R.layout.group_profile_item, (LinearLayout) v, false);
				viewHolder.text = (TextView) viewHolder.parent.findViewById(R.id.name);
				viewHolder.icon  = (ImageView) viewHolder.parent.findViewById(R.id.avatar);
				viewHolder.iconFrame = (ImageView) viewHolder.parent.findViewById(R.id.avatar_frame);
				viewHolder.infoContainer = viewHolder.parent.findViewById(R.id.owner_indicator);
				viewHolder.phoneNumViewDivider = viewHolder.parent.findViewById(R.id.divider);
				viewHolder.extraInfo = (TextView) viewHolder.parent.findViewById(R.id.telephone);
				
				break;

			case ADD_MEMBERS:
				v = new LinearLayout(context);
				break;
			case GROUP_RIGHTS_INFO:
				v = new LinearLayout(context);
				break;
			case STATUS:
				v = inflater.inflate(R.layout.profile_timeline_item, null);

				viewHolder.icon = (ImageView) v.findViewById(R.id.avatar);
				viewHolder.iconFrame = (ImageView) v.findViewById(R.id.avatar_frame);

				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.main_info);
				viewHolder.timeStamp = (TextView) v.findViewById(R.id.timestamp);
				viewHolder.parent = v.findViewById(R.id.main_content);
				break;

			case IMAGE_POST:
			case PROFILE_PIC_UPDATE:
			case TEXT_IMAGE_POST:
				v = inflater.inflate(R.layout.contact_timeline_item, null);

				viewHolder.icon = (ImageView) v.findViewById(R.id.avatar);

				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.main_info);
				viewHolder.image = (ImageView) v.findViewById(R.id.profile_pic);
				viewHolder.timeStamp = (TextView) v.findViewById(R.id.timestamp);
				viewHolder.infoContainer = v.findViewById(R.id.info_container);
				viewHolder.parent = v.findViewById(R.id.main_content);
				break;

			case PHONE_NUMBER:
				v = inflater.inflate(R.layout.phone_num_layout, null);
				viewHolder.parent = v.findViewById(R.id.phone_numbers);
				viewHolder.text = (TextView) viewHolder.parent.findViewById(R.id.name);
				viewHolder.extraInfo = (TextView) v.findViewById(R.id.phone_number);
				viewHolder.subText = (TextView) v.findViewById(R.id.main_info);
				viewHolder.phoneIcon = (ImageView) v.findViewById(R.id.call);
				break;

				case PRIVACY_SECTION:
					v = inflater.inflate(R.layout.profile_privacy_section, null);
					viewHolder.parent = v.findViewById(R.id.privacy_parent_layout);
					viewHolder.expandingLayout = (ExpandingLayout) v.findViewById(R.id.expanding_layout);
					viewHolder.lastSeenSwitch = (SwitchCompat) viewHolder.expandingLayout.findViewById(R.id.last_seen_switch);
					viewHolder.statusUpdateSwitch = (SwitchCompat) viewHolder.expandingLayout.findViewById(R.id.status_update_switch);
					viewHolder.lastSeenSetting = viewHolder.expandingLayout.findViewById(R.id.last_seen_section);
					viewHolder.statusUpdateSetting = viewHolder.expandingLayout.findViewById(R.id.status_update_section);
					break;
			}

			v.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) v.getTag();
		}

		switch (viewType)
		{
		case HEADER:
			
			String contmsisdn = mContactInfo.getMsisdn();
			String contname = TextUtils.isEmpty(mContactInfo.getName()) ? mContactInfo.getMsisdn() : mContactInfo.getName();
			viewHolder.text.setText(contname);
			String mapedId = contmsisdn + ProfileActivity.PROFILE_PIC_SUFFIX;
			ImageViewerInfo imageViewerInf = new ImageViewerInfo(mapedId, null, false, !ContactManager.getInstance().hasIcon(contmsisdn));
			viewHolder.image.setTag(imageViewerInf);
			if (profilePreview == null)
			{
				if(hasCustomPhoto)
				{
					profileImageLoader.loadImage(mapedId, viewHolder.image, isListFlinging);
				}
				else
				{
					viewHolder.image.setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(mContactInfo.getMsisdn(), false));
					viewHolder.image.setImageResource(R.drawable.ic_default_avatar_hires);
				}
			}
			else
			{
				viewHolder.image.setImageBitmap(profilePreview);
			}
			viewHolder.icon.setVisibility(View.VISIBLE);
			if (myProfile)
			{
				viewHolder.icon.setImageResource(R.drawable.ic_change_profile_pic);
			}
			
			else
			{
				viewHolder.icon.setImageResource(R.drawable.ic_new_conversation);
			}

			if (mContactInfo != null)
			{
				if (mContactInfo.getMsisdn().equals(mContactInfo.getId()))
				{
					viewHolder.subText.setVisibility(View.VISIBLE);
					viewHolder.subText.setText(R.string.tap_to_save);
				}
				else
				{
					viewHolder.subText.setVisibility(View.VISIBLE);
					viewHolder.subText.setText(mContactInfo.getMsisdn());
					if (!TextUtils.isEmpty(mContactInfo.getMsisdnType()))
					{
						viewHolder.subText.append(" (" + mContactInfo.getMsisdnType() + ")");
					}
				}
			}
			break;
		case SHARED_MEDIA:
			viewHolder.text.setText(context.getString(R.string.shared_med));
			List<HikeSharedFile> sharedMedia = (List<HikeSharedFile>) ((ProfileSharedMedia) profileItem).getSharedFilesList();
			LinearLayout layout = (LinearLayout) viewHolder.infoContainer;
			int smSizeDb = ((ProfileSharedMedia) profileItem).getSharedMediaCount();
			int maxMediaToShow = ((ProfileSharedMedia) profileItem).getMaxMediaToShow();
			viewHolder.subText.setText(Integer.toString(smSizeDb));
			viewHolder.mediaLayout.setVisibility(View.VISIBLE);
			
			if(sharedMedia!= null && !sharedMedia.isEmpty())
			{	viewHolder.infoContainer.setVisibility(View.VISIBLE);
				viewHolder.parent.setVisibility(View.GONE);  //Empty state
				if(sharedMedia.size() <= maxMediaToShow)
				{
					loadMediaInProfile(sharedMedia.size(), layout, sharedMedia);
					
					int i = layout.getChildCount()-1;
					if(i >= smSizeDb)   //Safety check. This was failing in one case, hence the equality sign
					{
						while(i != smSizeDb - 1)  //Cleaning up previous items if any
						{
						if(layout.getChildAt(i) != null)
						{
							layout.removeViewAt(i);
						}
							i--;
						}
					}
					viewHolder.icon.setVisibility(View.GONE);
				}
				else
				{
					loadMediaInProfile(maxMediaToShow, layout, sharedMedia);
				}
				
				if(maxMediaToShow < smSizeDb )
				{
					// Add Arrow Icon
					viewHolder.icon.setVisibility(View.VISIBLE);
					viewHolder.icon.setTag(OPEN_GALLERY);
					viewHolder.icon.setOnClickListener(profileActivity);
				}
			}
			else
			{		//Empty State
				layout.removeAllViews();
				viewHolder.mediaLayout.setVisibility(View.GONE);
				viewHolder.parent.setVisibility(View.VISIBLE);
				viewHolder.infoContainer.setVisibility(View.GONE);
			}
			
			break;

		case SHARED_CONTENT:
			String heading = ((ProfileSharedContent)profileItem).getText();
			viewHolder.text.setText(heading);
			viewHolder.sharedFilesCount.setText(Integer.toString(((ProfileSharedContent) profileItem).getSharedFilesCount()));
			
			TextView countView = (TextView)v.findViewById(R.id.count_pin_unread);
			
			int pinUnreadCount = ((ProfileSharedContent)profileItem).getUnreadPinCount();
			
			if (pinUnreadCount > 0)
			{			
				viewHolder.extraInfo.setVisibility(View.GONE);
				
				countView.setVisibility(View.VISIBLE);
				
				if (pinUnreadCount >= HikeConstants.MAX_PIN_CONTENT_LINES_IN_HISTORY)
				{
					countView.setText(R.string.max_pin_unread_counter);
				}
				else
				{
					countView.setText(Integer.toString(pinUnreadCount));
				}
				if(((ProfileSharedContent)profileItem).getPinAnimation())
				{
					countView.startAnimation(Utils.getNotificationIndicatorAnim());

					((ProfileSharedContent)profileItem).setPinAnimation(false);
				}
			}
			else
			{			
				countView.setVisibility(View.GONE);
				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				viewHolder.extraInfo.setText(Integer.toString(((ProfileSharedContent) profileItem).getSharedPinsCount()));
			}
			
			int totalfiles = ((ProfileSharedContent) profileItem).getSharedFilesCount() + ((ProfileSharedContent) profileItem).getSharedPinsCount();
			int filesCount = ((ProfileSharedContent) profileItem).getSharedFilesCount();
			int pinsCount = ((ProfileSharedContent) profileItem).getSharedPinsCount();
			viewHolder.subText.setText(Integer.toString(totalfiles)); 

			if(groupProfile)
			{	
				/*
				 * We will remove these two lines when we will be add groups for 1:1
				 */
				
				if(totalfiles>0)
				{
					viewHolder.sharedFiles.setVisibility(View.VISIBLE);
					if (groupConversation instanceof BroadcastConversation)
					{
						((LinearLayout) viewHolder.sharedFiles).getChildAt(1).setVisibility(View.GONE);
					}
					else
					{
						((LinearLayout) viewHolder.sharedFiles).getChildAt(1).setVisibility(View.VISIBLE);
						((LinearLayout) viewHolder.sharedFiles).findViewById(R.id.shared_content_seprator).setVisibility(View.VISIBLE);
					}
				
					viewHolder.groupOrPins.setText(context.getResources().getString(R.string.pins));
					viewHolder.icon.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_pin_2));
					viewHolder.timeStamp.setVisibility(View.GONE);
					if(filesCount == 0)
						disableView(viewHolder.sharedFilesText, viewHolder.sharedFilesCount,viewHolder.files);
					if(pinsCount == 0)
						disableView(viewHolder.groupOrPins, viewHolder.extraInfo,viewHolder.pins);
					
				}
				else//EmptyState
				{
					viewHolder.sharedFiles.setVisibility(View.GONE);
					viewHolder.timeStamp.setVisibility(View.VISIBLE);
					if (groupConversation instanceof BroadcastConversation)
					{
						viewHolder.timeStamp.setText(context.getResources().getString(R.string.no_file_broadcast));
					}
					else
					{
						viewHolder.timeStamp.setText(context.getResources().getString(R.string.no_file));
					}
				}
			}
			else
			{
				/*
				 * We will remove these two lines when we will be add groups for 1:1
				 */
				android.widget.LinearLayout.LayoutParams ll;
				if(totalfiles > 0)			
				{	
					viewHolder.sharedFiles.setVisibility(View.VISIBLE);
					ll = (LayoutParams) viewHolder.sharedFiles.getLayoutParams();
					ll.topMargin = context.getResources().getDimensionPixelSize(R.dimen.top_margin_shared_content) * -1;  
					viewHolder.sharedFiles.setLayoutParams(ll);   //Hack for top margin
					
					((LinearLayout) viewHolder.sharedFiles).getChildAt(1).setVisibility(View.GONE);
					((LinearLayout) viewHolder.sharedFiles).findViewById(R.id.shared_content_seprator).setVisibility(View.GONE);
					viewHolder.groupOrPins.setText(context.getResources().getString(R.string.groups));
					viewHolder.icon.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_group_2));
					viewHolder.timeStamp.setVisibility(View.GONE);
					if(filesCount == 0)
						disableView(viewHolder.sharedFilesText, viewHolder.sharedFilesCount,viewHolder.files);
						
				}
				else //EmptyState
				{	
					viewHolder.sharedFiles.setVisibility(View.GONE);
					viewHolder.timeStamp.setVisibility(View.VISIBLE);
					
					viewHolder.timeStamp.setText(context.getResources().getString(R.string.no_file_profile));
				}
			}
			break;
		case GROUP_SETTINGS:
			try {
				if (groupConversation != null && groupConversation.getMetadata()
						.getAddMembersRight() == OneToNConversationMetadata.ADD_MEMBERS_RIGHTS.ADMIN_CAN_ADD) {
					viewHolder.checkbox.setChecked(true);
				}else{
					viewHolder.checkbox.setChecked(false);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			break;
		case PHONE_NUMBER:
			String head = context.getResources().getString(R.string.phone_pa);
			viewHolder.text.setText(head);
			viewHolder.extraInfo.setText(mContactInfo.getMsisdn());
			
			if (!TextUtils.isEmpty(mContactInfo.getMsisdnType()))
			{	
				viewHolder.subText.setText(mContactInfo.getMsisdnType());
				viewHolder.subText.setVisibility(View.VISIBLE);
			}
			
			else
			{
				viewHolder.subText.setVisibility(View.GONE);
			}

			if(!mContactInfo.isOnhike() || !Utils.isVoipActivated(context) || 
					(OfflineUtils.isConnectedToSameMsisdn(mContactInfo.getMsisdn()) && !OfflineUtils.isFeautureAvailable(OfflineConstants.OFFLINE_VERSION_NUMER, OfflineUtils.getConnectedDeviceVersion(), OfflineConstants.VOIP_HIKE_DIRECT)))
			{
				viewHolder.phoneIcon.setVisibility(View.GONE);
			}
			break;

		case MEMBERS:
			if (groupConversation instanceof BroadcastConversation)
			{
				viewHolder.text.setText(context.getResources().getString(R.string.recipients));
				viewHolder.subText.setText(Integer.toString(((ProfileGroupItem)profileItem).getTotalMembers()) + "/" + HikeConstants.MAX_CONTACTS_IN_BROADCAST);
			}
			else
			{
				viewHolder.text.setText(context.getResources().getString(R.string.members));
				viewHolder.subText.setText(Integer.toString(((ProfileGroupItem)profileItem).getTotalMembers()) + "/" + HikeConstants.MAX_CONTACTS_IN_GROUP);
			}
			break;

		case GROUP_PARTICIPANT:
			LinearLayout parentView = (LinearLayout) v;
			parentView.removeAllViews();
			PairModified<GroupParticipant, String> groupParticipants = ((ProfileGroupItem) profileItem).getGroupParticipant();
			parentView.setBackgroundColor(Color.WHITE);
			GroupParticipant groupParticipant = groupParticipants.getFirst();
			
			ContactInfo contactInfo = groupParticipant.getContactInfo();
			
			if (groupParticipant.isAdmin())	//If Admin ----> show admin
			{
				viewHolder.infoContainer.setVisibility(View.VISIBLE);
				viewHolder.infoContainer.findViewById(R.id.owner_indicator_text).setVisibility(View.VISIBLE);
				viewHolder.infoContainer.findViewById(R.id.sms_member_indicator_text).setVisibility(View.GONE);
				
			}
			//else if Already a Member ----> show nothing
			//else It a SMS member ----> show waiting
			else if(groupParticipant.isSMSGroupMember())
			{
				viewHolder.infoContainer.setVisibility(View.VISIBLE);
				viewHolder.infoContainer.findViewById(R.id.sms_member_indicator_text).setVisibility(View.VISIBLE);
				viewHolder.infoContainer.findViewById(R.id.owner_indicator_text).setVisibility(View.GONE);
			}
			else
			{
				viewHolder.infoContainer.setVisibility(View.GONE);
			}
						
			String groupParticipantName = groupParticipants.getSecond();
			if (null == groupParticipantName)
			{
				groupParticipantName = contactInfo.getFirstNameAndSurname();
			}
			if(!contactInfo.isUnknownContact())
			{	viewHolder.text.setText(groupParticipantName);
				viewHolder.phoneNumViewDivider.setVisibility(View.GONE);
				viewHolder.extraInfo.setVisibility(View.GONE);
			}
			else
			{
				viewHolder.phoneNumViewDivider.setVisibility(View.VISIBLE);
				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				viewHolder.text.setText(contactInfo.getMsisdn());
				viewHolder.extraInfo.setText(groupParticipantName);
			}
				
			setAvatar(contactInfo.getMsisdn(), viewHolder.icon);
			viewHolder.parent.setOnLongClickListener(profileActivity);
			viewHolder.parent.setTag(groupParticipant);

			viewHolder.parent.setOnClickListener(profileActivity);

			parentView.addView(viewHolder.parent);
			
			break;

		case ADD_MEMBERS:
			LinearLayout addMemberLayout = (LinearLayout) v;
			addMemberLayout.removeAllViews();
			View groupParticipantParentView_mem = inflater.inflate(R.layout.group_profile_item, addMemberLayout, false);
			View avatarContainer = groupParticipantParentView_mem.findViewById(R.id.avatar_container);
			avatarContainer.setVisibility(View.GONE);
			TextView nameTextView_mem = (TextView) groupParticipantParentView_mem.findViewById(R.id.name);
			ImageView avatar_mem = (ImageView) groupParticipantParentView_mem.findViewById(R.id.add_participant);
			avatar_mem.setVisibility(View.VISIBLE);
			if (groupConversation instanceof BroadcastConversation)
			{
				nameTextView_mem.setText(R.string.add_recipients);
			}
			else
			{
				nameTextView_mem.setText(R.string.add_people);
			}
			nameTextView_mem.setTextColor(context.getResources().getColor(R.color.blue_hike));
			groupParticipantParentView_mem.setTag(null);
			groupParticipantParentView_mem.setOnClickListener(profileActivity);
			addMemberLayout.addView(groupParticipantParentView_mem);

			break;
		case GROUP_RIGHTS_INFO:
			LinearLayout rightInfoLayout = (LinearLayout) v;
			rightInfoLayout.removeAllViews();
			View rightInfoParentView = inflater.inflate(R.layout.group_profile_item, rightInfoLayout, false);
			View infoContainer = rightInfoParentView.findViewById(R.id.avatar_container);
			infoContainer.setVisibility(View.GONE);
			TextView nameTextView = (TextView) rightInfoParentView.findViewById(R.id.name);
			ImageView infoSign = (ImageView) rightInfoParentView.findViewById(R.id.add_participant);
			infoSign.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_chat_theme_info));
			infoSign.setVisibility(View.VISIBLE);
			nameTextView.setTextSize(12);
			nameTextView.setText(context.getResources().getString(R.string.group_rights_info));
			rightInfoLayout.setClickable(false);
			rightInfoLayout.addView( rightInfoParentView);
			
			
			
			break;
		case STATUS:
			StatusMessage statusMessage = ((ProfileStatusItem) profileItem).getStatusMessage();
			viewHolder.text.setText(myProfile ? context.getString(R.string.me) : statusMessage.getNotNullName());

			if (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED
					|| statusMessage.getStatusMessageType() == StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST)
			{
				boolean friendRequestAccepted = statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED;

				int infoMainResId = friendRequestAccepted ? R.string.accepted_your_favorite_request_details:R.string.you_accepted_favorite_request_details;
				String infoSubText = context.getString(Utils.isLastSeenSetToFavorite() ? R.string.both_ls_status_update : R.string.status_updates_proper_casing);
				viewHolder.subText.setText(context.getString(infoMainResId, Utils.getFirstName(statusMessage.getNotNullName()), infoSubText));
			}
			else
			{
				SmileyParser smileyParser = SmileyParser.getInstance();
				viewHolder.subText.setText(smileyParser.addSmileySpans(statusMessage.getText(), true));
			}

			Linkify.addLinks(viewHolder.subText, Linkify.ALL);
			viewHolder.text.setMovementMethod(null);

			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true, context));

			if (statusMessage.hasMood())
			{
				viewHolder.icon.setImageResource(EmoticonConstants.moodMapping.get(statusMessage.getMoodId()));
				viewHolder.iconFrame.setVisibility(View.GONE);
				viewHolder.icon.setBackgroundResource(0);
			}
			else
			{
				setAvatar(statusMessage.getMsisdn(), viewHolder.icon);
				viewHolder.iconFrame.setVisibility(View.VISIBLE);
			}
			break;

		case IMAGE_POST:
		case PROFILE_PIC_UPDATE:
		case TEXT_IMAGE_POST:
			StatusMessage profilePicStatusUpdate = ((ProfileStatusItem) profileItem).getStatusMessage();
			
			viewHolder.text.setText(myProfile ? context.getString(R.string.me) : profilePicStatusUpdate.getNotNullName());

			if (TextUtils.isEmpty(profilePicStatusUpdate.getText()))
			{
				viewHolder.subText.setText(R.string.status_profile_pic_notification);
			}
			else
			{
				viewHolder.subText.setText(profilePicStatusUpdate.getText());
			}
			
			setAvatar(profilePicStatusUpdate.getMsisdn(), viewHolder.icon);

			ImageViewerInfo imageViewerInfo2 = new ImageViewerInfo(profilePicStatusUpdate.getMappedId(), null, true);

			viewHolder.image.setTag(imageViewerInfo2);

			bigPicImageLoader.loadImage(profilePicStatusUpdate.getMappedId(), viewHolder.image, isListFlinging);

			viewHolder.timeStamp.setText(profilePicStatusUpdate.getTimestampFormatted(true, context));

			viewHolder.infoContainer.setTag(profilePicStatusUpdate);
			viewHolder.infoContainer.setOnLongClickListener(profileActivity);
			break;

			case PRIVACY_SECTION:

				viewHolder.expandingLayout.setExpandedHeight(Utils.dpToPx(115));
				viewHolder.expandingLayout.setSizeChangedListener(profileItem);

				if (!profileItem.isExpanded()) {
					viewHolder.expandingLayout.setVisibility(View.GONE);
				} else {
					viewHolder.expandingLayout.setVisibility(View.VISIBLE);
				}

				viewHolder.parent.setTag(position);
				viewHolder.parent.setOnClickListener(this);
				viewHolder.statusUpdateSetting.setOnClickListener(this);
				viewHolder.lastSeenSetting.setOnClickListener(this);
				viewHolder.lastSeenSwitch.setOnCheckedChangeListener(this);
				viewHolder.statusUpdateSwitch.setOnCheckedChangeListener(this);

				break;
		}

		if (viewHolder.parent != null)
		{
			int bottomPadding;

			if (position == getCount() - 1)
			{
				bottomPadding = context.getResources().getDimensionPixelSize(R.dimen.updates_margin);
			}
			else
			{
				bottomPadding = 0;
			}

			viewHolder.parent.setPadding(0, 0, 0, bottomPadding);
		}

		if (!profileItem.isExpanded()) {
			profileItem.setCollapsedHeight(v.getHeight());
		}

		return v;
	}

	private void disableView(TextView text, TextView count, View background)
	{
		text.setTextColor(context.getResources().getColor(R.color.files_disabled));
		count.setTextColor(context.getResources().getColor(R.color.files_disabled));
		background.setBackgroundDrawable(null);   //Removing the pressed state
	}

	private void loadMediaInProfile(int size, LinearLayout layout, List<HikeSharedFile> sharedMedia)
	{
		int i = 0;
		while(i<size)
		{
			HikeSharedFile galleryItem = sharedMedia.get(i);
			View image_thumb = layout.getChildAt(i);
			View image_duration = image_thumb.findViewById(R.id.vid_time_layout);
			View fileMissing = image_thumb.findViewById(R.id.file_missing_layout);
			if(galleryItem.getHikeFileType() == HikeFileType.VIDEO)
			{
				image_duration.setVisibility(View.VISIBLE);
			}
			
			ImageView image = (ImageView) image_thumb.findViewById(R.id.thumbnail);
			if(galleryItem.getFileFromExactFilePath().exists())
			{
				thumbnailLoader.loadImage(galleryItem.getImageLoaderKey(false), image);
				fileMissing.setVisibility(View.GONE);

				if (galleryItem.getHikeFileType() == HikeFileType.VIDEO)
				{
					image_duration.setVisibility(View.VISIBLE);
				}
				else
				{
					image_duration.setVisibility(View.GONE);
				}
			}
			else
			{
				image_duration.setVisibility(View.GONE);
				fileMissing.setVisibility(View.VISIBLE);
			}
			
			image_thumb.setTag(galleryItem);
			image_thumb.setOnClickListener(profileActivity);
			i++;
		}
	}

	private void setAvatar(String msisdn, ImageView avatarView)
	{
		iconLoader.loadImage(msisdn, avatarView, false, true);
	}

	private class ViewHolder
	{
		TextView text;

		TextView subText;

		TextView extraInfo;
		
		TextView sharedFilesCount;
		
		TextView groupOrPins;
		
		TextView sharedFilesText;

		ImageView image;

		ImageView icon;

		ImageView iconFrame;

		ImageView phoneIcon;

		TextView timeStamp;

		View infoContainer;

		View parent;
		
		View phoneNumViewDivider;
		
		View sharedFiles;
		
		View files;
		
		View pins;
		
		View mediaLayout;
		
		CheckBox checkbox;

		ExpandingLayout expandingLayout;

		SwitchCompat lastSeenSwitch, statusUpdateSwitch;

		View lastSeenSetting, statusUpdateSetting;
	}
	
	public void setProfilePreview(Bitmap preview)
	{
		this.profilePreview = preview;
		notifyDataSetChanged();
	}

	public void updateGroupConversation(OneToNConversation groupConversation)
	{
		this.groupConversation = groupConversation;
		notifyDataSetChanged();
	}

	public void updateContactInfo(ContactInfo contactInfo)
	{
		this.mContactInfo = contactInfo;
		notifyDataSetChanged();
	}

	public void setIsContactBlocked(boolean b)
	{
		isContactBlocked = b;
	}

	public boolean isContactBlocked()
	{
		return isContactBlocked;
	}

	public TimelineUpdatesImageLoader getTimelineImageLoader()
	{
		return bigPicImageLoader;
	}

	public IconLoader getIconImageLoader()
	{
		return iconLoader;
	}
	
	public ProfilePicImageLoader getProfilePicImageLoader()
	{
		return profileImageLoader;
	}
	
	public SharedFileImageLoader getSharedFileImageLoader()
	{
		return thumbnailLoader; 
	}

	private boolean isListFlinging;

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;

		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}
	

	private boolean getHasCustomPhoto()
	{
		// basically for the case of unknown number contactInfo object doesn't have the hasIcon information
		if(mContactInfo != null)
		{
			return this.mContactInfo.hasCustomPhoto() || ContactManager.getInstance().hasIcon(this.mContactInfo.getMsisdn());	
		}
		else 
		{
			return false;
		}
	}
	
	public void updateHasCustomPhoto()
	{
		this.hasCustomPhoto = getHasCustomPhoto();
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v The view that was clicked.
	 */
	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.privacy_parent_layout:
				int position = (int) v.getTag();
				ExpandableListItem item = getItem(position);

				if (!item.isExpanded()) {
					((ExpandingListView) listView).expandView((View) v.getParent());
				} else {
					((ExpandingListView) listView).collapseView((View) v.getParent());
				}
				break;

			case R.id.last_seen_section:
				toggleLastSeenPrivacyForUser(v);
				break;

			case R.id.status_update_section:
				toggleStatusUpdatePrivacyForUser(v);
				break;
		}
	}

	/**
	 * Called when the checked state of a compound button has changed.
	 *
	 * @param buttonView The compound button view whose state has changed.
	 * @param isChecked  The new checked state of buttonView.
	 */
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

		switch (buttonView.getId()) {
			case R.id.last_seen_switch:
				ContactManager.getInstance().toggleLastSeenSetting(mContactInfo, isChecked);
				break;

			case R.id.status_update_switch:
				ContactManager.getInstance().toggleStatusUpdateSetting(mContactInfo, isChecked);
				break;
		}

	}

	private void toggleLastSeenPrivacyForUser(View v) {
		SwitchCompat switchCompat = (SwitchCompat) v.findViewById(R.id.last_seen_switch);
		switchCompat.setChecked(!switchCompat.isChecked());
		// This will invoke onCheckedChanged, where the business logic resides for sending MQTT Packet
	}

	private void toggleStatusUpdatePrivacyForUser(View v) {
		SwitchCompat switchCompat = (SwitchCompat) v.findViewById(R.id.status_update_switch);
		switchCompat.setChecked(!switchCompat.isChecked());
		// This will invoke onCheckedChanged, where the business logic resides for sending MQTT Packet
	}

}
