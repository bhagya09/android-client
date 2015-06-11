/**
 * File   : StickerSearchHostManager.java
 * Content: It is a provider class to host all kinds of chat search demands.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.utils.Logger;

import android.content.Context;
import android.support.v4.util.Pair;
import android.text.TextUtils;

public class StickerSearchHostManager {

	private static final String TAG = StickerSearchHostManager.class.getSimpleName();

	private static final int MAXIMUM_STICKER_CAPACITY = 10;

	private Context mContext;
	private String mCurrentText;
	private int mCurrentTextSignificantLength;
	private ArrayList<String> mPreviousWords;
	private IndividualChatProfile mCurrentIndividualChatProfile;
	private GroupChatProfile mCurrentGroupChatProfile;

	private static LinkedList<Word> sWords;
	private static ArrayList<Sticker> sStickers;

	private static HashMap<String, IndividualChatProfile> sIndividualChatRecord;
	private static HashMap<String, GroupChatProfile> sGroupChatRecord;

	private static final Object sHostInitLock = new Object();
	private static final Object sHostOperateLock = new Object();

	private static volatile boolean sIsHostFinishingSearchTask;
	private static volatile StickerSearchHostManager sStickerSearchHostManager;

	private StickerSearchHostManager(Context context) {
		mContext = context;
		sWords = new LinkedList<Word>();
		sStickers = new ArrayList<Sticker>(MAXIMUM_STICKER_CAPACITY);
		mCurrentTextSignificantLength = 0;
	}

	/* Get the instance of this class from outside */
	public static StickerSearchHostManager getInstance() {

		if ((sStickerSearchHostManager == null) || sIsHostFinishingSearchTask) {
			synchronized (sHostInitLock) {
				if (sStickerSearchHostManager == null) {
					sIsHostFinishingSearchTask = false;
					sStickerSearchHostManager = new StickerSearchHostManager(HikeMessengerApp.getInstance());
				}
			}
		}

		return sStickerSearchHostManager;
	}

	/* Call this method just after choosing any contact to chat (while opening chat-thread) 
	   to load the history of that contact (either a person or a group) */
	public void loadChatProfile(String contactId, boolean isGroupChat) {
		Logger.v(TAG, "loadChatProfile(" + contactId + ", " + isGroupChat + ")");

		synchronized (sHostOperateLock) {
			if (mPreviousWords != null) {
				mPreviousWords.clear();
			} else {
				mPreviousWords = new ArrayList<String>();
			}

			mCurrentText = null;
			mCurrentTextSignificantLength = 0;
		}
	}

	public int [] beforeTextChange(CharSequence s, int start, int count, int after) {
		return null;
	}

	public Pair<CharSequence, int [] []> onTextChange(CharSequence s, int start, int before, int count) {
		Logger.d(TAG, "onTextChange(" + s + ", " + start + ", " + before + ", " + count + ")");

		int [] [] result = null;
		ArrayList<int []> tempResult = new ArrayList<int []>();
		// if (!HikeSharedPreferenceUtil.getInstance().getData("isPopulated", false)) return null;
		Object [] [] obj = StickerSearchUtility.splitAndDoIndexing(s.toString(), " ");
		ArrayList<Object []> cobj = new ArrayList<Object[]>();

		String value;
		for (int i = 0; i < obj.length; i++) {
			if (!TextUtils.isEmpty((String) obj [i] [0]))  {
				cobj.add(obj [i]);
			}
		}

		Logger.d(TAG, "" + cobj);

		for (int i = 0; i < cobj.size(); i++) {
			value = (String) cobj.get(i) [0];

			if (value.length() >= (i == 0 ? 1 : 2)) {
				if (!history.containsKey(value)) {
					ArrayList<String> results = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(value, (i == 0 && value.length() == 1));
					if (results != null && results.size() > 0) {
						LinkedHashSet<Sticker> stResules = new LinkedHashSet<Sticker>();
						for (String stData : results) {
							String [] ids = stData.split(":");
							Sticker st = new Sticker(ids [1], ids [0]);
							stResules.add(st);
						}
						ArrayList<Sticker> list = new ArrayList<Sticker>();
						list.addAll(stResules);
						history.put(value, list);
						tempResult.add(new int [] {(int) cobj.get(i) [1], (int) cobj.get(i) [1] + (int) cobj.get(i) [2]});
					}
				} else {
					tempResult.add(new int [] {(int) cobj.get(i) [1], (int) cobj.get(i) [1] + (int) cobj.get(i) [2]});
				}
			}
		}

		result = new int [tempResult.size()] [2];
		for (int i = 0; i < tempResult.size(); i++) {
			result [i] [0] = tempResult.get(i) [0];
			result [i] [1] = tempResult.get(i) [1];
		}

		Logger.d(TAG, "" + Arrays.toString(result));
		pResult = result;
		pwords = cobj;

		return new Pair<CharSequence, int [] []>(s, result);
	}

	public void onSend(CharSequence s) {
		pwords.clear();
		history.clear();
		pResult = null;
	}

	public ArrayList<Sticker> onClickToSendSticker(int where) {

		for (int i = 0; i < pwords.size(); i++) {
			if ((where >= (int) pwords.get(i) [1]) && (where <= (int) pwords.get(i) [1] + (int) pwords.get(i) [2])) {
				String value = (String) pwords.get(i) [0];
				Logger.d("ved", "" + history.get(value));
				return history.get(value);
			}
		}
		return null;
	}

	public void clear() {
		Logger.d(TAG, "clear()");

		sIsHostFinishingSearchTask = true;
		synchronized (sHostInitLock) {

			if (sWords != null) {
				for (Word word : sWords) {
					word.clear();
				}
				sWords.clear();
				sWords = null;
			}

			if (sStickers != null) {
				for (Sticker sticker : sStickers) {
					sticker.clear();
				}
				sStickers.clear();
				sStickers = null;
			}

			if (mPreviousWords != null) {
				mPreviousWords.clear();
				mPreviousWords = null;
			}

			mCurrentText = null;

			mCurrentIndividualChatProfile = null;
			mCurrentGroupChatProfile = null;

			if (sIndividualChatRecord != null) {
				Set<String> ids = sIndividualChatRecord.keySet();
				for (String id : ids) {
					sIndividualChatRecord.get(id).clear();
				}
				sIndividualChatRecord.clear();
				sIndividualChatRecord = null;
			}

			if (sGroupChatRecord != null) {
				Set<String> ids = sGroupChatRecord.keySet();
				for (String id : ids) {
					sGroupChatRecord.get(id).clear();
				}
				sGroupChatRecord.clear();
				sGroupChatRecord = null;
			}

			mContext = null;
			sStickerSearchHostManager = null;
		}
	}

	private void loadIndividualChatProfile(String contactId) {
		Logger.d(TAG, "loadIndividualChatProfile(" + contactId + ")");

		if (sIndividualChatRecord == null) {
			sIndividualChatRecord = new HashMap<String, StickerSearchHostManager.IndividualChatProfile>();
		}

		mCurrentIndividualChatProfile = sIndividualChatRecord.get(contactId);
		if (mCurrentIndividualChatProfile == null) {
			mCurrentIndividualChatProfile = new IndividualChatProfile(contactId);
		}
	}

	private void loadGroupChatProfile(String groupId) {
		Logger.d(TAG, "loadGroupChatProfile(" + groupId + ")");

		if (sGroupChatRecord == null) {
			sGroupChatRecord = new HashMap<String, StickerSearchHostManager.GroupChatProfile>();
		}

		mCurrentGroupChatProfile = sGroupChatRecord.get(groupId);
		if (mCurrentGroupChatProfile == null) {
			mCurrentGroupChatProfile = new GroupChatProfile(groupId);
		}
	}

	private void setPreviouslyCommunicatedText(String text) {
		Logger.v(TAG, "setPreviouslyCommunicatedText()");

		synchronized (sHostOperateLock) {
			if (sWords != null) {
				if (mPreviousWords != null) {
					mPreviousWords.clear();
				} else {
					mPreviousWords = new ArrayList<String>();
				}
				for (Word word : sWords) {
					if (mCurrentTextSignificantLength > 0) {
						mPreviousWords.add(word.getValue());
						mCurrentTextSignificantLength--;
					}
				}
			}
		}
	}

	private static class IndividualChatProfile {

		private IndividualChatProfile(String id) {
			populateChatStory();
		}

		private void populateChatStory() {
			
		}

		private void clear() {
			
		}
	}

	private static class GroupChatProfile {

		private GroupChatProfile(String id) {
			populateGroupStory();
		}

		private void populateGroupStory() {
			
		}

		private void clear() {
			
		}
	}

	private static class Word {

		private static final String TAG = Word.class.getSimpleName();

		private String mValue;
		private int mStartCharIndexInText;
		private int mIndexInText;
		private int mLength;
		private boolean mFlag;

		// Each of following lists has to in order w.r.t. remaining lists
		private ArrayList<String []> mStickerInfo;
		private ArrayList<Float> mFixedPriorities;
		private ArrayList<String []> mPositiveVariableData;
		private ArrayList<String []> mNegativeVariableData;

		private Word(String s, int startCharIndexInText, int indexInText, int length) {
			mValue = s;
			mStartCharIndexInText = startCharIndexInText;
			mIndexInText = indexInText;
			mLength = length;
		}

		private void fillStickerData(ArrayList<String> stickerRecognizers, ArrayList<Float> fixedPriority, ArrayList<String> positiveData, ArrayList<String> negativeData) {
			int count = ((stickerRecognizers == null) ? 0 : stickerRecognizers.size());
			Logger.d(TAG, "fillStickerData(No. of searched stickers = " + count);

			int i = 0;
			String [] s;
			String [] pd;
			String [] nd;
			for (String info : stickerRecognizers) {
				s = info.split(",");
				if (s.length == 2) {
					mStickerInfo.add(s);
					mFixedPriorities.add(fixedPriority.get(i));
					pd = positiveData.get(i).split(",");
					mPositiveVariableData.add(pd);
					nd = negativeData.get(i).split(",");
					mNegativeVariableData.add(nd);
				}
			}
		}

		private String getValue() {
			
			return ((mValue == null) ? HikeStickerSearchBaseConstants.EMPTY : mValue);
		}

		private ArrayList<String []> getStickers() {

			return mStickerInfo;
		}

		private void clear() {

			mValue = null;
			if (mStickerInfo != null) {
				mStickerInfo.clear();
				mStickerInfo = null;
			}

			if (mFixedPriorities != null) {
				mFixedPriorities.clear();
				mFixedPriorities = null;
			}

			if (mPositiveVariableData != null) {
				mPositiveVariableData.clear();
				mPositiveVariableData = null;
			}

			if (mNegativeVariableData != null) {
				mNegativeVariableData.clear();
				mNegativeVariableData = null;
			}
		}
	}

	// temp code to test
	private static ArrayList<Object []> pwords = new ArrayList<Object[]>();
	private static HashMap<String, ArrayList<Sticker>> history = new HashMap<String, ArrayList<Sticker>>();
	private static int [] [] pResult;
}
