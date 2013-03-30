package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

@SuppressWarnings("unchecked")
public class ProfileAdapter extends BaseAdapter {

	public static final int PROFILE_HEADER_ID = -1;
	public static final int PROFILE_BUTTON_ID = -2;
	public static final int PROFILE_EMPTY_ID = -3;

	public static final String GROUP_HEADER_ID = "-1";
	public static final String GROUP_BUTTON_ID = "-2";

	private static enum ViewType {
		HEADER, BUTTONS, STATUS, GROUP_PARTICIPANT, EMPTY_STATUS
	}

	private Context context;
	private List<GroupParticipant> groupParticipants;
	private List<StatusMessage> statusMessages;
	private GroupConversation groupConversation;
	private ContactInfo mContactInfo;
	private Bitmap profilePreview;
	private boolean groupProfile;
	private boolean myProfile;
	private boolean hasSMSUser;
	private int numParticipants;
	private boolean isContactBlocked;

	public ProfileAdapter(Context context, List<?> itemList,
			GroupConversation groupConversation, ContactInfo contactInfo,
			boolean myProfile) {
		this(context, itemList, groupConversation, contactInfo, myProfile,
				false);
	}

	public ProfileAdapter(Context context, List<?> itemList,
			GroupConversation groupConversation, ContactInfo contactInfo,
			boolean myProfile, boolean isContactBlocked) {
		this.context = context;
		this.groupProfile = groupConversation != null;
		if (groupProfile) {
			groupParticipants = (List<GroupParticipant>) itemList;
		} else {
			statusMessages = (List<StatusMessage>) itemList;
		}
		this.mContactInfo = contactInfo;
		this.groupConversation = groupConversation;
		this.myProfile = myProfile;
		this.isContactBlocked = isContactBlocked;
	}

	@Override
	public int getItemViewType(int position) {
		ViewType viewType;
		if (groupProfile) {
			ContactInfo contactInfo = groupParticipants.get(position)
					.getContactInfo();
			if (GROUP_HEADER_ID.equals(contactInfo.getId())) {
				viewType = ViewType.HEADER;
			} else if (GROUP_BUTTON_ID.equals(contactInfo.getId())) {
				viewType = ViewType.BUTTONS;
			} else {
				viewType = ViewType.GROUP_PARTICIPANT;
			}
		} else {
			StatusMessage statusMessage = statusMessages.get(position);
			if (PROFILE_HEADER_ID == statusMessage.getId()) {
				viewType = ViewType.HEADER;
			} else if (PROFILE_BUTTON_ID == statusMessage.getId()) {
				viewType = ViewType.BUTTONS;
			} else if (PROFILE_EMPTY_ID == statusMessage.getId()) {
				viewType = ViewType.EMPTY_STATUS;
			} else {
				viewType = ViewType.STATUS;
			}
		}
		return viewType.ordinal();
	}

