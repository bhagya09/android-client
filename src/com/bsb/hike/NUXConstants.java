package com.bsb.hike;


public class NUXConstants
{

	public static final String NUX_SHARED_PREF = "nux_shared_pref";

	public static final String TAG = "NUXMANAGER";

	public static final String IS_NUX_ACTIVE = "is_nux_active";

	public static final String CURRENT_NUX_CONTACTS = "current_nux_contacts";

	public static final String UNLOCKED_NUX_CONTACTS = "unlocked_nux_contacts";
	
	public static final String STRING_SPLIT_SEPERATOR = ", ";

	public static final String INVITEFRDS_MAIN_TITLE = "ttl";

	public static final String INVITEFRDS_TEXT = "txt";

	public static final String INVITEFRDS_BUT_TEXT = "btnTxt";

	public static final String INVITEFRDS_SKIP_TOGGLE_BUTTON = "nxTgl";

	public static final String INVITEFRDS_IMAGE = "img";
	
	public static final String INVITE_NUX_IS_SKIPPABLE = "isSkpble";

	public static final String INVITE_FRIENDS = "s1";

	public static final String SELECT_FRIENDS = "s2";

	public static final String SF_SECTION_TITLE = "ttl1";

	public static final String SF_SECTION_TITLE2 = "ttl2";

	public static final String SF_SECTION_TITLE3 = "ttl3";

	public static final String SF_RECO_SECTION_TITLE = "recTitle";

	public static final String SF_RECO_TOGGLE = "recTgl";

	public static final String SF_HIDE_LIST = "hdeLst";

	public static final String SF_CONTACT_SECTION_TOGGLE = "contact_section_toggle";

	public static final String SF_BUTTON_TEXT = "nxTxt";

	public static final String SF_MODULE_TOGGLE = "mdlTgl";

	public static final String SF_CONTACT_SECTION_TYPE = "cntctTyp";

	public static final String CUSTOM_MESSAGE = "s3";

	public static final String CM_SCREEN_TOGGLE = "scrnTgl";

	public static final String CM_DEF_MESSAGE = "defMsg";

	public static final String CM_BUTTON_TEXT = "nxTxt";

	public static final String CM_HINT = "hnt";

	public static final String CM_STICKER_LIST = "stkrLst";

	public static final String TASK_DETAILS = "task";

	public static final String TD_INCR_MIN = "incr_min";

	public static final String TD_INCR_MAX = "incr_max";

	public static final String TD_INCENTIVE_ID = "incentive";

	public static final String TD_ACTIVITY_ID = "activity";

	public static final String TD_MIN_CONTACTS = "min";

	public static final String TD_MAX_CONTACTS = "max";
	
	public static final String TD_PKT_CODE = "pktId";
	
	public static final String CHAT_REWARDS_BAR = "s4";

	public static final String CR_REWARD_CARD_TEXT = "rwdCrdTxt";

	public static final String CR_REWARD_CARD_SUCCESS_TEXT = "rwdCrdTxtSccss";

	public static final String CR_STATUS_TEXT = "sttsTxt";

	public static final String CR_CHAT_WAITING_TEXT = "chtWtngTxt";

	public static final String CR_PENDINGCHAT_ICON = "pndngChtIcn";

	public static final String CR_DETAILS_LINK = "dtlsLnk";

	public static final String CR_DETAILS_TEXT = "dtlsTxt";

	public static final String CR_INVITE_MORE_TEXT = "bttn2Txt";

	public static final String CR_REMIND_TEXT = "bttn1Txt";

	public static final String CR_TAPTOCLAIM = "tpToClmLnk";
	
	public static final String CR_TAPTOCLAIMTEXT="sttsTxtSccss";
	
	public static final String CR_SELECTFRIENDS="bttn0Txt";

	public static final String CURRENT_NUX_ACTIVITY = "current_nux_activity";

	public static final int NUX_NEW = 1;

	public static final int NUX_KILLED = 2;

	public static final int NUX_SKIPPED = 4;

	public static final int NUX_IS_ACTIVE = 8;
	
	public static final int COMPLETED = 16;

	public static final int OTHER_ACTIVITY = -1;

	public static final String SUBTYPE_NEW = "nuxNew";

	public static final String SCREENS = "s";

	public static final String KILLNUX = "nuxKill";

	public static final String REFRESH = "nuxRfsh";

	public static final String UNLOCK = "nuxUL";

	public static final String REMINDER = "nuxRmndr";

	public static final String CONTACTS_SECTION_TYPE_HIKE = "hike";

	public static final String CONTACTS_SECTION_TYPE_ALL = "all";

	public static final String CONTACTS_SECTION_TYPE_NONHIKE = "nonhike";

	public static final String CONTACTS_SECTION_TYPE_BOTH = "both";

	public static final String CONTACTS_SECTION_TYPE_NONE = "none";

	public static final String INVITE = "nuxInvt";

	public static final String INVITE_ARRAY = "invites";

	public static final String CURRENT_PERSONS_UNLOCKED="current_persons_unlocked";
	
	public static final String UNLOCK_PERSONS="msisdns";

	public static final String ID="_id";

	public static final String TYPE="type";

	public static final String INCENTIVE_AMOUNT="recharge";

	public static final String SHOW_REWARDS="rul";
	
	public static final String REMINDER_RECEIVED="rem_rec";
	
	public static final String SELECTED_FRIENDS="selected_friends";
	
	public static final String NOTIFICATION_PKT="notif";
	
	public static final String PUSH_TYPE="push";
	
	public static final String PUSH_TITLE="pushTtl";
	
	public static final String PUSH_TEXT="pushTxt";
	
	public static final String PUSH_REWARD_CARD_TYPE="rwdCrdTyp";
	
	public static final String REMINDER_NORMAL = "rem_normal";
	
	public static final String VIEW_DETAILS_URL="http://api.im.hike.in/nuxrewards/details/%1$s";
	
	public static final String TAP_CLAIM_URL="http://api.im.hike.in/nuxrewards/claim/%1$s";

	public static final String OTHER_STRING = "od1";
	
	public static final String NUXREMINDTOSERVER="nuxInvRmndr";
	
	public static enum PushTypeEnum
	{
		NONE(0), SILENT(1), PUSH(2), UNKNOWN(-1);
		int val;

		private PushTypeEnum(int val)
		{
			this.val = val;
		}

		public static PushTypeEnum getEnumValue(int val)
		{
			for (PushTypeEnum enum1 : PushTypeEnum.values())
			{
				if (enum1.val == val)
					return enum1;

			}
			return UNKNOWN;
		}
	}

	public static enum RewardTypeEnum
	{
		COMPRESSED(0), NORMAL(1), EXPANDED(2), UNKNOWN(-1);
		int val;

		private RewardTypeEnum(int val)
		{
			this.val = val;
		}

		public static RewardTypeEnum getEnumValue(int val)
		{
			for (RewardTypeEnum enum1 : RewardTypeEnum.values())
			{
				if (enum1.val == val)
					return enum1;

			}
			return UNKNOWN;
		}
	}
	
	public static enum ContactSectionTypeEnum
    {
        all(0), hike(1), nonhike(2), both(3), none(4), unknown(-1);

        private int value;

        public int getValue()
        {
            return value;
        }

        private ContactSectionTypeEnum(int value)
        {
            this.value = value;
        }

        public static ContactSectionTypeEnum getEnum(int value)
        {
            for (ContactSectionTypeEnum enum1 : ContactSectionTypeEnum.values())
                if (enum1.value == value)
                    return enum1;
            return unknown;
        }
    }

}