	@Override
	public int getViewTypeCount() {
		return ViewType.values().length;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		/*
		 * We got an IndexOutOfBoundsException here.
		 */
		if (position >= getCount()) {
			return false;
		}
		ViewType viewType = ViewType.values()[getItemViewType(position)];
		if (viewType == ViewType.HEADER) {
			return false;
		} else if (viewType == ViewType.BUTTONS) {
			return false;
		}
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		Object item = getItem(position);
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		GroupParticipant groupParticipant = null;
		StatusMessage statusMessage = null;

		if (item instanceof GroupParticipant) {
			groupParticipant = (GroupParticipant) item;
		} else {
			statusMessage = (StatusMessage) item;
		}

		ViewHolder viewHolder = null;
		View v = convertView;

		if (v == null) {
			viewHolder = new ViewHolder();

			switch (viewType) {
			case HEADER:
				v = inflater.inflate(R.layout.profile_header, null);

				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.info);

				viewHolder.image = (ImageView) v.findViewById(R.id.profile);
				viewHolder.icon = (ImageView) v
						.findViewById(R.id.change_profile);
				viewHolder.editGroupName = (ImageButton) v
						.findViewById(R.id.edit_group_name);
				viewHolder.requestLayout = (ViewGroup) v
						.findViewById(R.id.request_layout);

				viewHolder.requestMain = (TextView) v
						.findViewById(R.id.req_main);
				viewHolder.requestInfo = (TextView) v
						.findViewById(R.id.req_info);

				viewHolder.btn1 = (Button) v.findViewById(R.id.yes_btn);
				viewHolder.btn2 = (Button) v.findViewById(R.id.no_btn);
				break;

			case BUTTONS:
				v = inflater.inflate(R.layout.profile_btns_item, null);

				viewHolder.btn1 = (Button) v.findViewById(R.id.btn1);
				viewHolder.btn2 = (Button) v.findViewById(R.id.btn2);
				viewHolder.btn3 = (Button) v.findViewById(R.id.btn3);

				viewHolder.btnDivider = v.findViewById(R.id.btn_divider);
				viewHolder.marginView = v.findViewById(R.id.margin_view);

				viewHolder.container = (ViewGroup) v
						.findViewById(R.id.btn_container);
				break;

			case GROUP_PARTICIPANT:
				v = inflater.inflate(R.layout.group_profile_item, null);

				viewHolder.text = (TextView) v.findViewById(R.id.name);
				viewHolder.subText = (TextView) v.findViewById(R.id.info_txt);

				viewHolder.image = (ImageView) v.findViewById(R.id.avatar);
				viewHolder.icon = (ImageView) v
						.findViewById(R.id.unknown_contact);
				break;

			case STATUS:
				v = inflater.inflate(R.layout.profile_timeline_item, null);

				viewHolder.text = (TextView) v.findViewById(R.id.status_text);
				viewHolder.subText = (TextView) v.findViewById(R.id.time);

				viewHolder.image = (ImageView) v.findViewById(R.id.status_pic);
				viewHolder.icon = (ImageView) v.findViewById(R.id.status_type);

				viewHolder.container = (ViewGroup) v.findViewById(R.id.content);
				viewHolder.contentContainer = (ViewGroup) v
						.findViewById(R.id.content_container);
				break;

			case EMPTY_STATUS:
				v = inflater.inflate(R.layout.profile_timeline_negative_item,
						null);

				viewHolder.text = (TextView) v.findViewById(R.id.info);
				viewHolder.icon = (ImageView) v.findViewById(R.id.icon);
				viewHolder.btn1 = (Button) v.findViewById(R.id.btn);
				viewHolder.btn2 = (Button) v
						.findViewById(R.id.add_sms_friend_btn);
				break;
			}

			v.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) v.getTag();
		}

		switch (viewType) {
		case HEADER:
			String msisdn;
			String name;

			if (!myProfile
					&& (groupConversation != null || mContactInfo
							.getFavoriteType() != FavoriteType.FRIEND)) {
				v.setBackgroundResource(R.drawable.bg_group_profile);
			} else {
				v.setBackgroundResource(R.drawable.bg_profile);
			}

			if (groupParticipant != null) {
				msisdn = groupConversation.getMsisdn();
				name = groupConversation.getLabel();
				viewHolder.editGroupName.setVisibility(View.VISIBLE);
			} else {
				msisdn = mContactInfo.getMsisdn();
				name = TextUtils.isEmpty(mContactInfo.getName()) ? mContactInfo
						.getMsisdn() : mContactInfo.getName();
				viewHolder.editGroupName.setVisibility(View.GONE);
			}

			viewHolder.text.setText(name);
			if (profilePreview == null) {
				viewHolder.image.setImageDrawable(IconCacheManager
						.getInstance().getIconForMSISDN(msisdn));
			} else {
				viewHolder.image.setImageBitmap(profilePreview);
			}
			viewHolder.image.setTag(statusMessage);
			viewHolder.subText.setVisibility(View.GONE);
			viewHolder.icon
					.setVisibility((myProfile || groupConversation != null) ? View.VISIBLE
							: View.GONE);
			if (mContactInfo != null) {
				if (mContactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED) {
					viewHolder.requestLayout.setVisibility(View.VISIBLE);
					viewHolder.requestInfo.setVisibility(View.VISIBLE);
					viewHolder.requestMain.setVisibility(View.VISIBLE);
					viewHolder.btn2.setVisibility(View.VISIBLE);

					viewHolder.requestMain.setText(context.getString(
							R.string.added_as_friend, name));
					viewHolder.requestInfo.setText(context.getString(
							R.string.added_as_hike_friend_info,
							mContactInfo.getFirstName()));

					viewHolder.btn1.setText(R.string.confirm);
					viewHolder.btn1.setTag(mContactInfo);
					viewHolder.btn2.setTag(mContactInfo);
				} else if (mContactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED) {
					viewHolder.requestLayout.setVisibility(View.VISIBLE);
					viewHolder.requestInfo.setVisibility(View.GONE);
					viewHolder.requestMain.setVisibility(View.GONE);
					viewHolder.btn2.setVisibility(View.GONE);

					viewHolder.btn1.setText(R.string.add_as_friend);
					viewHolder.btn1.setTag(mContactInfo);
				} else {
					viewHolder.requestLayout.setVisibility(View.GONE);
				}
			}
			if (mContactInfo != null) {
				if (mContactInfo.isOnhike()) {
					if (mContactInfo.getHikeJoinTime() > 0) {
						viewHolder.subText.setVisibility(View.VISIBLE);
						viewHolder.subText.setText(context.getString(
								R.string.on_hike_since,
								mContactInfo.getFormattedHikeJoinTime()));
					} else {
						viewHolder.subText.setVisibility(View.GONE);
					}
				} else {
					viewHolder.subText.setVisibility(View.VISIBLE);
					viewHolder.subText.setText(R.string.on_sms);
				}
			}

			break;

		case BUTTONS:
			if (groupParticipant != null) {
				viewHolder.btn1
						.setVisibility(numParticipants < HikeConstants.MAX_CONTACTS_IN_GROUP ? View.VISIBLE
								: View.GONE);
				viewHolder.btn2.setVisibility(View.VISIBLE);
				viewHolder.btn3.setVisibility(hasSMSUser ? View.VISIBLE
						: View.GONE);
				viewHolder.marginView.setVisibility(hasSMSUser ? View.VISIBLE
						: View.GONE);

				viewHolder.btnDivider
						.setVisibility(viewHolder.btn1.getVisibility() == View.VISIBLE
								&& viewHolder.btn2.getVisibility() == View.VISIBLE ? View.VISIBLE
								: View.GONE);

				viewHolder.btn1.setText(R.string.add_member);
				viewHolder.btn2
						.setText(groupConversation.isMuted() ? R.string.unmute_group
								: R.string.mute_group);
				viewHolder.btn3.setText(R.string.invite_all_members);

				viewHolder.btn1.setCompoundDrawablesWithIntrinsicBounds(
						R.drawable.ic_add_member, 0, 0, 0);
				viewHolder.btn2.setCompoundDrawablesWithIntrinsicBounds(
						groupConversation.isMuted() ? R.drawable.ic_unmute
								: R.drawable.ic_mute, 0, 0, 0);
				viewHolder.btn1.setCompoundDrawablesWithIntrinsicBounds(
						R.drawable.ic_invite_all_members, 0, 0, 0);

				v.setBackgroundResource(R.drawable.thatch_repeat);
			} else {
				viewHolder.btn1.setVisibility(View.GONE);
				viewHolder.btn2.setVisibility(View.GONE);
				viewHolder.container.setVisibility(View.GONE);

				viewHolder.marginView.setVisibility(View.GONE);

				viewHolder.btn3.setText(myProfile ? R.string.post_status
						: R.string.send_message);
				viewHolder.btn3.setCompoundDrawablesWithIntrinsicBounds(
						myProfile ? R.drawable.ic_post_status
								: R.drawable.ic_msg_small, 0, 0, 0);
				v.setBackgroundResource(R.color.seen_timeline_item);
			}
			break;

		case GROUP_PARTICIPANT:
			ContactInfo contactInfo = groupParticipant.getContactInfo();

			viewHolder.image.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(contactInfo.getMsisdn()));

			viewHolder.text.setText(contactInfo.getName());
			if (contactInfo.isUnknownContact()) {
				viewHolder.text.append(" (" + contactInfo.getMsisdn() + ")");
			}

			viewHolder.subText.setVisibility(View.VISIBLE);
			if (groupConversation.getGroupOwner().equals(
					contactInfo.getMsisdn())) {
				viewHolder.subText.setText(R.string.owner);
			} else if (!contactInfo.isOnhike()) {
				viewHolder.subText
						.setText(groupParticipant.onDnd() ? R.string.on_dnd
								: R.string.on_sms);
			} else {
				viewHolder.subText.setVisibility(View.GONE);
			}

			viewHolder.icon.setVisibility(View.GONE);
			break;

		case STATUS:
			if (myProfile) {
				viewHolder.contentContainer
						.setBackgroundResource(R.drawable.seen_timeline_selector);
			}
			SmileyParser smileyParser = SmileyParser.getInstance();
			viewHolder.text.setText(smileyParser.addSmileySpans(
					statusMessage.getText(), true));
			viewHolder.image.setVisibility(View.GONE);
			viewHolder.subText.setText(statusMessage
					.getTimestampFormatted(true));
			viewHolder.icon.setImageResource(R.drawable.ic_text_status);
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC) {
				viewHolder.image.setVisibility(View.VISIBLE);
				viewHolder.image.setImageDrawable(IconCacheManager
						.getInstance().getIconForMSISDN(
								statusMessage.getMappedId()));
				viewHolder.icon
						.setImageResource(R.drawable.ic_profile_pic_status);
				viewHolder.image.setId(position);
				viewHolder.image.setTag(statusMessage);
				viewHolder.text.setText(R.string.changed_profile);
			} else if (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED
					|| statusMessage.getStatusMessageType() == StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST) {
				viewHolder.icon
						.setImageResource(R.drawable.ic_profile_pic_status);
			}
			if (statusMessage.hasMood()) {
				viewHolder.icon.setBackgroundDrawable(null);
				viewHolder.icon.setImageResource(Utils
						.getMoodsResource()[statusMessage.getMoodId()]);
			} else {
				viewHolder.icon
						.setBackgroundResource(R.drawable.bg_status_type);
			}
			LayoutParams lp = (LayoutParams) viewHolder.subText.getLayoutParams();
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC
					|| viewHolder.text.getLineCount() > 1) {
				lp.topMargin = 0;
				viewHolder.subText.setLayoutParams(lp);
			} else {
				lp.topMargin = (int) (5 * Utils.densityMultiplier);
				viewHolder.subText.setLayoutParams(lp);
			}
			break;

		case EMPTY_STATUS:
			String contactName = TextUtils.isEmpty(mContactInfo.getName()) ? mContactInfo
					.getMsisdn() : mContactInfo.getName();

			if (!isContactBlocked) {
				if (mContactInfo.isOnhike()) {
					viewHolder.btn2.setVisibility(View.GONE);
					viewHolder.icon.setImageResource(R.drawable.ic_not_friend);
					if (mContactInfo.getFavoriteType() == FavoriteType.REQUEST_SENT) {
						viewHolder.text.setText(context.getString(
								R.string.waiting_for_accept, contactName));
						viewHolder.btn1.setVisibility(View.GONE);
					} else {
						viewHolder.text.setText(context.getString(
								R.string.add_as_friend_info, contactName));
						viewHolder.btn1.setText(R.string.add_as_friend);
						viewHolder.btn1.setVisibility(View.VISIBLE);
					}
				} else {
					viewHolder.icon.setImageResource(R.drawable.ic_not_on_hike);
					viewHolder.text.setText(context.getString(
							R.string.not_on_hike, contactName));
					viewHolder.btn1.setText(R.string.invite_to_hike);
					viewHolder.btn2
							.setVisibility(mContactInfo.getFavoriteType() == FavoriteType.NOT_FRIEND ? View.VISIBLE
									: View.GONE);
				}
			} else {
				viewHolder.icon.setImageResource(R.drawable.ic_block_profile);
				viewHolder.text.setText(context.getString(
						R.string.user_blocked, contactName));
				viewHolder.btn1.setText(R.string.unblock_title);
				viewHolder.btn2.setVisibility(View.GONE);
			}

			break;
		}

		return v;
	}

	private class ViewHolder {
		TextView text;
		TextView subText;
		TextView requestMain;
		TextView requestInfo;
		ImageView image;
		ImageView icon;
		Button btn1;
		Button btn2;
		Button btn3;
		ViewGroup container;
		View btnDivider;
		ImageButton editGroupName;
		ViewGroup requestLayout;
		View marginView;
		ViewGroup contentContainer;
	}

	@Override
	public int getCount() {
		return groupProfile ? groupParticipants.size() : statusMessages.size();
	}

	@Override
	public Object getItem(int position) {
		return groupProfile ? groupParticipants.get(position) : statusMessages
				.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	public void setProfilePreview(Bitmap preview) {
		this.profilePreview = preview;
		notifyDataSetChanged();
	}

	public void updateGroupConversation(GroupConversation groupConversation) {
		this.groupConversation = groupConversation;
		notifyDataSetChanged();
	}

	public void updateContactInfo(ContactInfo contactInfo) {
		this.mContactInfo = contactInfo;
		notifyDataSetChanged();
	}

	public void setHasSmsUser(boolean hasSMSUser) {
		this.hasSMSUser = hasSMSUser;
	}

	public void setNumParticipants(int numParticipants) {
		this.numParticipants = numParticipants;
	}

	public void setIsContactBlocked(boolean b) {
		isContactBlocked = b;
	}

	public boolean isContactBlocked() {
		return isContactBlocked;
	}
}