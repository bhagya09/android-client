package com.bsb.hike.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeConstants.SMSSyncState;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.TrafficsStatsFile;
import com.bsb.hike.cropimage.CropImage;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ContactInfoData.DataType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.FtueContactsData;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.utils.JSONSerializable;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.service.ConnectionChangeReceiver;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.tasks.AuthSDKAsyncTask;
import com.bsb.hike.tasks.CheckForUpdateTask;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SyncOldSMSTask;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HikeDialog;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.ui.TimelineActivity;
import com.bsb.hike.ui.WebViewActivity;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.utils.AccountUtils.AccountInfo;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPUtils;
import com.google.android.maps.GeoPoint;

public class Utils
{
	public static Pattern shortCodeRegex;

	public static Pattern msisdnRegex;

	public static Pattern pinRegex;

	public static String shortCodeIntent;

	private static Animation mOutToRight;

	private static Animation mInFromLeft;

	private static TranslateAnimation mOutToLeft;

	private static TranslateAnimation mInFromRight;

	public static float scaledDensityMultiplier = 1.0f;
	
	public static float densityMultiplier = 1.0f;

	public static int densityDpi;

	private static Lock lockObj = new ReentrantLock();

	private static final String defaultCountryName = "India";

	static
	{
		shortCodeRegex = Pattern.compile("\\*\\d{3,10}#");
		msisdnRegex = Pattern.compile("\\[(\\+\\d*)\\]");
		pinRegex = Pattern.compile("\\d{4,6}");
	}

	public static String join(Collection<?> s, String delimiter, String startWith, String endWith)
	{
		StringBuilder builder = new StringBuilder();
		Iterator<?> iter = s.iterator();
		while (iter.hasNext())
		{
			if (!TextUtils.isEmpty(startWith))
			{
				builder.append(startWith);
			}
			builder.append(iter.next());
			if (!TextUtils.isEmpty(endWith))
			{
				builder.append(endWith);
			}
			if (!iter.hasNext())
			{
				break;
			}
			builder.append(delimiter);
		}
		Logger.d("Utils", "Joined string is: " + builder.toString());
		return builder.toString();
	}

	/*
	 * serializes the given collection into an object. Ignores exceptions
	 */
	public static JSONArray jsonSerialize(Collection<? extends JSONSerializable> elements)
	{
		JSONArray arr = new JSONArray();
		for (JSONSerializable elem : elements)
		{
			try
			{
				arr.put(elem.toJSON());
			}
			catch (JSONException e)
			{
				Logger.e("Utils", "error json serializing", e);
			}
		}
		return arr;
	}

	public static JSONObject jsonSerialize(Map<String, ? extends JSONSerializable> elements) throws JSONException
	{
		JSONObject obj = new JSONObject();
		for (Map.Entry<String, ? extends JSONSerializable> element : elements.entrySet())
		{
			obj.put(element.getKey(), element.getValue().toJSON());
		}
		return obj;
	}

	static final private int ANIMATION_DURATION = 400;

	public static Animation inFromRightAnimation(Context ctx)
	{
		if (mInFromRight == null)
		{
			synchronized (Utils.class)
			{
				mInFromRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
						Animation.RELATIVE_TO_PARENT, 0.0f);
				mInFromRight.setDuration(ANIMATION_DURATION);
				mInFromRight.setInterpolator(new AccelerateInterpolator());
			}
		}
		return mInFromRight;
	}

	public static Animation outToLeftAnimation(Context ctx)
	{
		if (mOutToLeft == null)
		{
			synchronized (Utils.class)
			{
				mOutToLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
						Animation.RELATIVE_TO_PARENT, 0.0f);
				mOutToLeft.setDuration(ANIMATION_DURATION);
				mOutToLeft.setInterpolator(new AccelerateInterpolator());
			}
		}

		return mOutToLeft;
	}

	public static Animation outToRightAnimation(Context ctx)
	{
		if (mOutToRight == null)
		{
			synchronized (Utils.class)
			{
				if (mOutToRight == null)
				{
					mOutToRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
							Animation.RELATIVE_TO_PARENT, 0.0f);
					mOutToRight.setDuration(ANIMATION_DURATION);
					mOutToRight.setInterpolator(new AccelerateInterpolator());
				}
			}
		}
		return mOutToRight;
	}

	public static Animation inFromLeftAnimation(Context ctx)
	{
		if (mInFromLeft == null)
		{
			synchronized (Utils.class)
			{
				if (mInFromLeft == null)
				{
					mInFromLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
							Animation.RELATIVE_TO_PARENT, 0.0f);
					mInFromLeft.setDuration(ANIMATION_DURATION);
					mInFromLeft.setInterpolator(new AccelerateInterpolator());
				}
			}
		}
		return mInFromLeft;
	}

	public static Intent createIntentFromContactInfo(final ContactInfo contactInfo, boolean openKeyBoard)
	{
		Intent intent = new Intent();

		// If the contact info was made using a group conversation, then the
		// Group ID is in the contact ID
		intent.putExtra(HikeConstants.Extras.MSISDN, Utils.isGroupConversation(contactInfo.getMsisdn()) ? contactInfo.getId() : contactInfo.getMsisdn());
		intent.putExtra(HikeConstants.Extras.SHOW_KEYBOARD, openKeyBoard);
		return intent;
	}

	public static Intent createIntentFromMsisdn(String msisdnOrGroupId, boolean openKeyBoard)
	{
		Intent intent = new Intent();

		// If the contact info was made using a group conversation, then the
		// Group ID is in the contact ID
		intent.putExtra(HikeConstants.Extras.MSISDN, msisdnOrGroupId);
		intent.putExtra(HikeConstants.Extras.SHOW_KEYBOARD, openKeyBoard);
		return intent;
	}

	/** Create a File for saving an image or video */
	public static File getOutputMediaFile(HikeFileType type, String orgFileName, boolean isSent)
	{
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		String path = getFileParent(type, isSent);
		if (path == null)
		{
			return null;
		}

		File mediaStorageDir = new File(path);
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists())
		{
			if (!mediaStorageDir.mkdirs())
			{
				Logger.d("Hike", "failed to create directory");
				return null;
			}
		}

		// File name should only be blank in case of profile images or while
		// capturing new media.
		if (TextUtils.isEmpty(orgFileName))
		{
			orgFileName = getOriginalFile(type, orgFileName);
		}

		// String fileName = getUniqueFileName(orgFileName, fileKey);

		return new File(mediaStorageDir, orgFileName);
	}

	public static String getOriginalFile(HikeFileType type, String orgFileName)
	{
		// Create a media file name
		// String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS")
		// .format(new Date());
		String timeStamp = Long.toString(System.currentTimeMillis());
		// File name should only be blank in case of profile images or while
		// capturing new media.
		if (TextUtils.isEmpty(orgFileName))
		{
			switch (type)
			{
			case PROFILE:
			case IMAGE:
				orgFileName = "IMG_" + timeStamp + ".jpg";
				break;
			case VIDEO:
				orgFileName = "MOV_" + timeStamp + ".mp4";
				break;
			case AUDIO:
			case AUDIO_RECORDING:
				orgFileName = "AUD_" + timeStamp + ".m4a";
			}
		}
		return orgFileName;
	}

	public static String getFinalFileName(HikeFileType type)
	{
		return getFinalFileName(type, null);
	}

	public static String getFinalFileName(HikeFileType type, String orgName)
	{
		StringBuilder orgFileName = new StringBuilder();
		// String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS")
		// .format(new Date());
		String timeStamp = Long.toString(System.currentTimeMillis());
		if (TextUtils.isEmpty(orgName))
		{
			switch (type)
			{
			case PROFILE:
			case IMAGE:
				orgFileName.append("IMG_" + timeStamp + ".jpg");
				break;
			case VIDEO:
				orgFileName.append("MOV_" + timeStamp + ".mp4");
				break;
			case AUDIO:
			case AUDIO_RECORDING:
				orgFileName.append("AUD_" + timeStamp + ".m4a");
				break;
			case OTHER:
				orgFileName.append("FILE_" + timeStamp);
			}
		}
		else
		{
			int lastDotIndex = orgName.lastIndexOf(".");

			String actualName;
			String extension = getFileExtension(orgName);

			if (lastDotIndex != -1 && lastDotIndex != orgName.length() - 1)
			{
				actualName = new String(orgName.substring(0, lastDotIndex));
			}
			else
			{
				actualName = orgName;
			}

			orgFileName.append(actualName + "_" + timeStamp);

			if (!TextUtils.isEmpty(extension))
			{
				orgFileName.append("." + extension);
			}
		}
		return orgFileName.toString();
	}

	public static String getFileExtension(String fileName)
	{
		int lastDotIndex = fileName.lastIndexOf(".");

		String extension = "";

		if (lastDotIndex != -1 && lastDotIndex != fileName.length() - 1)
		{
			extension = new String(fileName.substring(lastDotIndex + 1));
		}

		return extension;
	}

	public static String getFileParent(HikeFileType type, boolean isSent)
	{
		StringBuilder path = new StringBuilder(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT);
		switch (type)
		{
		case PROFILE:
			path.append(HikeConstants.PROFILE_ROOT);
			break;
		case IMAGE:
			path.append(HikeConstants.IMAGE_ROOT);
			break;
		case VIDEO:
			path.append(HikeConstants.VIDEO_ROOT);
			break;
		case AUDIO:
			path.append(HikeConstants.AUDIO_ROOT);
			break;
		case AUDIO_RECORDING:
			path.append(HikeConstants.AUDIO_RECORDING_ROOT);
			break;
		default:
			path.append(HikeConstants.OTHER_ROOT);
			break;
		}
		if (isSent)
		{
			path.append(HikeConstants.SENT_ROOT);
		}
		return path.toString();
	}

	public static void savedAccountCredentials(AccountInfo accountInfo, SharedPreferences.Editor editor)
	{
		AccountUtils.setToken(accountInfo.token);
		AccountUtils.setUID(accountInfo.uid);
		editor.putString(HikeMessengerApp.MSISDN_SETTING, accountInfo.msisdn);
		editor.putString(HikeMessengerApp.TOKEN_SETTING, accountInfo.token);
		editor.putString(HikeMessengerApp.UID_SETTING, accountInfo.uid);
		editor.putString(HikeMessengerApp.BACKUP_TOKEN_SETTING, accountInfo.backupToken);
		editor.putInt(HikeMessengerApp.SMS_SETTING, accountInfo.smsCredits);
		editor.putInt(HikeMessengerApp.INVITED, accountInfo.all_invitee);
		editor.putInt(HikeMessengerApp.INVITED_JOINED, accountInfo.all_invitee_joined);
		editor.putString(HikeMessengerApp.COUNTRY_CODE, accountInfo.country_code);
		editor.commit();
	}

	/*
	 * Extract a pin code from a specially formatted message to the application.
	 * 
	 * @return null iff the message isn't an SMS pincode, otherwise return the pincode
	 */
	public static String getSMSPinCode(String body)
	{
		Matcher m = pinRegex.matcher(body);
		return m.find() ? m.group() : null;
	}

	public static boolean requireAuth(Activity activity)
	{
		SharedPreferences settings = activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		if (!settings.getBoolean(HikeMessengerApp.ACCEPT_TERMS, false))
		{
			activity.startActivity(new Intent(activity, WelcomeActivity.class));
			activity.finish();
			return true;
		}

		if (settings.getString(HikeMessengerApp.NAME_SETTING, null) == null)
		{
			activity.startActivity(new Intent(activity, SignupActivity.class));
			activity.finish();
			return true;
		}

		if (!settings.getBoolean(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, false) || !settings.getBoolean(HikeMessengerApp.SIGNUP_COMPLETE, false))
		{
			if (isUserUpgrading(activity))
			{
				Editor editor = settings.edit();
				editor.putBoolean(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, true);
				editor.putBoolean(HikeMessengerApp.SIGNUP_COMPLETE, true);
				editor.commit();
				return false;
			}
			else
			{
				activity.startActivity(new Intent(activity, SignupActivity.class));
				activity.finish();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if the user has successfully signedup. This means user is has passed signuptask.
	 * Returns false otherwise. In this case it will open either SignupActivity or WelcomeActivity.
	 * 
	 * @param context
	 * @param launchSignup  -- true if you want to launch respective activity, false otherwise
	 * @return
	 */
	public static boolean isUserSignedUp(Context context, boolean launchSignup)
	{
		HikeSharedPreferenceUtil settingPref = HikeSharedPreferenceUtil.getInstance();
		if (!settingPref.getData(HikeMessengerApp.ACCEPT_TERMS, false))
		{
			if(launchSignup)
			{
				Intent i = new Intent(context, WelcomeActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);
			}
			return false;
		}

		if (settingPref.getData(HikeMessengerApp.NAME_SETTING, null) == null)
		{
			if(launchSignup)
			{
				Intent i = new Intent(context, SignupActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);
			}
			return false;
		}

		if (!settingPref.getData(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, false) || !settingPref.getData(HikeMessengerApp.SIGNUP_COMPLETE, false))
		{
			if (isUserUpgrading(context))
			{
				settingPref.saveData(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, true);
				settingPref.saveData(HikeMessengerApp.SIGNUP_COMPLETE, true);
				return true;
			}
			else
			{
				if(launchSignup)
				{
					Intent i = new Intent(context, SignupActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(i);
				}
				return false;
			}
		}
		return true;
	}
	
	private static boolean isUserUpgrading(Context context)
	{
		SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String currentAppVersion = settings.getString(HikeMessengerApp.CURRENT_APP_VERSION, "");
		String actualAppVersion = "";
		try
		{
			actualAppVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e)
		{
			Logger.e("Utils", "Unable to get the app version");
		}
		if (!currentAppVersion.equals("") && !currentAppVersion.equals(actualAppVersion))
		{
			return true;
		}
		return false;
	}

	public static String formatNo(String msisdn)
	{
		StringBuilder sb = new StringBuilder(msisdn);
		sb.insert(msisdn.length() - 4, '-');
		sb.insert(msisdn.length() - 7, '-');
		Logger.d("Fomat MSISD", "Fomatted number is:" + sb.toString());

		return sb.toString();
	}

	public static boolean isValidEmail(Editable text)
	{
		return (!TextUtils.isEmpty(text) && android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches());
	}

	public static void logEvent(Context context, String event)
	{
		logEvent(context, event, 1);
	}

	/**
	 * Used for logging the UI based events from the clients side.
	 * 
	 * @param context
	 * @param event
	 *            : The event which is to be logged.
	 * @param time
	 *            : This is only used to signify the time the user was on a screen for. For cases where this is not relevant we send 0.s
	 */
	public static void logEvent(Context context, String event, long increment)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ANALYTICS, 0);

		long currentVal = prefs.getLong(event, 0) + increment;

		Editor editor = prefs.edit();
		editor.putLong(event, currentVal);
		editor.commit();
	}

	public static List<String> splitSelectedContacts(String selections)
	{
		Matcher matcher = msisdnRegex.matcher(selections);
		List<String> contacts = new ArrayList<String>();
		if (matcher.find())
		{
			do
			{
				contacts.add(matcher.group().substring(1, matcher.group().length() - 1));
				Logger.d("Utils", "Adding: " + matcher.group().substring(1, matcher.group().length() - 1));
			}
			while (matcher.find(matcher.end()));
		}
		return contacts;
	}

	public static List<String> splitSelectedContactsName(String selections)
	{
		String[] selectedContacts = selections.split(", ");
		List<String> contactNames = new ArrayList<String>(selectedContacts.length);
		for (int i = 0; i < selectedContacts.length; i++)
		{
			if (!selectedContacts[i].contains("["))
			{
				continue;
			}
			contactNames.add(selectedContacts[i].substring(0, selectedContacts[i].indexOf("[")));
		}
		return contactNames;
	}

	public static boolean isGroupConversation(String msisdn)
	{
		return msisdn!=null && !msisdn.startsWith("+");
	}

	public static String validateBotMsisdn(String msisdn){
		if (!msisdn.startsWith("+")){
			msisdn = "+" + msisdn;
		}
		return msisdn;
	}


	public static String defaultGroupName(List<PairModified<GroupParticipant, String>> participantList)
	{
		List<GroupParticipant> groupParticipants = new ArrayList<GroupParticipant>();
		for (PairModified<GroupParticipant, String> participant : participantList)
		{
			if (!participant.getFirst().hasLeft())
			{
				groupParticipants.add(participant.getFirst());
			}
		}
		Collections.sort(groupParticipants);

		switch (groupParticipants.size())
		{
		case 0:
			return "";
		case 1:
			return groupParticipants.get(0).getContactInfo().getFirstName();
		case 2:
			return groupParticipants.get(0).getContactInfo().getFirstName() + " and " + groupParticipants.get(1).getContactInfo().getFirstName();
		default:
			return groupParticipants.get(0).getContactInfo().getFirstName() + " and " + (groupParticipants.size() - 1) + " others";
		}
	}

	public static String getGroupJoinHighlightText(JSONArray participantInfoArray, GroupConversation conversation)
	{
		JSONObject participant = (JSONObject) participantInfoArray.opt(0);
		String highlight = ((GroupConversation) conversation).getGroupParticipantFirstName(participant.optString(HikeConstants.MSISDN));

		if (participantInfoArray.length() == 2)
		{
			JSONObject participant2 = (JSONObject) participantInfoArray.opt(1);
			String name2 = ((GroupConversation) conversation).getGroupParticipantFirstName(participant2.optString(HikeConstants.MSISDN));

			highlight += " and " + name2;
		}
		else if (participantInfoArray.length() > 2)
		{
			highlight += " and " + (participantInfoArray.length() - 1) + " others";
		}
		return highlight;
	}

	public static void recordDeviceDetails(Context context)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			
			int height;
			int width;

			/*
			 * Doing this to avoid the ClassCastException when the context is sent from the BroadcastReceiver. As it is, we don't need to send the resolution from the
			 * BroadcastReceiver since it should have already been sent to the server.
			 */
			if (context instanceof Activity)
			{
				height = ((Activity) context).getWindowManager().getDefaultDisplay().getHeight();
				width = ((Activity) context).getWindowManager().getDefaultDisplay().getWidth();
				String resolution = height + "x" + width;
				metadata.put(HikeConstants.LogEvent.RESOLUTION, resolution);
			}
			TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

			String osVersion = Build.VERSION.RELEASE;
			String deviceId = null;
			try
			{
				deviceId = getHashedDeviceId(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
			}
			catch (NoSuchAlgorithmException e)
			{
				e.printStackTrace();
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			String os = "Android";
			String carrier = manager.getNetworkOperatorName();
			String device = Build.MANUFACTURER + " " + Build.MODEL;
			String appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

			Map<String, String> referralValues = retrieveReferralParams(context);
			if (!referralValues.isEmpty())
			{
				for (Entry<String, String> entry : referralValues.entrySet())
				{
					metadata.put(entry.getKey(), entry.getValue());
				}
			}
			metadata.put(HikeConstants.LogEvent.DEVICE_ID, deviceId);
			metadata.put(HikeConstants.LogEvent.OS, os);
			metadata.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
			metadata.put(HikeConstants.LogEvent.DEVICE, device);
			metadata.put(HikeConstants.LogEvent.CARRIER, carrier);
			metadata.put(HikeConstants.LogEvent.APP_VERSION, appVersion);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DEVICE_DETAILS, metadata, AnalyticsConstants.EVENT_TAG_CBS);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
		catch (NameNotFoundException e)
		{
			Logger.e("Utils", "Package not found", e);
		}
	}

	public static void getDeviceStats(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ANALYTICS, 0);
		Editor editor = prefs.edit();
		Map<String, ?> keys = prefs.getAll();

		JSONObject metadata = new JSONObject();

		try
		{
			if (!keys.isEmpty())
			{
				for (String key : keys.keySet())
				{
					Logger.d("Utils", "Getting keys: " + key);
					metadata.put(key, prefs.getLong(key, 0));
					editor.remove(key);
				}
				editor.commit();
				HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DEVICE_STATS, metadata);
			}
		}
		catch (JSONException e)
		{
			Logger.e("Utils", "Invalid JSON", e);
		}
	}

	public static CharSequence addContactName(String firstName, CharSequence message)
	{
		SpannableStringBuilder messageWithName = new SpannableStringBuilder(firstName + HikeConstants.SEPARATOR + message);
		messageWithName.setSpan(new StyleSpan(Typeface.BOLD), 0, firstName.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return messageWithName;
	}

	/**
	 * Used for setting the density multiplier, which is to be multiplied with any pixel value that is programmatically given
	 * 
	 * @param activity
	 */
	public static void setDensityMultiplier(DisplayMetrics displayMetrics)
	{
		Utils.scaledDensityMultiplier = displayMetrics.scaledDensity;
		Utils.densityDpi = displayMetrics.densityDpi;
		Utils.densityMultiplier = displayMetrics.density;
	}

	public static CharSequence getFormattedParticipantInfo(String info, String textToHighight)
	{
		SpannableStringBuilder ssb = new SpannableStringBuilder(info);
		ssb.setSpan(new StyleSpan(Typeface.BOLD), info.indexOf(textToHighight), info.indexOf(textToHighight) + textToHighight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return ssb;
	}

	/**
	 * Used for preventing the cursor from being shown initially on the text box in touch screen devices. On touching the text box the cursor becomes visible
	 * 
	 * @param editText
	 */
	public static void hideCursor(final EditText editText, Resources resources)
	{
		if (resources.getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS || resources.getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES)
		{
			editText.setCursorVisible(false);
			editText.setOnTouchListener(new OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					if (event.getAction() == MotionEvent.ACTION_DOWN)
					{
						editText.setCursorVisible(true);
					}
					return false;
				}
			});
		}
	}

	public static ContactInfo getUserContactInfo(SharedPreferences prefs)
	{
		return getUserContactInfo(prefs, false);
	}

	public static ContactInfo getUserContactInfo(SharedPreferences prefs, boolean showNameAsYou)
	{
		String myMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
		long userJoinTime = prefs.getLong(HikeMessengerApp.USER_JOIN_TIME, 0);

		String myName;
		if (showNameAsYou)
		{
			myName = "You";
		}
		else
		{
			myName = prefs.getString(HikeMessengerApp.NAME_SETTING, null);
		}

		ContactInfo contactInfo = new ContactInfo(myName, myMsisdn, myName, myMsisdn, true);
		contactInfo.setHikeJoinTime(userJoinTime);

		return contactInfo;
	}

	public static boolean wasScreenOpenedNNumberOfTimes(SharedPreferences prefs, String whichScreen)
	{
		return prefs.getInt(whichScreen, 0) >= HikeConstants.NUM_TIMES_SCREEN_SHOULD_OPEN_BEFORE_TOOL_TIP;
	}

	public static void incrementNumTimesScreenOpen(SharedPreferences prefs, String whichScreen)
	{
		Editor editor = prefs.edit();
		editor.putInt(whichScreen, prefs.getInt(whichScreen, 0) + 1);
		editor.commit();
	}

	public static boolean isUpdateRequired(String version, Context context)
	{
		try
		{
			String appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

			StringTokenizer updateVersion = new StringTokenizer(version, ".");
			StringTokenizer currentVersion = new StringTokenizer(appVersion, ".");
			while (currentVersion.hasMoreTokens())
			{
				if (!updateVersion.hasMoreTokens())
				{
					return false;
				}
				int currentVersionToken = Integer.parseInt(currentVersion.nextToken());
				int updateVersionToken = Integer.parseInt(updateVersion.nextToken());
				if (updateVersionToken > currentVersionToken)
				{
					return true;
				}
				else if (updateVersionToken < currentVersionToken)
				{
					return false;
				}
			}
			while (updateVersion.hasMoreTokens())
			{
				if (Integer.parseInt(updateVersion.nextToken()) > 0)
				{
					return true;
				}
			}
			return false;
		}
		catch (NameNotFoundException e)
		{
			Logger.e("Utils", "Package not found...", e);
			return false;
		}
	}

	/*
	 * Stores the referral parameters in the app's sharedPreferences.
	 */
	public static void storeReferralParams(Context context, List<NameValuePair> params)
	{
		SharedPreferences storage = context.getSharedPreferences(HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = storage.edit();

		for (NameValuePair nameValuePair : params)
		{
			String name = nameValuePair.getName();
			String value = nameValuePair.getValue();
			editor.putString(name, value);
		}

		editor.commit();
	}

	/*
	 * Returns a map with the Market Referral parameters pulled from the sharedPreferences.
	 */
	public static Map<String, String> retrieveReferralParams(Context context)
	{
		Map<String, String> params = new HashMap<String, String>();
		SharedPreferences storage = context.getSharedPreferences(HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE);

		for (String key : storage.getAll().keySet())
		{
			String value = storage.getString(key, null);
			if (value != null)
			{
				params.put(key, value);
			}
		}
		// We don't need these values anymore
		Editor editor = storage.edit();
		editor.clear();
		editor.commit();
		return params;
	}

	public static boolean isUserOnline(Context context)
	{
		try
		{
			if (context == null)
			{
				Logger.e("HikeService", "Hike service is null!!");
				return false;
			}
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			return (cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected());
		}
		catch (NullPointerException e)
		{
			/*
			 * We were seeing NPEs on the console in this method. Added this since could not find any reason why we would get an NPE here.
			 */
			return false;
		}
	}

	/**
	 * Requests the server to send an account info packet
	 */
	public static void requestAccountInfo(boolean upgrade, boolean sendbot)
	{
		Logger.d("Utils", "Requesting account info");
		JSONObject requestAccountInfo = new JSONObject();
		try
		{
			requestAccountInfo.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.REQUEST_ACCOUNT_INFO);

			JSONObject data = new JSONObject();
			data.put(HikeConstants.UPGRADE, upgrade);
			data.put(HikeConstants.SENDBOT, sendbot);
			data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis() / 1000));
			data.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
			requestAccountInfo.put(HikeConstants.DATA, data);
			HikeMqttManagerNew.getInstance().sendMessage(requestAccountInfo, HikeMqttManagerNew.MQTT_QOS_ONE);
		}
		catch (JSONException e)
		{
			Logger.e("Utils", "Invalid JSON", e);
		}
	}

	public static String ellipsizeName(String name)
	{
		return name.length() <= HikeConstants.MAX_CHAR_IN_NAME ? name : (name.substring(0, HikeConstants.MAX_CHAR_IN_NAME - 3) + "...");
	}

	public static String getInviteMessage(Context context, int messageResId)
	{
		String inviteToken = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeConstants.INVITE_TOKEN, "");
		inviteToken = "";
		// Adding the user's invite token to the invite url
		String inviteMessage = context.getString(messageResId, inviteToken);

		return inviteMessage;
	}

	public static void startShareIntent(Context context, String message)
	{
		Intent s = new Intent(android.content.Intent.ACTION_SEND);
		s.setType("text/plain");
		s.putExtra(Intent.EXTRA_TEXT, message);
		context.startActivity(s);
	}

	public static void startShareImageIntent(String mimeType, String imagePath,String text)
	{
		Intent s = new Intent(android.content.Intent.ACTION_SEND);
		s.setType(mimeType);
		s.putExtra(Intent.EXTRA_STREAM, Uri.parse(imagePath));
		if(!TextUtils.isEmpty(text))
		{
			s.putExtra(Intent.EXTRA_TEXT, text);
		}
		s.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		Logger.i("imageShare", "shared image with "+s.getExtras());
		HikeMessengerApp.getInstance().getApplicationContext().startActivity(s);
		
	}
	public static void startShareImageIntent(String mimeType, String imagePath)
	{
		startShareImageIntent(mimeType, imagePath, null);
	}

	public static void bytesToFile(byte[] bytes, File dst)
	{
		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(dst);
			out.write(bytes, 0, bytes.length);
		}
		catch (IOException e)
		{
			Logger.e("Utils", "Excecption while copying the file", e);
		}
		finally
		{
			if (out != null)
			{
				try
				{
					out.flush();
					out.getFD().sync();
					out.close();
				}
				catch (IOException e)
				{
					Logger.e("Utils", "Excecption while closing the stream", e);
				}
			}
		}
	}

	public static byte[] fileToBytes(File file)
	{
		byte[] bytes = new byte[(int) file.length()];
		FileInputStream fileInputStream = null;
		try
		{
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bytes);
			return bytes;
		}
		catch (IOException e)
		{
			Logger.e("Utils", "Excecption while reading the file " + file.getName(), e);
			return null;
		}
		finally
		{
			if (fileInputStream != null)
			{
				try
				{
					fileInputStream.close();
				}
				catch (IOException e)
				{
					Logger.e("Utils", "Excecption while closing the file " + file.getName(), e);
				}
			}
		}
	}

	public static Drawable stringToDrawable(String encodedString)
	{
		if (TextUtils.isEmpty(encodedString))
		{
			return null;
		}
		byte[] thumbnailBytes = Base64.decode(encodedString, Base64.DEFAULT);
		return new BitmapDrawable(BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length));
	}

	public static String drawableToString(Drawable ic)
	{
		if (ic != null)
		{
			Bitmap bitmap = ((BitmapDrawable) ic).getBitmap();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			bitmap.compress(CompressFormat.PNG, 100, outputStream);
			byte[] bitmapByte = outputStream.toByteArray();
			return Base64.encodeToString(bitmapByte,Base64.DEFAULT);
		}
		return null;
	}

	public static Bitmap getRotatedBitmap(String path, Bitmap bitmap)
	{
		if (bitmap == null)
		{
			return null;
		}

		Bitmap rotatedBitmap = null;
		Matrix m = new Matrix();
		ExifInterface exif = null;
		int orientation = 1;

		try
		{
			if (path != null)
			{
				// Getting Exif information of the file
				exif = new ExifInterface(path);
			}
			if (exif != null)
			{
				orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
				switch (orientation)
				{
				case ExifInterface.ORIENTATION_ROTATE_270:
					m.preRotate(270);
					break;

				case ExifInterface.ORIENTATION_ROTATE_90:
					m.preRotate(90);
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					m.preRotate(180);
					break;
				}
				// Rotates the image according to the orientation
				rotatedBitmap = HikeBitmapFactory.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return rotatedBitmap;
	}

	public static Bitmap makeSquareThumbnail(Bitmap thumbnail, int dimensionLimit)
	{
		dimensionLimit = thumbnail.getWidth() < thumbnail.getHeight() ? thumbnail.getWidth() : thumbnail.getHeight();

		int startX = thumbnail.getWidth() > dimensionLimit ? (int) ((thumbnail.getWidth() - dimensionLimit) / 2) : 0;
		int startY = thumbnail.getHeight() > dimensionLimit ? (int) ((thumbnail.getHeight() - dimensionLimit) / 2) : 0;

		Logger.d("Utils", "StartX: " + startX + " StartY: " + startY + " WIDTH: " + thumbnail.getWidth() + " Height: " + thumbnail.getHeight());
		Bitmap squareThumbnail = Bitmap.createBitmap(thumbnail, startX, startY, dimensionLimit, dimensionLimit);

		if (squareThumbnail != thumbnail)
		{
			thumbnail.recycle();
		}
		thumbnail = null;
		return squareThumbnail;
	}

	public static Bitmap stringToBitmap(String thumbnailString)
	{
		byte[] encodeByte = Base64.decode(thumbnailString, Base64.DEFAULT);
		return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
	}

	public static boolean isThumbnailSquare(Bitmap thumbnail)
	{
		return (thumbnail.getWidth() == thumbnail.getHeight());
	}

	public static byte[] bitmapToBytes(Bitmap bitmap, Bitmap.CompressFormat format)
	{
		return bitmapToBytes(bitmap, format, 50);
	}

	public static byte[] bitmapToBytes(Bitmap bitmap, Bitmap.CompressFormat format, int quality)
	{
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		bitmap.compress(format, quality, bao);
		return bao.toByteArray();
	}

	// If source is local file path then previous getRealPathFromUri implementation (which uses deprecated manage query) provides null, So adding this implementation to solve the issue.
	public static String getRealPathFromUri(Uri uri, Context mContext)
	{
		String result = null;
		Cursor cursor = null;
		try
		{
		    cursor = mContext.getContentResolver().query(uri, null, null, null, null);
		    if (cursor == null) {
		        result = uri.getPath();
		    } else {
		        if(cursor.moveToFirst())
		        {
		        	int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
			        result = cursor.getString(idx);
		        }
		        else
		        {
		        	result = null;
		        }
		    }
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(cursor != null)
				cursor.close();
		}
	    return result;
	}


	public static enum ExternalStorageState
	{
		WRITEABLE, READ_ONLY, NONE
	}

	public static ExternalStorageState getExternalStorageState()
	{
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state))
		{
			// We can read and write the media
			return ExternalStorageState.WRITEABLE;
		}
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
		{
			// We can only read the media
			return ExternalStorageState.READ_ONLY;
		}
		else
		{
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			return ExternalStorageState.NONE;
		}
	}

	public static String getFirstName(String name)
	{
		return name.trim().split(" ", 2)[0];
	}

	public static String getFirstNameAndSurname(String name)
	{
		/*
		 * String fullname = name.trim().split(" ", 2)[0]; if(name.contains(" ")) { int spaceIndex = name.indexOf(" "); fullname.concat(" " + name.charAt(spaceIndex + 1)); }
		 */
		return name;
	}

	public static double getFreeSpace()
	{
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		double sdAvailSize = 0.0;
		if(isJELLY_BEAN_MR2OrHigher())
		{
			 sdAvailSize = (double) stat.getAvailableBlocksLong() * (double) stat.getBlockSizeLong();
		}
		else
		{
			sdAvailSize = (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
		}
		Logger.d("StickerSize", "get available blocks : " + (double) stat.getAvailableBlocks() + "  get block size : " + (double) stat.getBlockSize());
		
		return sdAvailSize;
	}

	public static boolean copyFile(String srcFilePath, String destFilePath, HikeFileType hikeFileType)
	{
		/*
		 * If source and destination have the same path, just return.
		 */
		if (srcFilePath.equals(destFilePath))
		{
			return true;
		}
		try
		{
			InputStream src;
			if (hikeFileType == HikeFileType.IMAGE)
			{
				String imageOrientation = Utils.getImageOrientation(srcFilePath);
				Bitmap tempBmp = HikeBitmapFactory.scaleDownBitmap(srcFilePath, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX,
						Bitmap.Config.RGB_565, true, false);
				tempBmp = HikeBitmapFactory.rotateBitmap(tempBmp, Utils.getRotatedAngle(imageOrientation));
				// Temporary fix for when a user uploads a file through Picasa
				// on ICS or higher.
				if (tempBmp != null)
				{
					byte[] fileBytes = BitmapUtils.bitmapToBytes(tempBmp, Bitmap.CompressFormat.JPEG, 75);
					tempBmp.recycle();
					src = new ByteArrayInputStream(fileBytes);
				}
				else
				{
					src = new FileInputStream(new File(srcFilePath));
				}
			}
			else
			{
				src = new FileInputStream(new File(srcFilePath));
			}
			FileOutputStream dest = new FileOutputStream(new File(destFilePath));

			byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
			int len;

			while ((len = src.read(buffer)) > 0)
			{
				dest.write(buffer, 0, len);
			}

			dest.flush();
			dest.getFD().sync();
			src.close();
			dest.close();

			return true;
		}
		catch (FileNotFoundException e)
		{
			Logger.e("Utils", "File not found while copying", e);
			return false;
		}
		catch (IOException e)
		{
			Logger.e("Utils", "Error while reading/writing/closing file", e);
			return false;
		}
		catch (Exception ex)
		{
			Logger.e("Utils", "WTF Error while reading/writing/closing file", ex);
			return false;
		}
	}

	public static boolean compressAndCopyImage(String srcFilePath, String destFilePath, Context context)
	{
		SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		int quality = appPrefs.getInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_DEFAULT);
		return compressAndCopyImage(srcFilePath, destFilePath, context, quality);
	}
	
	public static boolean compressAndCopyImage(String srcFilePath, String destFilePath, Context context, int quality)
	{
		try
		{
			InputStream src;
			String imageOrientation = Utils.getImageOrientation(srcFilePath);
			Bitmap tempBmp = null;

			if (quality == ImageQuality.QUALITY_MEDIUM)
			{
				tempBmp = HikeBitmapFactory.scaleDownBitmap(srcFilePath, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX,
						Bitmap.Config.RGB_565, true, false);
			}
			else if (quality != ImageQuality.QUALITY_ORIGINAL)
			{
				tempBmp = HikeBitmapFactory.scaleDownBitmap(srcFilePath, HikeConstants.MAX_DIMENSION_LOW_FULL_SIZE_PX, HikeConstants.MAX_DIMENSION_LOW_FULL_SIZE_PX,
						Bitmap.Config.RGB_565, false, false); // Reducing further for small
			}
			tempBmp = HikeBitmapFactory.rotateBitmap(tempBmp, Utils.getRotatedAngle(imageOrientation));
			if (tempBmp != null)
			{
				byte[] fileBytes = BitmapUtils.bitmapToBytes(tempBmp, Bitmap.CompressFormat.JPEG, 75);
				tempBmp.recycle();
				src = new ByteArrayInputStream(fileBytes);
			}
			else
			{
				src = new FileInputStream(new File(srcFilePath));
			}

			FileOutputStream dest = new FileOutputStream(new File(destFilePath));

			byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
			int len;

			while ((len = src.read(buffer)) > 0)
			{
				dest.write(buffer, 0, len);
			}
			dest.flush();
			dest.getFD().sync();
			src.close();
			dest.close();
			return true;
		}
		catch (FileNotFoundException e)
		{
			Logger.e("Utils", "File not found while copying", e);
			return false;
		}
		catch (IOException e)
		{
			Logger.e("Utils", "Error while reading/writing/closing file", e);
			return false;
		}
		catch (Exception ex)
		{
			Logger.e("Utils", "WTF Error while reading/writing/closing file", ex);
			return false;
		}
	}

	public static void resetImageQuality(SharedPreferences appPrefs)
	{
		// TODO Auto-generated method stub
		final Editor editor = appPrefs.edit();
		editor.putInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_DEFAULT);
		editor.commit();
	}

	public static String getImageOrientation(String filePath)
	{
		ExifInterface exif;
		try
		{
			exif = new ExifInterface(filePath);
			return exif.getAttribute(ExifInterface.TAG_ORIENTATION);
		}
		catch (IOException e)
		{
			Logger.e("Utils", "Error while opening file", e);
			return null;
		}
	}

	public static int getRotatedAngle(String imageOrientation)
	{
		if (!TextUtils.isEmpty(imageOrientation))
		{
			switch (Integer.parseInt(imageOrientation))
			{
			case ExifInterface.ORIENTATION_ROTATE_180:
				return 180;
			case ExifInterface.ORIENTATION_ROTATE_270:
				return 270;
			case ExifInterface.ORIENTATION_ROTATE_90:
				return 90;
			}
		}
		return 0;
	}

	public static Bitmap rotateBitmap(Bitmap b, int degrees)
	{
		if (degrees != 0 && b != null)
		{
			Matrix m = new Matrix();
			m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
			try
			{
				Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
				if (b != b2)
				{
					b.recycle();
					b = b2;
				}
			}
			catch (OutOfMemoryError e)
			{
				Logger.e("Utils", "Out of memory", e);
			}
		}
		return b;
	}

	public static void setupUri(Context ctx)
	{
		SharedPreferences settings = ctx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		boolean connectUsingSSL = Utils.switchSSLOn(ctx);
		Utils.setupServerURL(settings.getBoolean(HikeMessengerApp.PRODUCTION, true), connectUsingSSL);
	}
		
	public static void setupServerURL(boolean isProductionServer, boolean ssl)
	
	{
		Logger.d("SSL", "Switching SSL on? " + ssl);

		AccountUtils.ssl = ssl;
		AccountUtils.mClient = null;

		String httpString = ssl ? AccountUtils.HTTPS_STRING : AccountUtils.HTTP_STRING;

		AccountUtils.host = isProductionServer ? AccountUtils.PRODUCTION_HOST : AccountUtils.STAGING_HOST;
		AccountUtils.port = isProductionServer ? (ssl ? AccountUtils.PRODUCTION_PORT_SSL : AccountUtils.PRODUCTION_PORT) : (ssl ? AccountUtils.STAGING_PORT_SSL
						: AccountUtils.STAGING_PORT);

		if (isProductionServer)

		{
			AccountUtils.base = httpString + AccountUtils.host + "/v1";
			AccountUtils.baseV2 = httpString + AccountUtils.host + "/v2";
			AccountUtils.SDK_AUTH_BASE = AccountUtils.SDK_AUTH_BASE_URL_PROD;
		}
		else
		{
			AccountUtils.base = httpString + AccountUtils.host + ":" + Integer.toString(AccountUtils.port) + "/v1";
			AccountUtils.baseV2 = httpString + AccountUtils.host + ":" + Integer.toString(AccountUtils.port) + "/v2";
			AccountUtils.SDK_AUTH_BASE = AccountUtils.SDK_AUTH_BASE_URL_STAGING;
		}

		AccountUtils.fileTransferHost = isProductionServer ? AccountUtils.PRODUCTION_FT_HOST : AccountUtils.STAGING_HOST;
		AccountUtils.fileTransferBase = httpString + AccountUtils.fileTransferHost + ":" + Integer.toString(AccountUtils.port) + "/v1";
		 
		CheckForUpdateTask.UPDATE_CHECK_URL = httpString + (isProductionServer ? CheckForUpdateTask.PRODUCTION_URL : CheckForUpdateTask.STAGING_URL);
		 
		AccountUtils.fileTransferBaseDownloadUrl = AccountUtils.fileTransferBase + AccountUtils.FILE_TRANSFER_DOWNLOAD_BASE;
		AccountUtils.fastFileUploadUrl = AccountUtils.fileTransferBase + AccountUtils.FILE_TRANSFER_DOWNLOAD_BASE + "ffu/";
		AccountUtils.fileTransferBaseViewUrl = AccountUtils.HTTP_STRING
				+ (isProductionServer ? AccountUtils.FILE_TRANSFER_BASE_VIEW_URL_PRODUCTION : AccountUtils.FILE_TRANSFER_BASE_VIEW_URL_STAGING);

		AccountUtils.analyticsUploadUrl = AccountUtils.base + AccountUtils.ANALYTICS_UPLOAD_BASE;
		
		AccountUtils.rewardsUrl = isProductionServer ? AccountUtils.REWARDS_PRODUCTION_BASE : AccountUtils.REWARDS_STAGING_BASE;
		AccountUtils.gamesUrl = isProductionServer ? AccountUtils.GAMES_PRODUCTION_BASE : AccountUtils.GAMES_STAGING_BASE;
		AccountUtils.stickersUrl = AccountUtils.HTTP_STRING + (isProductionServer ? AccountUtils.STICKERS_PRODUCTION_BASE : AccountUtils.STICKERS_STAGING_BASE);
		AccountUtils.h2oTutorialUrl = AccountUtils.HTTP_STRING + (isProductionServer ? AccountUtils.H2O_TUTORIAL_PRODUCTION_BASE : AccountUtils.H2O_TUTORIAL_STAGING_BASE);
		Logger.d("SSL", "Base: " + AccountUtils.base);
		Logger.d("SSL", "FTHost: " + AccountUtils.fileTransferHost);
		Logger.d("SSL", "FTUploadBase: " + AccountUtils.fileTransferBase);
		Logger.d("SSL", "UpdateCheck: " + CheckForUpdateTask.UPDATE_CHECK_URL);
		Logger.d("SSL", "FTDloadBase: " + AccountUtils.fileTransferBaseDownloadUrl);
		Logger.d("SSL", "FTViewBase: " + AccountUtils.fileTransferBaseViewUrl);
	}

	private static void setHostAndPort(int whichServer, boolean ssl)
	{
		switch (whichServer)
		{

		case AccountUtils._PRODUCTION_HOST:
			AccountUtils.host = AccountUtils.PRODUCTION_HOST;
			AccountUtils.port = ssl ? AccountUtils.PRODUCTION_PORT_SSL : AccountUtils.PRODUCTION_PORT;
			break;
			
		case AccountUtils._STAGING_HOST:
			AccountUtils.host = AccountUtils.STAGING_HOST;
			AccountUtils.port = ssl ? AccountUtils.STAGING_PORT_SSL : AccountUtils.STAGING_PORT;
			break;
			
		case AccountUtils._DEV_STAGING_HOST:
			AccountUtils.host = AccountUtils.DEV_STAGING_HOST;
			AccountUtils.port = ssl ? AccountUtils.STAGING_PORT_SSL : AccountUtils.STAGING_PORT;
			break;
		}

	}

	public static boolean shouldChangeMessageState(ConvMessage convMessage, int stateOrdinal)
	{
		if (convMessage == null || convMessage.getTypingNotification() != null || convMessage.getUnreadCount() != -1)
		{
			return false;
		}
		int minStatusOrdinal;
		int maxStatusOrdinal;
		if (stateOrdinal <= State.SENT_DELIVERED_READ.ordinal())
		{
			minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
			maxStatusOrdinal = stateOrdinal;
		}
		else
		{
			minStatusOrdinal = State.RECEIVED_UNREAD.ordinal();
			maxStatusOrdinal = stateOrdinal;
		}

		int convMessageStateOrdinal = convMessage.getState().ordinal();

		if (convMessageStateOrdinal <= maxStatusOrdinal && convMessageStateOrdinal >= minStatusOrdinal)
		{
			return true;
		}
		return false;
	}

	public static ConvMessage makeHike2SMSInviteMessage(String msisdn, Context context)
	{
		long time = (long) System.currentTimeMillis() / 1000;

		/*
		 * Randomising the invite text.
		 */
		Random random = new Random();
		int index = random.nextInt(HikeConstants.INVITE_STRINGS.length);

		ConvMessage convMessage = new ConvMessage(getInviteMessage(context, HikeConstants.INVITE_STRINGS[index]), msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setInvite(true);

		return convMessage;
	}

	public static void sendInvite(String msisdn, Context context)
	{
		sendInvite(msisdn, context, false);
	}

	public static void sendInvite(String msisdn, Context context, boolean dbUpdated)
	{
		sendInvite(msisdn, context, dbUpdated, false);
	}

	public static void sendInvite(String msisdn, Context context, boolean dbUpdated, boolean sentMqttPacket)
	{

		boolean sendNativeInvite = !HikeMessengerApp.isIndianUser()
				|| context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SEND_NATIVE_INVITE, false);

		ConvMessage convMessage = Utils.makeHike2SMSInviteMessage(msisdn, context);
		if (!sentMqttPacket)
		{
			HikeMqttManagerNew.getInstance().sendMessage(convMessage.serialize(sendNativeInvite), HikeMqttManagerNew.MQTT_QOS_ONE);
		}

		if (sendNativeInvite)
		{
			SmsManager smsManager = SmsManager.getDefault();
			ArrayList<String> messages = smsManager.divideMessage(convMessage.getMessage());

			ArrayList<PendingIntent> pendingIntents = new ArrayList<PendingIntent>();

			/*
			 * Adding blank pending intents as a workaround for where sms don't get sent when we pass this as null
			 */
			for (int i = 0; i < messages.size(); i++)
			{
				Intent intent = new Intent();
				pendingIntents.add(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
			}
			/*
			 * The try-catch block is needed for a bug in certain LG devices where it throws an NPE here.
			 */
			try
			{
				smsManager.sendMultipartTextMessage(convMessage.getMsisdn(), null, messages, pendingIntents, null);
			}
			catch (NullPointerException e)
			{
				Logger.d("Send invite", "NPE while trying to send SMS", e);
			}
		}

		if (!dbUpdated)
		{
			long time = System.currentTimeMillis() / 1000;
			ContactManager.getInstance().updateInvitedTimestamp(msisdn, time);
		}
	}

	public static enum WhichScreen
	{
		FRIENDS_TAB, UPDATES_TAB, SMS_SECTION, OTHER
	}

	/*
	 * msisdn : mobile number to which we need to send the invite context : context of calling activity v : View of invite button which need to be set invited if not then send this
	 * as null checkPref : preference which need to set to not show this dialog. header : header text of the dialog popup body : body text message of dialog popup
	 */
	public static void sendInviteUtil(final ContactInfo contactInfo, final Context context, final String checkPref, String header, String body)
	{
		sendInviteUtil(contactInfo, context, checkPref, header, body, WhichScreen.OTHER);
	}

	public static void sendInviteUtil(final ContactInfo contactInfo, final Context context, final String checkPref, String header, String body, final WhichScreen whichScreen)
	{
		final SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		if (!settings.getBoolean(checkPref, false) && (!HikeMessengerApp.isIndianUser() || settings.getBoolean(HikeMessengerApp.SEND_NATIVE_INVITE, false)))
		{
			final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
			dialog.setContentView(R.layout.operator_alert_popup);
			dialog.setCancelable(true);

			TextView headerView = (TextView) dialog.findViewById(R.id.header);
			TextView bodyView = (TextView) dialog.findViewById(R.id.body_text);
			Button btnOk = (Button) dialog.findViewById(R.id.btn_ok);
			Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);

			btnCancel.setText(R.string.cancel);
			btnOk.setText(R.string.ok);

			headerView.setText(header);
			bodyView.setText(String.format(body, contactInfo.getFirstName()));

			CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.body_checkbox);
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
			{

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					Editor editor = settings.edit();
					editor.putBoolean(checkPref, isChecked);
					editor.commit();
				}
			});
			checkBox.setText(context.getResources().getString(R.string.not_show_call_alert_msg));

			btnOk.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					dialog.dismiss();
					invite(context, contactInfo, whichScreen);
				}
			});

			btnCancel.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					dialog.dismiss();
				}
			});

			dialog.show();
		}
		else
		{
			invite(context, contactInfo, whichScreen);
		}
	}

	private static void invite(Context context, ContactInfo contactInfo, WhichScreen whichScreen)
	{
		sendInvite(contactInfo.getMsisdn(), context, true);
		Toast.makeText(context, R.string.invite_sent, Toast.LENGTH_SHORT).show();

		boolean isReminding = contactInfo.getInviteTime() != 0;

		long inviteTime = System.currentTimeMillis() / 1000;
		contactInfo.setInviteTime(inviteTime);

		ContactManager.getInstance().updateInvitedTimestamp(contactInfo.getMsisdn(), inviteTime);

		HikeMessengerApp.getPubSub().publish(HikePubSub.INVITE_SENT, null);

		try
		{
			JSONObject md = new JSONObject();
			String msisdn = contactInfo.getMsisdn();
			
			switch (whichScreen)
			{
			case FRIENDS_TAB:
				md.put(HikeConstants.EVENT_KEY, !isReminding ? HikeConstants.LogEvent.INVITE_FTUE_FRIENDS_CLICK : HikeConstants.LogEvent.REMIND_FTUE_FRIENDS_CLICK);				
				break;
			case UPDATES_TAB:
				md.put(HikeConstants.EVENT_KEY, !isReminding ? HikeConstants.LogEvent.INVITE_FTUE_UPDATES_CLICK : HikeConstants.LogEvent.REMIND_FTUE_UPDATES_CLICK);
				break;
			case SMS_SECTION:
				md.put(HikeConstants.EVENT_KEY, !isReminding ? HikeConstants.LogEvent.INVITE_SMS_CLICK : HikeConstants.LogEvent.REMIND_SMS_CLICK);				
				break;
			}
			
			if(!TextUtils.isEmpty(msisdn))
			{
				md.put(HikeConstants.TO, msisdn);
			}
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, md);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	public static String getAddressFromGeoPoint(GeoPoint geoPoint, Context context)
	{
		try
		{
			Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
			List<Address> addresses = geoCoder.getFromLocation(geoPoint.getLatitudeE6() / 1E6, geoPoint.getLongitudeE6() / 1E6, 1);

			final StringBuilder address = new StringBuilder();
			if (!addresses.isEmpty())
			{
				for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
					address.append(addresses.get(0).getAddressLine(i) + "\n");
			}

			return address.toString();
		}
		catch (IOException e)
		{
			Logger.e("Utils", "IOException", e);
			return "";
		}
	}

	public static void addFileName(String fileName, String fileKey)
	{
		File hikeFileList = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT, HikeConstants.HIKE_FILE_LIST_NAME);

		JSONObject currentFiles = getHikeFileListData(hikeFileList);

		if (currentFiles == null)
		{
			Logger.d("Utils", "File did not exist. Will create a new one");
			currentFiles = new JSONObject();
		}
		FileOutputStream fileOutputStream = null;
		ByteArrayInputStream byteArrayInputStream = null;
		try
		{
			Logger.d("Utils", "Adding data : " + "File Name: " + fileName + " File Key: " + fileKey);
			currentFiles.put(fileName, fileKey);
			fileOutputStream = new FileOutputStream(hikeFileList);
			byteArrayInputStream = new ByteArrayInputStream(currentFiles.toString().getBytes("UTF-8"));

			int b;
			byte[] data = new byte[8];
			while ((b = byteArrayInputStream.read(data)) != -1)
			{
				fileOutputStream.write(data, 0, b);
			}
		}
		catch (FileNotFoundException e)
		{
			Logger.e("Utils", "File not found", e);
		}
		catch (JSONException e)
		{
			Logger.e("Utils", "Invalid JSON", e);
		}
		catch (UnsupportedEncodingException e)
		{
			Logger.e("Utils", "Unsupported Encoding Exception", e);
		}
		catch (IOException e)
		{
			Logger.e("Utils", "IOException", e);
		}
		finally
		{
			if (fileOutputStream != null)
			{
				try
				{
					fileOutputStream.flush();
					fileOutputStream.getFD().sync();
					fileOutputStream.close();
				}
				catch (IOException e)
				{
					Logger.e("Utils", "Exception while closing the output stream", e);
				}
			}
		}
	}

	public static String getUniqueFileName(String orgFileName, String fileKey)
	{
		File hikeFileList = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT, HikeConstants.HIKE_FILE_LIST_NAME);
		JSONObject currentFiles = getHikeFileListData(hikeFileList);
		if (currentFiles == null || !currentFiles.has(orgFileName))
		{
			Logger.d("Utils", "File with this name does not exist");
			return orgFileName;
		}

		String fileExtension = orgFileName.contains(".") ? orgFileName.substring(orgFileName.lastIndexOf("."), orgFileName.length()) : "";
		String orgFileNameWithoutExtension = !TextUtils.isEmpty(fileExtension) ? orgFileName.substring(0, orgFileName.indexOf(fileExtension)) : orgFileName;
		StringBuilder newFileName = new StringBuilder(orgFileNameWithoutExtension);

		String currentNameToCheck = orgFileName;
		int i = 1;
		Logger.d("Utils", "File name: " + newFileName.toString() + " Extension: " + fileExtension);
		while (true)
		{
			String existingFileKey = currentFiles.optString(currentNameToCheck);
			if (TextUtils.isEmpty(existingFileKey) || existingFileKey.equals(fileKey))
			{
				break;
			}
			else
			{
				newFileName = new StringBuilder(orgFileNameWithoutExtension + "_" + i++);
				currentNameToCheck = newFileName + fileExtension;
			}
		}
		Logger.d("Utils", "NewFile name: " + newFileName.toString() + " Extension: " + fileExtension);
		newFileName.append(fileExtension);
		return newFileName.toString();
	}

	public static void makeNewFileWithExistingData(JSONObject data)
	{
		File hikeFileList = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT, HikeConstants.HIKE_FILE_LIST_NAME);

		Logger.d("Utils", "Writing data: " + data.toString());

		FileOutputStream fileOutputStream = null;
		ByteArrayInputStream byteArrayInputStream = null;
		try
		{
			fileOutputStream = new FileOutputStream(hikeFileList);
			byteArrayInputStream = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));

			int b;
			byte[] d = new byte[8];
			while ((b = byteArrayInputStream.read(d)) != -1)
			{
				fileOutputStream.write(d, 0, b);
			}
		}
		catch (FileNotFoundException e)
		{
			Logger.e("Utils", "File not found", e);
		}
		catch (UnsupportedEncodingException e)
		{
			Logger.e("Utils", "Unsupported Encoding Exception", e);
		}
		catch (IOException e)
		{
			Logger.e("Utils", "IOException", e);
		}
		finally
		{
			if (fileOutputStream != null)
			{
				try
				{
					fileOutputStream.flush();
					fileOutputStream.getFD().sync();
					fileOutputStream.close();
				}
				catch (IOException e)
				{
					Logger.e("Utils", "Exception while closing the output stream", e);
				}
			}
		}
	}

	private static JSONObject getHikeFileListData(File hikeFileList)
	{
		if (!hikeFileList.exists())
		{
			return null;
		}
		FileInputStream fileInputStream = null;
		JSONObject currentFiles = null;
		try
		{
			fileInputStream = new FileInputStream(hikeFileList);
			BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));

			StringBuilder builder = new StringBuilder();
			CharBuffer target = CharBuffer.allocate(10000);
			int read = reader.read(target);

			while (read >= 0)
			{
				builder.append(target.array(), 0, read);
				target.clear();
				read = reader.read(target);
			}

			currentFiles = new JSONObject(builder.toString());
			Logger.d("Utils", "File found: Current data: " + builder.toString());
		}
		catch (FileNotFoundException e)
		{
			Logger.e("Utils", "File not found", e);
			hikeFileList.delete();
		}
		catch (IOException e)
		{
			Logger.e("Utils", "IOException", e);
			hikeFileList.delete();
		}
		catch (JSONException e)
		{
			Logger.e("Utils", "Invalid JSON", e);
			hikeFileList.delete();
		}
		finally
		{
			if (fileInputStream != null)
			{
				try
				{
					fileInputStream.close();
				}
				catch (IOException e)
				{
					Logger.e("Utils", "Exception while closing the input stream", e);
				}
			}
		}
		return currentFiles;
	}

	public static String getSquareThumbnail(JSONObject obj)
	{
		String thumbnailString = obj.optString(HikeConstants.THUMBNAIL);
		if (TextUtils.isEmpty(thumbnailString))
		{
			return thumbnailString;
		}

		Bitmap thumbnailBmp = HikeBitmapFactory.stringToBitmap(thumbnailString);
		if (!BitmapUtils.isThumbnailSquare(thumbnailBmp))
		{
			Bitmap squareThumbnail = HikeBitmapFactory.makeSquareThumbnail(thumbnailBmp);
			thumbnailString = Base64.encodeToString(BitmapUtils.bitmapToBytes(squareThumbnail, Bitmap.CompressFormat.JPEG), Base64.DEFAULT);
			squareThumbnail.recycle();
			squareThumbnail = null;
		}
		if (!thumbnailBmp.isRecycled())
		{
			thumbnailBmp.recycle();
			thumbnailBmp = null;
		}

		return thumbnailString;
	}

	public static String normalizeNumber(String inputNumber, String countryCode)
	{
		if (inputNumber.startsWith("+"))
		{
			return inputNumber;
		}
		else if (inputNumber.startsWith("00"))
		{
			/*
			 * Doing for US numbers
			 */
			return inputNumber.replaceFirst("00", "+");
		}
		else if (inputNumber.startsWith("0"))
		{
			return inputNumber.replaceFirst("0", countryCode);
		}
		else
		{
			return countryCode + inputNumber;
		}
	}

	public static void downloadAndSaveFile(Context context, File destFile, Uri uri) throws Exception
	{
		InputStream is = null;
		OutputStream os = null;
		try
		{

			if (isPicasaUri(uri.toString()) && !uri.toString().startsWith("http"))
			{
				is = context.getContentResolver().openInputStream(uri);
			}
			else
			{
				is = new URL(uri.toString()).openStream();
			}
			os = new FileOutputStream(destFile);

			byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
			int len;

			while ((len = is.read(buffer)) > 0)
			{
				os.write(buffer, 0, len);
			}
		}
		finally
		{
			if (os != null)
			{
				os.close();
			}
			if (is != null)
			{
				is.close();
			}
		}
	}

	public static boolean isPicasaUri(String picasaUriString)
	{
		return (picasaUriString.toString().startsWith(HikeConstants.OTHER_PICASA_URI_START) || picasaUriString.toString().startsWith(HikeConstants.JB_PICASA_URI_START)
				|| picasaUriString.toString().startsWith("http") || picasaUriString.toString().startsWith(HikeConstants.GMAIL_PREFIX) || picasaUriString.toString().startsWith(
				HikeConstants.GOOGLE_PLUS_PREFIX));
	}

	public static Uri makePicasaUri(Uri uri)
	{
		if (uri.toString().startsWith("content://com.android.gallery3d.provider"))
		{
			// use the com.google provider, not the com.android
			// provider.
			return Uri.parse(uri.toString().replace("com.android.gallery3d", "com.google.android.gallery3d"));
		}
		return uri;
	}

	/**
	 * This will return true when SSL toggle is on and connection type is WIFI
	 * 
	 * @param context
	 * @return
	 */
	public static boolean switchSSLOn(Context context)
	{
		/*
		 * If the preference itself is switched to off, we don't need to check if the wifi is on or off.
		 */
		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.SSL_PREF, true))
		{
			return false;
		}
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		try
		{
			return (cm != null && cm.getActiveNetworkInfo() != null && (cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI));
		}
		catch (NullPointerException e)
		{
			// TODO Auto-generated catch block
			return false;
		}
	}

	public static boolean renameTempProfileImage(String msisdn)
	{
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		String tempFileName = Utils.getTempProfileImageFileName(msisdn);
		String newFileName = Utils.getProfileImageFileName(msisdn);

		File tempFile = new File(directory, tempFileName);
		File newFile = new File(directory, newFileName);
		return tempFile.renameTo(newFile);
	}

	public static boolean removeTempProfileImage(String msisdn)
	{
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		String tempFileName = Utils.getTempProfileImageFileName(msisdn);

		return (new File(directory, tempFileName)).delete();
	}

	public static String getTempProfileImageFileName(String msisdn)
	{
		return getValidFileNameForMsisdn(msisdn) + "_tmp.jpg";
	}

	public static String getProfileImageFileName(String msisdn)
	{
		return getValidFileNameForMsisdn(msisdn) + ".jpg";
	}

	public static String getValidFileNameForMsisdn(String msisdn)
	{
		return msisdn.replaceAll(":", "-");
	}

	public static void removeLargerProfileImageForMsisdn(String msisdn)
	{
		String path = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		String fileName = Utils.getProfileImageFileName(msisdn);
		(new File(path, fileName)).delete();
	}

	public static void vibrateNudgeReceived(Context context)
	{
		String VIB_OFF = context.getResources().getString(R.string.vib_off);
		if (VIB_OFF.equals(PreferenceManager.getDefaultSharedPreferences(context).getString(HikeConstants.VIBRATE_PREF_LIST, getOldVibratePref(context))))
		{
			return;
		}
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		int ringerMode = audioManager.getRingerMode();

		if (ringerMode != AudioManager.RINGER_MODE_SILENT && !Utils.isUserInAnyTypeOfCall(context))
		{
			vibrate(100);
		}
	}

	public static void vibrate(int msecs)
	{
		Vibrator vibrator = (Vibrator) HikeMessengerApp.getInstance().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
		if (vibrator != null)
		{
			vibrator.vibrate(msecs);
		}
	}

	private static String convertToHex(byte[] data)
	{
		StringBuilder buf = new StringBuilder();
		for (byte b : data)
		{
			int halfbyte = (b >>> 4) & 0x0F;
			int two_halfs = 0;
			do
			{
				buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
				halfbyte = b & 0x0F;
			}
			while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(text.getBytes("iso-8859-1"), 0, text.length());
		byte[] sha1hash = md.digest();
		return convertToHex(sha1hash);
	}

	public static String getHashedDeviceId(String deviceId) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		return "and:" + SHA1(deviceId);
	}

	public static void startCropActivity(Activity activity, String path, String destPath)
	{
		/* Crop the image */
		Intent intent = new Intent(activity, CropImage.class);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, destPath);
		intent.putExtra(HikeConstants.Extras.IMAGE_PATH, path);
		intent.putExtra(HikeConstants.Extras.SCALE, true);
		intent.putExtra(HikeConstants.Extras.OUTPUT_X, HikeConstants.MAX_DIMENSION_FULL_SIZE_PROFILE_PX);
		intent.putExtra(HikeConstants.Extras.OUTPUT_Y, HikeConstants.MAX_DIMENSION_FULL_SIZE_PROFILE_PX);
		intent.putExtra(HikeConstants.Extras.ASPECT_X, 1);
		intent.putExtra(HikeConstants.Extras.ASPECT_Y, 1);
		activity.startActivityForResult(intent, HikeConstants.CROP_RESULT);
	}

	public static long getContactId(Context context, long rawContactId)
	{
		Cursor cur = null;
		try
		{
			cur = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, new String[] { ContactsContract.RawContacts.CONTACT_ID },
					ContactsContract.RawContacts._ID + "=" + rawContactId, null, null);
			if (cur.moveToFirst())
			{
				return cur.getLong(cur.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (cur != null)
			{
				cur.close();
			}
		}
		return -1l;
	}

	public static List<ContactInfoData> getContactDataFromHikeFile(HikeFile hikeFile)
	{
		List<ContactInfoData> items = new ArrayList<ContactInfoData>();

		JSONArray phoneNumbers = hikeFile.getPhoneNumbers();
		JSONArray emails = hikeFile.getEmails();
		JSONArray events = hikeFile.getEvents();
		JSONArray addresses = hikeFile.getAddresses();

		if (phoneNumbers != null)
		{
			for (int i = 0; i < phoneNumbers.length(); i++)
			{
				JSONObject data = phoneNumbers.optJSONObject(i);
				String key = data.names().optString(0);
				items.add(new ContactInfoData(DataType.PHONE_NUMBER, data.optString(key), key));
			}
		}

		if (emails != null)
		{
			for (int i = 0; i < emails.length(); i++)
			{
				JSONObject data = emails.optJSONObject(i);
				String key = data.names().optString(0);
				items.add(new ContactInfoData(DataType.EMAIL, data.optString(key), key));
			}
		}

		if (events != null)
		{
			for (int i = 0; i < events.length(); i++)
			{
				JSONObject data = events.optJSONObject(i);
				String key = data.names().optString(0);
				items.add(new ContactInfoData(DataType.EVENT, data.optString(key), key));
			}
		}

		if (addresses != null)
		{
			for (int i = 0; i < addresses.length(); i++)
			{
				JSONObject data = addresses.optJSONObject(i);
				String key = data.names().optString(0);
				items.add(new ContactInfoData(DataType.ADDRESS, data.optString(key), key));
			}
		}
		return items;
	}

	/**
	 * Get unseen status, user-status and friend request count
	 * 
	 * @param accountPrefs Account settings shared preference
	 * @param countUsersStatus Whether to include user status count in the total
	 * @return
	 */
	public static int getNotificationCount(SharedPreferences accountPrefs, boolean countUsersStatus)
	{
		int notificationCount = 0;

		notificationCount += accountPrefs.getInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);

		if (countUsersStatus)
		{
			notificationCount += accountPrefs.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
		}

		int frCount = accountPrefs.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0);
		notificationCount += frCount;
		return notificationCount;
	}

	/*
	 * This method returns whether the device is an mdpi or ldpi device. The assumption is that these devices are low end and hence a DB call may block the UI on those devices.
	 */
	public static boolean loadOnUiThread()
	{
		return ((int) 10 * Utils.scaledDensityMultiplier) > 10;
	}

	public static void hideSoftKeyboard(Context context, View v)
	{
		if (v == null)
		{
			return;
		}
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

	public static void showSoftKeyboard(Context context, View v)
	{
		if (v == null)
		{
			return;
		}
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(v, InputMethodManager.RESULT_UNCHANGED_SHOWN);
	}

	public static void sendLocaleToServer(Context context)
	{
		JSONObject object = new JSONObject();
		JSONObject data = new JSONObject();

		try
		{
			data.put(HikeConstants.LOCALE, context.getResources().getConfiguration().locale.getLanguage());
			data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis() / 1000));

			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);
			object.put(HikeConstants.DATA, data);

			HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ONE);
		}
		catch (JSONException e)
		{
			Logger.w("Locale", "Invalid JSON", e);
		}
	}

	public static void setReceiveSmsSetting(Context context, boolean value)
	{
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(HikeConstants.RECEIVE_SMS_PREF, value);
		editor.commit();

		sendDefaultSMSClientLogEvent(value);
	}

	public static void setSendUndeliveredAlwaysAsSmsSetting(Context context, boolean value)
	{
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, value);
		editor.commit();
	}

	public static void setSendUndeliveredAlwaysAsSmsSetting(Context context, boolean value, boolean nativeSms)
	{
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, value);
		editor.putBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, nativeSms);
		editor.commit();
	}

	public static boolean isContactInternational(String msisdn)
	{
		return !msisdn.startsWith("+91");
	}

	public static Dialog showSMSSyncDialog(final Context context, boolean syncConfirmation)
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.enable_sms_client_popup);

		final View btnContainer = dialog.findViewById(R.id.button_container);

		final ProgressBar syncProgress = (ProgressBar) dialog.findViewById(R.id.loading_progress);
		TextView header = (TextView) dialog.findViewById(R.id.header);
		final TextView info = (TextView) dialog.findViewById(R.id.body);
		Button okBtn = (Button) dialog.findViewById(R.id.btn_ok);
		Button cancelBtn = (Button) dialog.findViewById(R.id.btn_cancel);
		final View btnDivider = dialog.findViewById(R.id.sms_divider);

		header.setText(R.string.import_sms);
		info.setText(R.string.import_sms_info);
		okBtn.setText(R.string.yes);
		cancelBtn.setText(R.string.no);

		setupSyncDialogLayout(syncConfirmation, btnContainer, syncProgress, info, btnDivider);

		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.SMS_SYNC_START, null);

				executeSMSSyncStateResultTask(new SyncOldSMSTask(context));

				setupSyncDialogLayout(false, btnContainer, syncProgress, info, btnDivider);

				sendSMSSyncLogEvent(true);
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dialog.dismiss();

				sendSMSSyncLogEvent(false);
			}
		});

		dialog.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss(DialogInterface dialog)
			{
				Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
				editor.putBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, true);
				editor.commit();
			}
		});

		dialog.show();
		return dialog;
	}

	private static void setupSyncDialogLayout(boolean syncConfirmation, View btnContainer, ProgressBar syncProgress, TextView info, View btnDivider)
	{
		btnContainer.setVisibility(syncConfirmation ? View.VISIBLE : View.GONE);
		syncProgress.setVisibility(syncConfirmation ? View.GONE : View.VISIBLE);
		btnDivider.setVisibility(syncConfirmation ? View.VISIBLE : View.GONE);
		info.setText(syncConfirmation ? R.string.import_sms_info : R.string.importing_sms_info);
	}

	public static int getResolutionId()
	{
		if(densityDpi > 480)
		{
			return HikeConstants.XXXHDPI_ID;
		}
		else if(densityDpi > 320)
		{
			return HikeConstants.XXHDPI_ID;
		}
		else if(densityDpi > 240)
		{
			return HikeConstants.XHDPI_ID;
		}
		else if(densityDpi > 160)
		{
			return HikeConstants.HDPI_ID;
		}
		else if(densityDpi > 120)
		{
			return HikeConstants.MDPI_ID;
		}
		else
		{
			return HikeConstants.LDPI_ID;
		}
	}

	public static void saveBase64StringToFile(File file, String base64String) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(file);

		byte[] b = Base64.decode(base64String, Base64.DEFAULT);
		if (b == null)
		{
			throw new IOException();
		}
		fos.write(b);
		fos.flush();
		fos.getFD().sync();
		fos.close();
	}

	public static void setupFormattedTime(TextView tv, long timeElapsed)
	{
		if(timeElapsed < 0)
			return;
		int totalSeconds = (int) (timeElapsed);
		int minutesToShow = (int) (totalSeconds / 60);
		int secondsToShow = totalSeconds % 60;

		String time = String.format("%d:%02d", minutesToShow, secondsToShow);
		tv.setText(time);
	}

	public static boolean isUserAuthenticated(Context context)
	{
		return !TextUtils.isEmpty(context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.NAME_SETTING, null));
	}

	public static void appStateChanged(Context context)
	{
		appStateChanged(context, true, false);
	}

	public static void appStateChanged(Context context, boolean resetStealth, boolean checkIfActuallyBackgrounded)
	{
		appStateChanged(context, resetStealth, checkIfActuallyBackgrounded, true, false, true);
	}

	public static void appStateChanged(Context context, boolean resetStealth,
			boolean checkIfActuallyBackgrounded, boolean requestBulkLastSeen, boolean dueToConnect, boolean toLog)
	{
		if (!isUserAuthenticated(context))
		{
			return;
		}

		if (checkIfActuallyBackgrounded)
		{
			boolean screenOn = isScreenOn(context);
			Logger.d("HikeAppState", "Screen On? " + screenOn);

			if (screenOn)
			{
				boolean isForegrounded = isAppForeground(context);

				if (isForegrounded)
				{
					if (HikeMessengerApp.currentState != CurrentState.OPENED && HikeMessengerApp.currentState != CurrentState.RESUMED)
					{
						Logger.d("HikeAppState", "Wrong state! correcting it");
						HikeMessengerApp.currentState = CurrentState.RESUMED;
						return;
					}
				}
			}
		}

		sendAppState(context, requestBulkLastSeen, dueToConnect, toLog);

		if (resetStealth)
		{
			if (HikeMessengerApp.currentState != CurrentState.OPENED && HikeMessengerApp.currentState != CurrentState.RESUMED)
			{
				resetStealthMode(context);
			}
			else
			{
				clearStealthResetTimer(context);
			}
		}
	}

	public static boolean isScreenOn(Context context)
	{
		return ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isScreenOn();
	}

	private static void sendAppState(Context context, boolean requestBulkLastSeen, boolean dueToConnect, boolean toLog)
	{
		JSONObject object = new JSONObject();

		try
		{
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.APP_STATE);
			if (HikeMessengerApp.currentState == CurrentState.OPENED || HikeMessengerApp.currentState == CurrentState.RESUMED)
			{
				object.put(HikeConstants.SUB_TYPE, HikeConstants.FOREGROUND);

				JSONObject data = new JSONObject();
				data.put(HikeConstants.JUST_OPENED, HikeMessengerApp.currentState == CurrentState.OPENED);
				/*
				 * We don't need to request for the bulk last seen from here anymore. We have the HTTP call for this.
				 */
				data.put(HikeConstants.BULK_LAST_SEEN, false);
				object.put(HikeConstants.DATA, data);

				HikeMessengerApp.getPubSub().publish(HikePubSub.APP_FOREGROUNDED, null);
				if(toLog)
				{
					HAManager.getInstance().recordSessionStart();
				}
			}
			else if (!dueToConnect)
			{
				object.put(HikeConstants.SUB_TYPE, HikeConstants.BACKGROUND);
				HikeMessengerApp.getPubSub().publish(HikePubSub.APP_BACKGROUNDED, null);
				if(toLog)
				{
					HAManager.getInstance().recordSessionEnd();
				}
			}
			else
			{
				return;
			}
			HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ZERO);
		}
		catch (JSONException e)
		{
			Logger.w("AppState", "Invalid json", e);
		}
	}

	private static void resetStealthMode(Context context)
	{
		StealthResetTimer.getInstance(context).resetStealthToggle();
	}

	private static void clearStealthResetTimer(Context context)
	{
		StealthResetTimer.getInstance(context).clearScheduledStealthToggleTimer();
	}

	public static String getLastSeenTimeAsString(Context context, long lastSeenTime, int offline)
	{
		return getLastSeenTimeAsString(context, lastSeenTime, offline, false);
	}

	public static String getLastSeenTimeAsString(Context context, long lastSeenTime, int offline, boolean groupParticipant)
	{
		return getLastSeenTimeAsString(context, lastSeenTime, offline, groupParticipant, false);
	}

	public static String getLastSeenTimeAsString(Context context, long lastSeenTime, int offline, boolean groupParticipant, boolean fromChatThread)
	{
		/*
		 * This refers to the setting being turned off
		 */
		if (offline == -1)
		{
			return null;
		}
		/*
		 * This refers to the user being online
		 */
		if (offline == 0)
		{
			return context.getString(R.string.online);
		}

		long lastSeenTimeMillis = lastSeenTime * 1000;
		Calendar lastSeenCalendar = Calendar.getInstance();
		lastSeenCalendar.setTimeInMillis(lastSeenTimeMillis);

		Date lastSeenDate = new Date(lastSeenTimeMillis);

		Calendar nowCalendar = Calendar.getInstance();

		int lastSeenYear = lastSeenCalendar.get(Calendar.YEAR);
		int nowYear = nowCalendar.get(Calendar.YEAR);

		int lastSeenDay = lastSeenCalendar.get(Calendar.DAY_OF_YEAR);
		int nowDay = nowCalendar.get(Calendar.DAY_OF_YEAR);

		int lastSeenDayOfMonth = lastSeenCalendar.get(Calendar.DAY_OF_MONTH);

		/*
		 * More than 7 days old.
		 */
		if ((lastSeenYear < nowYear) || ((nowDay - lastSeenDay) > 7))
		{
			return context.getString(fromChatThread ? R.string.last_seen_while_ago_ct : R.string.last_seen_while_ago);
		}

		boolean is24Hour = android.text.format.DateFormat.is24HourFormat(context);

		String lastSeen;
		/*
		 * More than 1 day old.
		 */
		if ((nowDay - lastSeenDay) > 1)
		{
			String format;
			if (groupParticipant)
			{
				format = "dd/MM/yy";
				DateFormat df = new SimpleDateFormat(format);
				lastSeen = df.format(lastSeenDate);
			}
			else
			{
				if (is24Hour)
				{
					format = "d'" + getDayOfMonthSuffix(lastSeenDayOfMonth) + "' MMM, HH:mm";
				}
				else
				{
					format = "d'" + getDayOfMonthSuffix(lastSeenDayOfMonth) + "' MMM, h:mmaaa";
				}
				DateFormat df = new SimpleDateFormat(format);
				lastSeen = context.getString(fromChatThread ? R.string.last_seen_more_ct : R.string.last_seen_more, df.format(lastSeenDate));
			}
		}
		else
		{
			String format;
			if (is24Hour)
			{
				format = "HH:mm";
			}
			else
			{
				format = "h:mmaaa";
			}

			DateFormat df = new SimpleDateFormat(format);
			if (groupParticipant)
			{
				lastSeen = (nowDay > lastSeenDay) ? context.getString(R.string.last_seen_yesterday_group_participant) : df.format(lastSeenDate);
			}
			else
			{
				int stringRes;
				if (fromChatThread)
				{
					stringRes = (nowDay > lastSeenDay) ? R.string.last_seen_yesterday_ct : R.string.last_seen_today_ct;
				}
				else
				{
					stringRes = (nowDay > lastSeenDay) ? R.string.last_seen_yesterday : R.string.last_seen_today;
				}
				lastSeen = context.getString(stringRes, df.format(lastSeenDate));
			}
		}

		lastSeen = lastSeen.replace("AM", "am");
		lastSeen = lastSeen.replace("PM", "pm");

		return lastSeen;

	}

	private static String getDayOfMonthSuffix(int dayOfMonth)
	{
		if (dayOfMonth >= 11 && dayOfMonth <= 13)
		{
			return "th";
		}
		switch (dayOfMonth % 10)
		{
		case 1:
			return "st";
		case 2:
			return "nd";
		case 3:
			return "rd";
		default:
			return "th";
		}
	}

	public static long getServerTimeOffset(Context context)
	{
		return context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getLong(HikeMessengerApp.SERVER_TIME_OFFSET, 0);
	}

	/**
	 * Applies the server time offset and ensures that the time does not go into the future
	 * 
	 * @param context
	 * @param time
	 * @return
	 */
	public static long applyServerTimeOffset(Context context, long time)
	{
		time += getServerTimeOffset(context);
		long now = System.currentTimeMillis() / 1000;
		if (time > now)
		{
			return now;
		}
		else
		{
			return time;
		}
	}

	public static void blockOrientationChange(Activity activity)
	{
		final int rotation = activity.getWindowManager().getDefaultDisplay().getOrientation();

		boolean isPortrait = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO || rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
		{
			activity.setRequestedOrientation(isPortrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
		else if (rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_270)
		{
			activity.setRequestedOrientation(isPortrait ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
		}
	}

	public static void unblockOrientationChange(Activity activity)
	{
		if (activity == null)
		{
			return;
		}
		activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
	}

	public static String getMessageDisplayText(ConvMessage convMessage, Context context)
	{
		if (convMessage.isFileTransferMessage())
		{
			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);

			switch (hikeFile.getHikeFileType())
			{
			case IMAGE:
				return context.getString(R.string.send_sms_img_msg);
			case VIDEO:
				return context.getString(R.string.send_sms_video_msg);
			case AUDIO:
				return context.getString(R.string.send_sms_audio_msg);
			case LOCATION:
				return context.getString(R.string.send_sms_location_msg);
			case CONTACT:
				return context.getString(R.string.send_sms_contact_msg);
			case AUDIO_RECORDING:
				return context.getString(R.string.send_sms_audio_msg);

			default:
				return context.getString(R.string.send_sms_file_msg);
			}

		}
		else if (convMessage.isStickerMessage())
		{
			return context.getString(R.string.send_sms_sticker_msg);
		}
		return convMessage.getMessage();
	}

	public static void deleteFile(File file)
	{
		if (file.isDirectory())
		{
			for (File f : file.listFiles())
			{
				deleteFile(f);
			}
		}
		file.delete();
	}

	public static void sendLogEvent(JSONObject data)
	{
		sendLogEvent(data, null, null);
	}
	
	public static void sendLogEvent(JSONObject data, String subType, String toMsisdn){

		JSONObject object = new JSONObject();
		try
		{
			data.put(HikeConstants.LogEvent.TAG, HikeConstants.LOGEVENT_TAG);
			data.put(HikeConstants.C_TIME_STAMP, System.currentTimeMillis());
			data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis() / 1000));
			if(!TextUtils.isEmpty(subType))
			{
				data.put(HikeConstants.SUB_TYPE, subType);
			}
			if(!TextUtils.isEmpty(toMsisdn))
			{
				object.put(HikeConstants.TO, toMsisdn);
			}
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ANALYTICS_EVENT);
			object.put(HikeConstants.DATA, data);

			HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ONE);
		}
		catch (JSONException e)
		{
			Logger.w("LogEvent", e);
		}
	
	}

	private static void sendSMSSyncLogEvent(boolean syncing)
	{
		JSONObject metadata = new JSONObject();

		try
		{
			metadata.put(HikeConstants.PULL_OLD_SMS, syncing);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.SMS, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}

	}

	public static void sendDefaultSMSClientLogEvent(boolean defaultClient)
	{
		JSONObject metadata = new JSONObject();

		try
		{
			metadata.put(HikeConstants.UNIFIED_INBOX, defaultClient);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.SMS, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}

	}

	public static void sendFreeSmsLogEvent(boolean freeSmsOn)
	{
		JSONObject metadata = new JSONObject();

		try
		{
			metadata.put(HikeConstants.FREE_SMS_ON, freeSmsOn);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.SMS, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}

	}

	public static void sendNativeSmsLogEvent(boolean nativeSmsOn)
	{
		JSONObject metadata = new JSONObject();

		try
		{
			metadata.put(HikeConstants.NATIVE_SMS, nativeSmsOn);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.SMS, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}

	}

	public static JSONObject getJSONfromURL(String url)
	{

		// initialize
		InputStream is = null;
		String result = "";
		JSONObject jObject = null;

		// http post
		try
		{
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(url);
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();

			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				sb.append(line + "\n");
			}
			is.close();
			result = sb.toString();
		}
		catch (Exception e)
		{
			Logger.e("LogEvent", "Error converting result " + e.toString());
		}

		// try parse the string to a JSON object
		try
		{
			jObject = new JSONObject(result);
		}
		catch (JSONException e)
		{
			Logger.e("LogEvent", "Error parsing data " + e.toString());
		}

		return jObject;
	}
	
	public static boolean isGingerbreadOrHigher()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}
	
	public static boolean isHoneycombOrHigher()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean isKitkatOrHigher()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	}
	
	public static boolean isJELLY_BEAN_MR2OrHigher()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
	}
	public static void executeAsyncTask(AsyncTask<Void, Void, Void> asyncTask)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
	}

	public static void executeFtResultAsyncTask(AsyncTask<Void, Void, FTResult> asyncTask)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
	}

	public static void executeIntProgFtResultAsyncTask(AsyncTask<Void, Integer, FTResult> asyncTask)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
	}

	public static void executeBoolResultAsyncTask(AsyncTask<Void, Void, Boolean> asyncTask)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
	}

	public static void executeHttpTask(AsyncTask<HikeHttpRequest, Integer, Boolean> asyncTask, HikeHttpRequest... hikeHttpRequests)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, hikeHttpRequests);
		}
		else
		{
			asyncTask.execute(hikeHttpRequests);
		}
	}
	
	public static void executeAuthSDKTask(AuthSDKAsyncTask argTask, HttpRequestBase... requests)
	{
		if (isHoneycombOrHigher())
		{
			argTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, requests);
		}
		else
		{
			argTask.execute(requests);
		}
	}

	public static void executeSignupTask(AsyncTask<Void, SignupTask.StateValue, Boolean> asyncTask)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
	}

	public static void executeLongResultTask(AsyncTask<Void, Void, Long> asyncTask)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
	}

	public static void executeContactListResultTask(AsyncTask<Void, Void, List<Pair<AtomicBoolean, ContactInfo>>> asyncTask)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
	}

	public static void executeContactInfoListResultTask(AsyncTask<Void, Void, FtueContactsData> asyncTask)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
	}

	public static void executeStringResultTask(AsyncTask<Void, Void, String> asyncTask)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
	}

	public static void executeSMSSyncStateResultTask(AsyncTask<Void, Void, SMSSyncState> asyncTask)
	{
		if (isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
	}

	public static void executeConvAsyncTask(AsyncTask<Conversation, Void, Conversation[]> asyncTask, Conversation... conversations)
	{
		if (Utils.isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, conversations);
		}
		else
		{
			asyncTask.execute(conversations);
		}
	}

	public static boolean getSendSmsPref(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.SEND_SMS_PREF, false);
	}

	public static boolean isFilenameValid(String file)
	{
		File f = new File(file);
		try
		{
			f.getCanonicalPath();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	public static void resetUnseenStatusCount(Context context)
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
	}

	public static void resetUnseenFriendRequestCount(Context context)
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.FRIEND_REQ_COUNT, 0) > 0)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.FRIEND_REQ_COUNT, 0);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_COUNT_CHANGED, null);
	}

	public static boolean shouldIncrementCounter(ConvMessage convMessage)
	{
		return !convMessage.isSent() && convMessage.getState() == State.RECEIVED_UNREAD && convMessage.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE;
	}

	public static Intent createIntentForConversation(Context context, Conversation conversation)
	{
		Intent intent = new Intent(context, ChatThread.class);
		if (conversation.getContactName() != null)
		{
			intent.putExtra(HikeConstants.Extras.NAME, conversation.getContactName());
		}
		intent.putExtra(HikeConstants.Extras.MSISDN, conversation.getMsisdn());
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static void createShortcut(Activity activity, Conversation conv)
	{
		Intent shortcutIntent = Utils.createIntentForConversation(activity, conv);
		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, conv.getLabel());

		Drawable avatarDrawable = Utils.getAvatarDrawableForNotificationOrShortcut(activity, conv.getMsisdn(), false);

		Bitmap bitmap = HikeBitmapFactory.drawableToBitmap(avatarDrawable, Bitmap.Config.RGB_565);

		int dimension = (int) (Utils.scaledDensityMultiplier * 48);

		Bitmap scaled = HikeBitmapFactory.createScaledBitmap(bitmap, dimension, dimension, Bitmap.Config.RGB_565, false, true, true);
		bitmap = null;
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, scaled);
		intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		activity.sendBroadcast(intent);
		Toast.makeText(activity, R.string.shortcut_created, Toast.LENGTH_SHORT).show();
	}

	public static boolean isVoipActivated(Context context)
	{
		int voipActivated = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.VOIP_ACTIVATED, 1);
		return (voipActivated == 0)? false : true;
	}

	public static void onCallClicked(Context context, final String mContactNumber, VoIPUtils.CallSource source)
	{
		if(!isUserOnline(context))
		{
			Toast.makeText(context, context.getString(R.string.voip_offline_error), Toast.LENGTH_SHORT).show();
			return;
		}
		context.startService(IntentManager.getVoipCallIntent(context, mContactNumber, source));
	}

	public static String getFormattedDateTimeFromTimestamp(long milliSeconds, Locale current)
	{
		String dateFormat = "dd/MM/yyyy hh:mm:ss a";
		DateFormat formatter = new SimpleDateFormat(dateFormat, current);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(milliSeconds * 1000);
		return formatter.format(calendar.getTime());
	}
	
	public static String getFormattedDateTimeWOSecondsFromTimestamp(long milliSeconds, Locale current)
	{
		String dateFormat = "dd/MM/yyyy hh:mm a";
		DateFormat formatter = new SimpleDateFormat(dateFormat, current);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(milliSeconds * 1000);
		return formatter.format(calendar.getTime());
	}

	public static void sendUILogEvent(String key)
	{
		sendUILogEvent(key, null);
	}

	public static void sendUILogEvent(String key, String msisdn)
	{
		try
		{
			JSONObject data = new JSONObject();
			data.put(HikeConstants.SUB_TYPE, HikeConstants.UI_EVENT);

			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.CLICK);
			metadata.put(HikeConstants.EVENT_KEY, key);

			if (!TextUtils.isEmpty(msisdn))
			{
				JSONArray msisdns = new JSONArray();
				msisdns.put(msisdn);

				metadata.put(HikeConstants.TO, msisdns);
			}

			data.put(HikeConstants.METADATA, metadata);

			sendLogEvent(data);
		}
		catch (JSONException e)
		{
			Logger.w("LE", "Invalid json");
		}
	}

	public static void sendMd5MismatchEvent(String fileName, String fileKey, String md5, int recBytes, boolean downloading)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.FILE_NAME, fileName);
			metadata.put(HikeConstants.FILE_KEY, fileKey);
			metadata.put(HikeConstants.MD5_HASH, md5);
			metadata.put(HikeConstants.FILE_SIZE, recBytes);
			metadata.put(HikeConstants.DOWNLOAD, downloading);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.CRC_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(AnalyticsConstants.ANALYTICS_TAG, "Invalid json");
		}
	}

	public static void resetUpdateParams(SharedPreferences prefs)
	{
		Editor prefEditor = prefs.edit();
		prefEditor.remove(HikeMessengerApp.DEVICE_DETAILS_SENT);
		prefEditor.remove(HikeMessengerApp.UPGRADE_RAI_SENT);
		prefEditor.putBoolean(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, true);
		prefEditor.putBoolean(HikeMessengerApp.SIGNUP_COMPLETE, true);
		prefEditor.commit();
	}

	public static String fileToMD5(String filePath)
	{
		InputStream inputStream = null;
		try
		{
			inputStream = new FileInputStream(filePath);
			byte[] buffer = new byte[1024];
			MessageDigest digest = MessageDigest.getInstance("MD5");
			int numRead = 0;
			while (numRead != -1)
			{
				numRead = inputStream.read(buffer);
				if (numRead > 0)
					digest.update(buffer, 0, numRead);
			}
			byte[] md5Bytes = digest.digest();
			return convertHashToString(md5Bytes);
		}
		catch (Exception e)
		{
			return null;
		}
		finally
		{
			if (inputStream != null)
			{
				try
				{
					inputStream.close();
				}
				catch (Exception e)
				{
				}
			}
		}
	}

	private static String convertHashToString(byte[] md5Bytes)
	{
		String returnVal = "";
		for (int i = 0; i < md5Bytes.length; i++)
		{
			returnVal += Integer.toString((md5Bytes[i] & 0xff) + 0x100, 16).substring(1);
		}
		return returnVal;
	}

	public static Intent getHomeActivityIntent(Context context)
	{
		final Intent intent = new Intent(context, HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		return intent;
	}

	public static Intent getPeopleActivityIntent(Context context)
	{
		final Intent intent = new Intent(context, PeopleActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(HikeConstants.Extras.FROM_NOTIFICATION, true);

		return intent;
	}

	public static Intent getTimelineActivityIntent(Context context)
	{
		final Intent intent = new Intent(context, TimelineActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(HikeConstants.Extras.FROM_NOTIFICATION, true);

		return intent;
	}

	public static void addCommonDeviceDetails(JSONObject jsonObject, Context context) throws JSONException
	{
		int height = context.getResources().getDisplayMetrics().heightPixels;
		int width = context.getResources().getDisplayMetrics().widthPixels;

		TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		String res = height + "x" + width;
		String operator = manager.getSimOperatorName();
		String circle = manager.getSimOperator();
		String pdm = Float.toString(Utils.scaledDensityMultiplier);

		jsonObject.put(HikeConstants.RESOLUTION, res);
		jsonObject.put(HikeConstants.OPERATOR, operator);
		jsonObject.put(HikeConstants.CIRCLE, circle);
		jsonObject.put(HikeConstants.PIXEL_DENSITY_MULTIPLIER, pdm);
	}

	public static ConvMessage makeConvMessage(String msisdn, boolean conversationOnHike)
	{
		return makeConvMessage(msisdn, "", conversationOnHike);
	}

	public static ConvMessage makeConvMessage(String msisdn, String message, boolean isOnhike)
	{
		return makeConvMessage(msisdn, message, isOnhike, State.SENT_UNCONFIRMED);
	}

	public static ConvMessage makeConvMessage(String msisdn, String message, boolean isOnhike, State state)
	{
		long time = (long) System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(message, msisdn, time, state);
		convMessage.setSMS(!isOnhike);

		return convMessage;
	}

	public static boolean canInBitmap()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean hasFroyo()
	{
		// Can use static final constants like FROYO, declared in later versions
		// of the OS since they are inlined at compile time. This is guaranteed
		// behavior.
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}

	public static boolean hasGingerbread()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}

	public static boolean hasHoneycombMR1()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
	}

	public static boolean hasJellyBean()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}
	
	public static boolean hasJellyBeanMR1()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
	}

	public static boolean hasKitKat()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	}
	
	public static boolean hasIceCreamSandwich()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}

	public static boolean hasEnoughFreeSpaceForProfilePic()
	{
		double freeSpaceAvailable = getFreeSpace();
		return freeSpaceAvailable > HikeConstants.PROFILE_PIC_FREE_SPACE;
	}

	public static void addToContacts(List<ContactInfoData> items, String name, Context context)
	{
		Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		int phoneCount = 0;
		int emailCount = 0;
		i.putExtra(Insert.NAME, name);
		for (ContactInfoData contactData : items)
		{
			if (contactData.getDataType() == DataType.PHONE_NUMBER)
			{
				switch (phoneCount)
				{
				case 0:
					i.putExtra(Insert.PHONE, contactData.getData());
					break;
				case 1:
					i.putExtra(Insert.SECONDARY_PHONE, contactData.getData());
					break;
				case 2:
					i.putExtra(Insert.TERTIARY_PHONE, contactData.getData());
					break;
				default:
					break;
				}
				phoneCount++;
			}
			else if (contactData.getDataType() == DataType.EMAIL)
			{
				switch (emailCount)
				{
				case 0:
					i.putExtra(Insert.EMAIL, contactData.getData());
					break;
				case 1:
					i.putExtra(Insert.SECONDARY_EMAIL, contactData.getData());
					break;
				case 2:
					i.putExtra(Insert.TERTIARY_EMAIL, contactData.getData());
					break;
				default:
					break;
				}
				emailCount++;
			}
			else if (contactData.getDataType() == DataType.ADDRESS)
			{
				i.putExtra(Insert.POSTAL, contactData.getData());

			}

		}
		context.startActivity(i);
	}

	public static int getNumColumnsForGallery(Resources resources, int sizeOfImage)
	{
		return (int) (resources.getDisplayMetrics().widthPixels / sizeOfImage);
	}

	public static int getActualSizeForGallery(Resources resources, int sizeOfImage, int numColumns)
	{
		int remainder = resources.getDisplayMetrics().widthPixels - (numColumns * sizeOfImage);
		return (int) (sizeOfImage + (int) (remainder / numColumns));
	}

	public static void makeNoMediaFile(File root)
	{
		makeNoMediaFile(root, false);
	}

	/*
	 * Whenever creating a nomedia file in any dirctory and if images/videos are already present in 
	 * that directory then we need to do re-scan to make them invisible from gallery.
	 */
	public static void makeNoMediaFile(File root, boolean reScan)
	{
		if (root == null)
		{
			return;
		}

		if (!root.exists())
		{
			root.mkdirs();
		}
		File file = new File(root, ".nomedia");
		if (!file.exists())
		{
			FileOutputStream dest = null;
			try
			{
				dest = new FileOutputStream(file);
				/*
				 * File content could be blank (for backwards compatibility), or have one or more of the following values separated by a newline:
				 * image|sound|video
				 * Reference - https://code.google.com/p/android/issues/detail?id=35879
				 */
				String data = "";
				dest.write(data.getBytes(), 0, data.getBytes().length);
			}
			catch (IOException e)
			{
				Logger.d("NoMedia", "Failed to make nomedia file");
			}
			finally
			{
				try
				{
					if(dest != null)
					{
						dest.flush();
						dest.getFD().sync();
						dest.close();
					}
				}
				catch (IOException e)
				{
					Logger.d("NoMedia", "Failed to make nomedia file");
				}
			}
			if(reScan)
			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				{
					HikeMessengerApp.getInstance().getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" +
							root)));
				}
				else
				{
					HikeMessengerApp.getInstance().getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" +
							root)));
				}
			}
		}
	}

	public static Set<String> getServerRecommendedContactsSelection(String serverRecommendedArrayString, String myMsisdn)
	{
		Set<String> msisdns = new HashSet<String>();

		if (TextUtils.isEmpty(serverRecommendedArrayString))
		{
			return null;
		}
		try
		{
			JSONArray serverRecommendedArray = new JSONArray(serverRecommendedArrayString);
			if (serverRecommendedArray.length() == 0)
			{
				return null;
			}

			int i = 0;
			for (i = 0; i < serverRecommendedArray.length(); i++)
			{
				String msisdn = serverRecommendedArray.optString(i);
				if (!myMsisdn.equals(msisdn))
				{
					msisdns.add(msisdn);
				}
			}
			return msisdns;
		}
		catch (JSONException e)
		{
			return null;
		}
	}

	/*
	 * When Active Contacts >= 3 show the 'Add Friends' pop-up When Activate Contacts <3 show the 'Invite Friends' pop-up
	 */
	public static boolean shouldShowAddFriendsFTUE(int hikeContactsCount, int recommendedCount)
	{
		Logger.d("AddFriendsActivity", " hikeContactsCount=" + hikeContactsCount + " recommendedCount=" + recommendedCount);
		/*
		 * also if all the recommended contacts are your friend we should not show add friends popup
		 */
		if (recommendedCount == 0 || hikeContactsCount == 0)
		{
			return false;
		}
		if (recommendedCount > 2)
		{
			return true;
		}
		return false;
	}

	public static void startChatThread(Context context, ContactInfo contactInfo)
	{
		Intent intent = new Intent(context, ChatThread.class);
		if (contactInfo.getName() != null)
		{
			intent.putExtra(HikeConstants.Extras.NAME, contactInfo.getName());
		}
		intent.putExtra(HikeConstants.Extras.MSISDN, contactInfo.getMsisdn());
		intent.putExtra(HikeConstants.Extras.SHOW_KEYBOARD, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	public static void toggleActionBarElementsEnable(View doneBtn, ImageView arrow, TextView postText, boolean enabled)
	{
		doneBtn.setEnabled(enabled);
		arrow.setEnabled(enabled);
		postText.setEnabled(enabled);
	}

	public static Drawable getAvatarDrawableForNotificationOrShortcut(Context context, String msisdn, boolean isPin)
	{
		if(msisdn.equals(context.getString(R.string.app_name)) || msisdn.equals(HikeNotification.HIKE_STEALTH_MESSAGE_KEY))
		{
			return context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		}
		
		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);

		if (isPin || drawable == null)
		{
			Drawable background = context.getResources().getDrawable(BitmapUtils.getDefaultAvatarResourceId(msisdn, false));

			Drawable iconDrawable = null;

			if (isPin)
			{
				iconDrawable = context.getResources().getDrawable(R.drawable.ic_pin_notification);
			}
			else
			{
				iconDrawable = context.getResources().getDrawable(Utils.isGroupConversation(msisdn) ? R.drawable.ic_default_avatar_group : R.drawable.ic_default_avatar);
			}
			drawable = new LayerDrawable(new Drawable[] { background, iconDrawable });
		}
		return drawable;
	}

	public static void getRecommendedAndHikeContacts(Context context, List<ContactInfo> recommendedContacts, List<ContactInfo> hikeContacts, List<ContactInfo> friendsList)
	{
		SharedPreferences settings = (SharedPreferences) context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, "");
		friendsList.addAll(HikeMessengerApp.getContactManager().getContactsOfFavoriteType(FavoriteType.FRIEND, HikeConstants.BOTH_VALUE, msisdn, false));
		friendsList.addAll(HikeMessengerApp.getContactManager().getContactsOfFavoriteType(FavoriteType.REQUEST_SENT, HikeConstants.BOTH_VALUE, msisdn, false));
		friendsList.addAll(HikeMessengerApp.getContactManager().getContactsOfFavoriteType(FavoriteType.REQUEST_SENT_REJECTED, HikeConstants.BOTH_VALUE, msisdn, false));

		Logger.d("AddFriendsActivity", " friendsList size " + friendsList.size());
		Set<String> recommendedContactsSelection = Utils.getServerRecommendedContactsSelection(settings.getString(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null), msisdn);
		Logger.d("AddFriendsActivity", " recommendedContactsSelection " + recommendedContactsSelection);
		if (!recommendedContactsSelection.isEmpty())
		{
			recommendedContacts.addAll(HikeMessengerApp.getContactManager().getHikeContacts(-1, recommendedContactsSelection, null, msisdn));
		}

		Logger.d("AddFriendsActivity", " size recommendedContacts = " + recommendedContacts.size());

		hikeContacts.addAll(HikeMessengerApp.getContactManager().getContactsOfFavoriteType(FavoriteType.NOT_FRIEND, HikeConstants.ON_HIKE_VALUE, msisdn, false));
		hikeContacts.addAll(HikeMessengerApp.getContactManager()
				.getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED_REJECTED, HikeConstants.ON_HIKE_VALUE, msisdn, false, true));
		hikeContacts.addAll(HikeMessengerApp.getContactManager().getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED, HikeConstants.BOTH_VALUE, msisdn, false, true));
	}

	public static void addFavorite(final Context context, final ContactInfo contactInfo, final boolean isFtueContact)
	{
		toggleFavorite(context, contactInfo, isFtueContact);
		if (!contactInfo.isOnhike() || HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_ADD_FAVORITE_TIP, false))
		{
			return;
		}

		HikeDialog.showDialog(context, HikeDialog.FAVORITE_ADDED_DIALOG, new HikeDialog.HikeDialogListener()
		{

			@Override
			public void positiveClicked(Dialog dialog)
			{
				dialog.dismiss();
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_ADD_FAVORITE_TIP, true);
			}

			@Override
			public void neutralClicked(Dialog dialog)
			{
			}

			@Override
			public void negativeClicked(Dialog dialog)
			{
				dialog.dismiss();
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_ADD_FAVORITE_TIP, true);
			}

			@Override
			public void onSucess(Dialog dialog)
			{
				// TODO Auto-generated method stub

			}
		}, contactInfo.getFirstName());
	}

	private static void toggleFavorite(Context context, ContactInfo contactInfo, boolean isFtueContact)
	{
		FavoriteType favoriteType;
		if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED)
		{
			favoriteType = FavoriteType.FRIEND;
		}
		else
		{
			favoriteType = FavoriteType.REQUEST_SENT;
			Toast.makeText(context, R.string.favorite_request_sent, Toast.LENGTH_SHORT).show();
		}

		Pair<ContactInfo, FavoriteType> favoriteAdded;

		if (isFtueContact)
		{
			/*
			 * Cloning the object since we don't want to send the ftue reference.
			 */
			ContactInfo contactInfo2 = new ContactInfo(contactInfo);
			favoriteAdded = new Pair<ContactInfo, FavoriteType>(contactInfo2, favoriteType);
		}
		else
		{
			favoriteAdded = new Pair<ContactInfo, FavoriteType>(contactInfo, favoriteType);
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED, favoriteAdded);
	}

	public static void addToContacts(Activity context, String msisdn)
	{
		Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		i.putExtra(Insert.PHONE, msisdn);
		context.startActivity(i);
	}

	

	public static final void cancelScheduledStealthReset(Context context)
	{
		HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME);
	}

	public static long getOldTimestamp(int min)
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -min);
		long old = cal.getTimeInMillis();
		return old;
	};

	public static boolean isAppForeground(Context context)
	{
		ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> l = mActivityManager.getRunningAppProcesses();
		Iterator<RunningAppProcessInfo> i = l.iterator();
		while (i.hasNext())
		{
			RunningAppProcessInfo info = i.next();

			if (info.uid == context.getApplicationInfo().uid && info.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
			{
				return true;
			}
		}
		return false;
	}

	public static String replaceUrlSpaces(String fileUriString)
	{
		/*
		 * In some phones URI is received with spaces in file path we should first replace all these spaces with %20 than pass it on to URI.create() method. URI.create() method
		 * treats space as an invalid charactor in URI.
		 */
		return fileUriString.replace(" ", "%20");
	}

	/*
	 * This function is to respect old vibrate preference before vib list pref , if previous was on send, VIB Default else return VIB_OFF
	 */
	public static String getOldVibratePref(Context context)
	{
		SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
		Resources res = context.getResources();
		String vibOff = res.getString(R.string.vib_off);
		String vibDef = res.getString(R.string.vib_default);

		if (preferenceManager.getBoolean(HikeConstants.VIBRATE_PREF, true))
		{
			return vibDef;
		}
		else
		{
			return vibOff;
		}
	}

	/*
	 * This function is to respect old sound preference before sound list pref , if previous was on then check for hike jingle, else return SOUND_OFF
	 */
	public static String getOldSoundPref(Context context)
	{
		SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
		Resources res = context.getResources();
		String notifSoundOff = res.getString(R.string.notif_sound_off);
		String notifSoundDefault = res.getString(R.string.notif_sound_default);
		String notifSoundHike = res.getString(R.string.notif_sound_Hike);

		if (preferenceManager.getBoolean(HikeConstants.SOUND_PREF, true))
		{
			if (preferenceManager.getBoolean(HikeConstants.HIKE_JINGLE_PREF, true))
			{
				return notifSoundHike;
			}
			return notifSoundDefault;
		}
		else
		{
			return notifSoundOff;
		}
	}

	public static int getFreeSMSCount(Context context)
	{
		return context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, context.MODE_PRIVATE).getInt(HikeMessengerApp.SMS_SETTING, 0);
	}

	public static void handleBulkLastSeenPacket(Context context, JSONObject jsonObj) throws JSONException
	{
		/*
		 * {"t": "bls", "ts":<server timestamp>, "d": {"lastseens":{"+919818149394":<last_seen_time_in_epoch> ,"+919810335374":<last_seen_time_in_epoch>}}}
		 */
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		JSONObject lastSeens = null;
		if (data != null)
			lastSeens = data.getJSONObject(HikeConstants.BULK_LAST_SEEN_KEY);
		// Iterator<String> iterator = lastSeens.keys();

		if (lastSeens != null)
		{
			for (Iterator<String> iterator = lastSeens.keys(); iterator.hasNext();)
			{
				String msisdn = iterator.next();
				int isOffline;
				long lastSeenTime = lastSeens.getLong(msisdn);
				if (lastSeenTime > 0)
				{
					isOffline = 1;
					lastSeenTime = Utils.applyServerTimeOffset(context, lastSeenTime);
				}
				else
				{
					/*
					 * Otherwise the last seen time notifies that the user is either online or has turned the setting off.
					 */
					isOffline = (int) lastSeenTime;
					lastSeenTime = System.currentTimeMillis() / 1000;
				}
				ContactManager.getInstance().updateLastSeenTime(msisdn, lastSeenTime);
				ContactManager.getInstance().updateIsOffline(msisdn, (int) isOffline);

				HikeMessengerApp.lastSeenFriendsMap.put(msisdn, new Pair<Integer, Long>(isOffline, lastSeenTime));
			}
			HikeMessengerApp.getPubSub().publish(HikePubSub.LAST_SEEN_TIME_BULK_UPDATED, null);
		}
	}

	public static void updateLastSeenTimeInBulk(List<ContactInfo> contactList)
	{
		for (ContactInfo contactInfo : contactList)
		{
			String msisdn = contactInfo.getMsisdn();
			if (HikeMessengerApp.lastSeenFriendsMap.containsKey(msisdn))
			{
				Pair<Integer, Long> lastSeenValuePair = HikeMessengerApp.lastSeenFriendsMap.get(msisdn);

				int isOffline = lastSeenValuePair.first;

				long updatedLastSeenValue = lastSeenValuePair.second;
				long previousLastSeen = contactInfo.getLastSeenTime();

				if (updatedLastSeenValue > previousLastSeen)
				{
					contactInfo.setLastSeenTime(updatedLastSeenValue);
				}
				contactInfo.setOffline(isOffline);
			}
		}
	}

	public static boolean isListContainsMsisdn(List<ContactInfo> contacts, String msisdn)
	{
		for (ContactInfo contactInfo : contacts)
		{
			if (contactInfo.getMsisdn().equals(msisdn))
			{
				Logger.d("tesst", "matched");
				return true;
			}
		}
		return false;
	}

	/**
	 * Adding this method to compute the overall count for showing in overflow menu on home screen
	 * 
	 * @param accountPref
	 * @param defaultValue
	 * @return
	 */
	public static int updateHomeOverflowToggleCount(SharedPreferences accountPref, boolean defaultValue)
	{
		int overallCount = 0;
		if (!(accountPref.getBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, defaultValue)) && accountPref.getBoolean(HikeMessengerApp.SHOW_GAMES, false))
		{
			overallCount++;
		}
		if (!(accountPref.getBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, defaultValue)) && accountPref.getBoolean(HikeMessengerApp.SHOW_REWARDS, false))
		{
			overallCount++;
		}
		return overallCount;
	}

	public static void incrementOrDecrementFriendRequestCount(SharedPreferences accountPref, int count)
	{
		int currentCount = accountPref.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0);

		currentCount += count;
		if (currentCount >= 0)
		{
			Editor editor = accountPref.edit();
			editor.putInt(HikeMessengerApp.FRIEND_REQ_COUNT, currentCount);
			editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
			editor.commit();
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_COUNT_CHANGED, null);
	}

	public static boolean isPackageInstalled(Context context, String packageName)
	{
		PackageManager pm = context.getPackageManager();
		try
		{
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			return true;
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public static void clearJar(Context c)
	{
		HashMap<URL, JarFile> jarCache = null;
		try
		{
			Class<?> jarURLConnectionImplClass;
			if (isHoneycombOrHigher())
			{
				jarURLConnectionImplClass = Class.forName("libcore.net.url.JarURLConnectionImpl");
			}
			else
			{
				jarURLConnectionImplClass = Class.forName("org.apache.harmony.luni.internal.net.www.protocol.jar.JarURLConnectionImpl");
			}
			final Field jarCacheField = jarURLConnectionImplClass.getDeclaredField("jarCache");
			jarCacheField.setAccessible(true);
			jarCache = (HashMap<URL, JarFile>) jarCacheField.get(null);
		}
		catch (Exception e)
		{
			Logger.e("clearJar", "Exception while getting jarCacheField : " + e);
		}

		if (jarCache != null)
		{
			try
			{
				for (final Iterator<Map.Entry<URL, JarFile>> iterator = jarCache.entrySet().iterator(); iterator.hasNext();)
				{
					final Map.Entry<URL, JarFile> e = iterator.next();
					final URL url = e.getKey();
					if (url.toString().endsWith(".apk") && url.toString().contains(c.getPackageName()))
					{
						Logger.i("clearJar", "Removing static hashmap entry for " + url);
						try
						{
							final JarFile jarFile = e.getValue();
							jarFile.close();
							iterator.remove();
						}
						catch (Exception f)
						{
							Logger.e("clearJar", "Exception in removing hashmap entry for " + url, f);
						}
					}
				}
			}
			catch (Exception e)
			{
				Logger.e("clearJar", "Exception when traversing through hashmap" + e);
			}
		}
	}

	public static String combineInOneSmsString(Context context, boolean resetTimestamp, Collection<ConvMessage> convMessages, boolean isFreeHikeSms)
	{
		String combinedMessageString = "";
		int count = 0;
		for (ConvMessage convMessage : convMessages)
		{
			if (!convMessage.isSent())
			{
				break;
			}

			if (resetTimestamp && convMessage.getState().ordinal() < State.SENT_CONFIRMED.ordinal())
			{
				convMessage.setTimestamp(System.currentTimeMillis() / 1000);
			}

			combinedMessageString += Utils.getMessageDisplayText(convMessage, context);

			if (++count >= HikeConstants.MAX_FALLBACK_NATIVE_SMS)
			{
				break;
			}

			/*
			 * Added line enters among messages
			 */
			if (count != convMessages.size())
			{
				combinedMessageString += "\n\n";
			}
		}

		if (isFreeHikeSms)
		{
			combinedMessageString += "\n\n" + "- " + context.getString(R.string.sent_by_hike);
		}

		return combinedMessageString;
	}

	// @GM
	// The following methods returns the user readable size when passed the bytes in size
	public static String getSizeForDisplay(int bytes)
	{
		if (bytes <= 0)
			return ("");
		if (bytes >= 1000 * 1024 * 1024)
		{
			int gb = bytes / (1024 * 1024 * 1024);
			int gbPoint = bytes % (1024 * 1024 * 1024);
			gbPoint /= (1024 * 1024 * 1024);
			return (Integer.toString(gb) + "." + Integer.toString(gbPoint) + " GB");
		}
		else if (bytes >= (1000 * 1024))
		{
			int mb = bytes / (1024 * 1024);
			int mbPoint = bytes % (1024 * 1024);
			mbPoint /= (1024 * 102);
			return (Integer.toString(mb) + "." + Integer.toString(mbPoint) + " MB");
		}
		else if (bytes >= 1000)
		{
			int kb;
			if (bytes < 1024) // To avoid showing "1000KB"
				kb = bytes / 1000;
			else
				kb = bytes / 1024;
			return (Integer.toString(kb) + " KB");
		}
		else
			return (Integer.toString(bytes) + " B");
	}

	public static Intent getIntentForPrivacyScreen(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.privacy_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.privacy);
		return intent;
	}

	public static boolean isCompressed(byte[] bytes)
	{
		try
		{
			if ((bytes == null) || (bytes.length < 2))
			{
				return false;
			}
			else
			{
				return ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
			}
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public static byte[] uncompressByteArray(byte[] bytes) throws IOException
	{

		int DEFAULT_BUFFER_SIZE = 1024 * 4;

		if (!isCompressed(bytes))
		{
			return bytes;
		}

		ByteArrayInputStream bais = null;
		GZIPInputStream gzis = null;
		ByteArrayOutputStream baos = null;

		try
		{
			bais = new ByteArrayInputStream(bytes);
			gzis = new GZIPInputStream(bais);
			baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);

			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int n = 0;
			while (-1 != (n = gzis.read(buffer)))
			{
				baos.write(buffer, 0, n);
			}
			gzis.close();
			bais.close();

			byte[] uncompressedByteArray = baos.toByteArray();
			baos.close();

			return uncompressedByteArray;
		}
		catch (IOException ioex)
		{
			throw ioex;
		}
		finally
		{
			if (gzis != null)
			{
				gzis.close();
			}
			if (bais != null)
			{
				bais.close();
			}
			if (baos != null)
			{
				baos.close();
			}
		}
	}

	public static void emoticonClicked(Context context, int emoticonIndex, EditText composeBox)
	{
		HikeConversationsDatabase.getInstance().updateRecencyOfEmoticon(emoticonIndex, System.currentTimeMillis());
		// We don't add an emoticon if the compose box is near its maximum
		// length of characters
		if (composeBox.length() >= context.getResources().getInteger(R.integer.max_length_message) - 20)
		{
			return;
		}
		SmileyParser.getInstance().addSmiley(composeBox, emoticonIndex);
	}

	public static Animation getNotificationIndicatorAnim()
	{
		AnimationSet animSet = new AnimationSet(true);
		float a = 0.5f;
		float b = 1.15f;
		float c = 0.8f;
		float d = 1.07f;
		float e = 1f;
		int initialOffset = 0;
		float pivotX = 0.5f;
		float pivotY = 0.5f;
		Animation anim0 = new ScaleAnimation(1, a, 1, a, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim0.setInterpolator(new AccelerateInterpolator(2f));
		anim0.setStartOffset(initialOffset);
		anim0.setDuration(150);
		animSet.addAnimation(anim0);

		Animation anim1 = new ScaleAnimation(1, b / a, 1, b / a, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim1.setInterpolator(new AccelerateInterpolator(2f));
		anim1.setDuration(200);
		anim1.setStartOffset(initialOffset + anim0.getDuration());
		animSet.addAnimation(anim1);

		Animation anim2 = new ScaleAnimation(1f, c / b, 1f, c / b, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim2.setInterpolator(new AccelerateInterpolator(-1f));
		anim2.setDuration(150);
		anim2.setStartOffset(initialOffset + anim0.getDuration() + anim1.getDuration());
		animSet.addAnimation(anim2);

		Animation anim3 = new ScaleAnimation(1f, d / c, 1f, d / c, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim2.setInterpolator(new AccelerateInterpolator(1f));
		anim3.setDuration(150);
		anim3.setStartOffset(initialOffset + anim0.getDuration() + anim1.getDuration() + anim2.getDuration());
		animSet.addAnimation(anim3);

		Animation anim4 = new ScaleAnimation(1f, e / d, 1f, e / d, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim4.setInterpolator(new AccelerateInterpolator(1f));
		anim4.setDuration(150);
		anim4.setStartOffset(initialOffset + anim0.getDuration() + anim1.getDuration() + anim2.getDuration() + anim3.getDuration());
		animSet.addAnimation(anim4);

		return animSet;
	}

	public static void setupCountryCodeData(Context context, String countryCode, final EditText countryCodeEditor, final TextView countryNameEditor,
			final ArrayList<String> countriesArray, final HashMap<String, String> countriesMap, final HashMap<String, String> codesMap, final HashMap<String, String> languageMap)
	{
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().getAssets().open("countries.txt")));
			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] args = line.split(";");
				countriesArray.add(0, args[1]);
				countriesMap.put(args[1], args[2]);
				codesMap.put(args[2], args[1]);
				languageMap.put(args[0], args[1]);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		Collections.sort(countriesArray, new Comparator<String>()
		{
			@Override
			public int compare(String lhs, String rhs)
			{
				return lhs.compareTo(rhs);
			}
		});

		String prevCode = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).getString(HikeMessengerApp.TEMP_COUNTRY_CODE, "");
		if (TextUtils.isEmpty(countryCode))
		{
			TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String countryIso = TextUtils.isEmpty(prevCode) ? manager.getNetworkCountryIso().toUpperCase() : prevCode;
			String countryName = languageMap.get(countryIso);

			if (countryName == null || selectCountry(countryName, countriesMap, countriesArray, countryCode, countryCodeEditor, countryNameEditor))
			{
				selectCountry(defaultCountryName, countriesMap, countriesArray, countryCode, countryCodeEditor, countryNameEditor);
			}
		}

		countryCodeEditor.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
			{
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
			{
			}

			@Override
			public void afterTextChanged(Editable arg0)
			{
				String text = countryCodeEditor.getText().toString();
				String countryName = codesMap.get(text);
				if (countryName != null)
				{
					int index = countriesArray.indexOf(countryName);
					if (index != -1)
					{
						countryNameEditor.setText(countryName);
					}
					else
					{
						countryNameEditor.setText(R.string.wrong_country);
					}
				}
				else
				{
					countryNameEditor.setText(R.string.wrong_country);
				}
			}
		});
	}

	public static boolean selectCountry(String countryName, HashMap<String, String> countriesMap, ArrayList<String> countriesArray, String countryCode, TextView countryCodeEditor,
			TextView countryNameEditor)
	{
		int index = countriesArray.indexOf(countryName);
		if (index != -1)
		{
			countryCode = countriesMap.get(countryName);
			countryCodeEditor.setText(countryCode);
			countryNameEditor.setText(countryName);
		}
		return !TextUtils.isEmpty(countryCode);
	}

	// added for db query
	public static String getMsisdnStatement(Collection<String> msisdnList)
	{
		if (null == msisdnList)
		{
			return null;
		}
		else
		{
			if (msisdnList.isEmpty())
			{
				return null;
			}
			StringBuilder sb = new StringBuilder("(");
			for (String msisdn : msisdnList)
			{
				sb.append(DatabaseUtils.sqlEscapeString(msisdn));
				sb.append(",");
			}
			int idx = sb.lastIndexOf(",");
			if (idx >= 0)
				sb.replace(idx, sb.length(), ")");
			else
				sb.append(")");
			return sb.toString();
		}
	}

	public static void startWebViewActivity(Context context, String url, String title)
	{
		Intent intent = new Intent(context, WebViewActivity.class);
		intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, url);
		intent.putExtra(HikeConstants.Extras.TITLE, title);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	public static Drawable getChatTheme(ChatTheme chatTheme, Context context)
	{
		/*
		 * for xhdpi and above we should not scale down the chat theme nodpi asset for hdpi and below to save memory we should scale it down
		 */
		int inSampleSize = 1;
		if (!chatTheme.isTiled() && Utils.scaledDensityMultiplier < 2)
		{
			inSampleSize = 2;
		}

		Bitmap b = HikeBitmapFactory.decodeSampledBitmapFromResource(context.getResources(), chatTheme.bgResId(), inSampleSize);

		BitmapDrawable bd = HikeBitmapFactory.getBitmapDrawable(context.getResources(), b);

		Logger.d(context.getClass().getSimpleName(), "chat themes bitmap size= " + BitmapUtils.getBitmapSize(b));

		if (bd != null && chatTheme.isTiled())
		{
			bd.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		}

		return bd;
	}

	public static void resetPinUnreadCount(Conversation conv)
	{
		if (conv.getMetaData() != null)
		{
			try
			{
				conv.getMetaData().setUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN, 0);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_PIN_METADATA, conv);
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNREAD_PIN_COUNT_RESET, conv);
		}
	}	
	
	public static void handleFileForwardObject(JSONObject multiMsgFwdObject, HikeFile hikeFile) throws JSONException
	{
		multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_KEY, hikeFile.getFileKey());
		if (hikeFile.getHikeFileType() == HikeFileType.LOCATION)
		{
			multiMsgFwdObject.putOpt(HikeConstants.Extras.ZOOM_LEVEL, hikeFile.getZoomLevel());
			multiMsgFwdObject.putOpt(HikeConstants.Extras.LATITUDE, hikeFile.getLatitude());
			multiMsgFwdObject.putOpt(HikeConstants.Extras.LONGITUDE, hikeFile.getLongitude());
		}
		else if (hikeFile.getHikeFileType() == HikeFileType.CONTACT)
		{
			multiMsgFwdObject.putOpt(HikeConstants.Extras.CONTACT_METADATA, hikeFile.serialize().toString());
		}
		else
		{
			multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_PATH, hikeFile.getFilePath());
			multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_TYPE, hikeFile.getFileTypeString());
			if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
			{
				multiMsgFwdObject.putOpt(HikeConstants.Extras.RECORDING_TIME, hikeFile.getRecordingDuration());
			}
		}

	}
	
	public static String getFormattedDate(Context context, long timestamp)
	{
		Date date = new Date(timestamp * 1000);
		String format;
		if (android.text.format.DateFormat.is24HourFormat(context))
		{
			format = "d MMM ''yy";
		}
		else
		{
			format = "d MMM ''yy";
		}

		DateFormat df = new SimpleDateFormat(format);
		return df.format(date);
	}
	
	public static String getFormattedTime(boolean pretty, Context context, long timestamp)
	{
		Date date = new Date(timestamp * 1000);
		if (pretty)
		{
			PrettyTime p = new PrettyTime();
			return p.format(date);
		}
		else
		{
			String format;
			if (android.text.format.DateFormat.is24HourFormat(context))
			{
				format = "HH:mm";
			}
			else
			{
				format = "h:mm aaa";
			}

			DateFormat df = new SimpleDateFormat(format);
			return df.format(date);
		}
	}

	public static Pair<String[], String[]> getMsisdnToNameArray(Conversation conversation)
	{
		if (conversation instanceof GroupConversation)
		{
			Map<String, PairModified<GroupParticipant, String>> groupParticipants = ((GroupConversation) conversation).getGroupParticipantList();
			String[] msisdnArray = new String[groupParticipants.size()];
			String[] nameArray = new String[groupParticipants.size()];

			int i = 0;
			for (PairModified<GroupParticipant, String> groupParticipant : groupParticipants.values())
			{
				msisdnArray[i] = groupParticipant.getFirst().getContactInfo().getMsisdn();
				nameArray[i++] = groupParticipant.getSecond();
			}
			return new Pair<String[], String[]>(msisdnArray, nameArray);
		}
		return new Pair<String[], String[]>(null, null);
	}
	
	public static String formatFileSize(long size)
	{
		if (size < 1024)
		{
			return String.format("%d B", size);
		}
		else if (size < 1024 * 1024)
		{
			return String.format("%.1f KB", size / 1024.0f);
		}
		else if (size < 1024 * 1024 * 1024)
		{
			return String.format("%.1f MB", size / 1024.0f / 1024.0f);
		}
		else
		{
			return String.format("%.1f GB", size / 1024.0f / 1024.0f / 1024.0f);
		}
	}


	public static AlertDialog showNetworkUnavailableDialog(Context context)
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(R.string.no_internet_try_again);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
		return dialog;
	}
	
	public static Bitmap createBlurredImage (Bitmap originalBitmap, Context context)
	{
		final int BLUR_RADIUS = 8;
		if(hasJellyBeanMR1()){
			Bitmap output = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
	
		    RenderScript rs = RenderScript.create(context.getApplicationContext());
		    ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
		    Allocation inAlloc = Allocation.createFromBitmap(rs, originalBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_GRAPHICS_TEXTURE);
		    Allocation outAlloc = Allocation.createFromBitmap(rs, output);
		    script.setRadius(BLUR_RADIUS);
		    script.setInput(inAlloc);
		    script.forEach(outAlloc);
		    outAlloc.copyTo(output);
	
		    rs.destroy();
	
		    return output;
		}
		return null;
	}
	
	/**
	 * 
	 * @param c
	 * 	 - contact info object
	 * @param myMsisdn
	 * 	 - self msisdn
	 * @return
	 * 	<br>false if</br>
	 * 
	 * 	   <li> contact msisdn equals myMsisdn</li>
	 * 	   <li> contact favorite state is FRIENDS</li>
	 *     <li> contact favorite state is REQUEST_RECIEVED</li>
	 *     <li> contact favorite state is REQUEST_RECIEVED_REJECTED</li>
	 *     
	 *  <p>true otherwise</p>
	 */
	public static boolean shouldDeleteIcon(ContactInfo c, String myMsisdn)
	{
		String msisdn = c.getMsisdn();
		if(msisdn.equalsIgnoreCase(myMsisdn) || c.getFavoriteType().equals(FavoriteType.FRIEND) || c.getFavoriteType().equals(FavoriteType.REQUEST_RECEIVED) || c.getFavoriteType().equals(FavoriteType.REQUEST_RECEIVED_REJECTED))
		{
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("deprecation")
	public static void setClipboardText(String str, Context context)
	{
		if(isHoneycombOrHigher())
		{
			ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("", str);
			clipboard.setPrimaryClip(clip);
		}
		else
		{
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(str);
		}
	}
		
	/**
	 * This method is used to remove a contact as a favorite based on existing favorite type. It returns either FavoriteType.REQUEST_RECEIVED_REJECTED or FavoriteType.NOT_FRIEND
	 * @param contactInfo
	 */
	
	public static FavoriteType checkAndUnfriendContact(ContactInfo contactInfo)
	{
		FavoriteType favoriteType;
		if (contactInfo.getFavoriteType() == FavoriteType.FRIEND)
		{
			favoriteType = FavoriteType.REQUEST_RECEIVED_REJECTED;
		}
		else
		{
			favoriteType = FavoriteType.NOT_FRIEND;
		}
		
		Pair<ContactInfo, FavoriteType> favoriteRemoved = new Pair<ContactInfo, FavoriteType>(contactInfo, favoriteType);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED, favoriteRemoved);
		return favoriteType;
	}
	
	public static String loadJSONFromAsset(Context context, String jsonFileName)
	{
		String json = null;
		try
		{
			InputStream is = context.getAssets().open(jsonFileName + ".json");
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			json = new String(buffer, "UTF-8");

		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			return null;
		}
		return json;
	}
	
	/**
	 * Returns the device Orientation as either ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
	 * @param ctx
	 * @return ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
	 */
	public static int getDeviceOrientation(Context ctx)
	{
		return ctx.getResources().getConfiguration().orientation;
	}
	
	/**
	 * Fetches the network connection using connectivity manager
	 * @param context
	 * @return
	 * <li>-1 in case of no network</li>
	 * <li> 0 in case of unknown network</li>
	 * <li> 1 in case of wifi</li>
	 * <li> 2 in case of 2g</li>
	 * <li> 3 in case of 3g</li>
	 * <li> 4 in case of 4g</li>
	 *     
	 */
	public static short getNetworkType(Context context)
	{
		int networkType = -1;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		// Contains all the information about current connection
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info != null)
		{
			if (!info.isConnected())
				return -1;
			// If device is connected via WiFi
			if (info.getType() == ConnectivityManager.TYPE_WIFI)
				return 1; // return 1024 * 1024;
			else
				networkType = info.getSubtype();
		}

		// There are following types of mobile networks
		switch (networkType)
		{
		case TelephonyManager.NETWORK_TYPE_HSUPA: // ~ 1-23 Mbps
		case TelephonyManager.NETWORK_TYPE_LTE: // ~ 10+ Mbps // API level 11
		case TelephonyManager.NETWORK_TYPE_HSPAP: // ~ 10-20 Mbps // API level 13
		case TelephonyManager.NETWORK_TYPE_EVDO_B: // ~ 5 Mbps // API level 9
			return 4;
		case TelephonyManager.NETWORK_TYPE_EVDO_0: // ~ 400-1000 kbps
		case TelephonyManager.NETWORK_TYPE_EVDO_A: // ~ 600-1400 kbps
		case TelephonyManager.NETWORK_TYPE_HSDPA: // ~ 2-14 Mbps
		case TelephonyManager.NETWORK_TYPE_HSPA: // ~ 700-1700 kbps
		case TelephonyManager.NETWORK_TYPE_UMTS: // ~ 400-7000 kbps
		case TelephonyManager.NETWORK_TYPE_EHRPD: // ~ 1-2 Mbps // API level 11
			return 3;
		case TelephonyManager.NETWORK_TYPE_1xRTT: // ~ 50-100 kbps
		case TelephonyManager.NETWORK_TYPE_CDMA: // ~ 14-64 kbps
		case TelephonyManager.NETWORK_TYPE_EDGE: // ~ 50-100 kbps
		case TelephonyManager.NETWORK_TYPE_GPRS: // ~ 100 kbps
		case TelephonyManager.NETWORK_TYPE_IDEN: // ~25 kbps // API level 8
			return 2;
		case TelephonyManager.NETWORK_TYPE_UNKNOWN:
		default:
			return 0;
		}
	}

	public static void sendDetailsAfterSignup(Context context, boolean upgrade, boolean sendBot)
	{
		sendDeviceDetails(context, upgrade, sendBot);
		SharedPreferences accountPrefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (accountPrefs.getBoolean(HikeMessengerApp.FB_SIGNUP, false))
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FB_CLICK);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		}
		if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) > -1)
		{
			try
			{
				JSONObject metadata = new JSONObject();
				
				if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) == HikeConstants.WelcomeTutorial.STICKER_VIEWED.ordinal())
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_TUTORIAL_STICKER_VIEWED);
				}
				else if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) == HikeConstants.WelcomeTutorial.CHAT_BG_VIEWED.ordinal())
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_TUTORIAL_CBG_VIEWED);
				}
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
	
				Editor editor = accountPrefs.edit();
				editor.remove(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED);
				editor.commit();
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		}
	}

	private static void sendDeviceDetails(Context context, boolean upgrade, boolean sendBot)
	{
		recordDeviceDetails(context);
		requestAccountInfo(upgrade, sendBot);
		sendLocaleToServer(context);
	}
	
	/**
	 * @param calendar
	 * @param hour   hour value in 24 hour format eg. 2PM = 14
	 * @param minutes
	 * @param seconds
	 */
	public static long getTimeInMillis(Calendar calendar, int hour, int minutes, int seconds, int milliseconds)
	{
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minutes);
		calendar.set(Calendar.SECOND, seconds);
		calendar.set(Calendar.MILLISECOND, milliseconds);
		return calendar.getTimeInMillis();
	}
	public static void disableNetworkListner(Context context)
	{
		ComponentName mmComponentName = new ComponentName(context, ConnectionChangeReceiver.class);

		context.getPackageManager().setComponentEnabledSetting(mmComponentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

	}

	public static JSONObject getPostDeviceDetails(Context context)
	{
		String osVersion = Build.VERSION.RELEASE;
		String devType = HikeConstants.ANDROID;
		String os = HikeConstants.ANDROID;
		String deviceVersion = Build.MANUFACTURER + " " + Build.MODEL;
		String appVersion = "";
		try
		{
			appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e)
		{
			Logger.e("AccountUtils", "Unable to get app version");
		}

		TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String deviceKey = manager.getDeviceId();

		JSONObject data = new JSONObject();
		try
		{
			data.put(HikeConstants.DEV_TYPE, devType);
			data.put(HikeConstants.APP_VERSION, appVersion);
			data.put(HikeConstants.LogEvent.OS, os);
			data.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
			data.put(HikeConstants.DEVICE_VERSION, deviceVersion);
			data.put(HikeConstants.DEVICE_KEY, deviceKey);
			Utils.addCommonDeviceDetails(data, context);
		}
		catch (JSONException e)
		{
			Logger.e("Exception", "Invalid JSON", e);
		}
		return data;
	}

	/**
	 * Checks if is user signed up. Works with application context.
	 * 
	 * @return true, if is user signed up
	 */
	public static boolean requireAuth(Context appContext, boolean allowOpeningActivity)
	{
		appContext = appContext.getApplicationContext();

		HikeSharedPreferenceUtil settingPref = HikeSharedPreferenceUtil.getInstance();

		if (!settingPref.getData(HikeMessengerApp.ACCEPT_TERMS, false))
		{
			if (allowOpeningActivity)
			{
				IntentManager.openWelcomeActivity(appContext);
			}
			return false;
		}

		if (settingPref.getData(HikeMessengerApp.NAME_SETTING, null) == null)
		{
			if (allowOpeningActivity)
			{
				IntentManager.openSignupActivity(appContext);
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Tells if User is on Telephonic/Audio/Vedio/Voip Call
	 * @param context
	 * @return
	 */
	public static boolean isUserInAnyTypeOfCall(Context context) {
		
		AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
		boolean callMode = manager.getMode() == AudioManager.MODE_IN_COMMUNICATION 
				|| manager.getMode() == AudioManager.MODE_IN_CALL;
        
		return callMode;
    } 
	 /** Fetches the network connection using connectivity manager
	 * @param context
	 * @return
	 * <li>-1 in case of no network</li>
	 * <li> 0 in case of unknown network</li>
	 * <li> 1 in case of wifi</li>
	 * <li> 2 in case of 2g</li>
	 * <li> 3 in case of 3g</li>
	 * <li> 4 in case of 4g</li>
	 *     
	 */
	public static String getNetworkTypeAsString(Context context)
	{
		String networkType = "";
		switch (getNetworkType(context))
		{
		case -1:
			networkType = "off";
			break;

		case 0:
			networkType = "unknown";
			break;

		case 1:
			networkType = "wifi";
			break;

		case 2:
			networkType = "2g";
			break;

		case 3:
			networkType = "3g";
			break;

		case 4:
			networkType = "4g";
			break;

		default:
			break;
		}
		return networkType;
	}

	public static String conversationType(String msisdn)
	{
		if (isBot(msisdn))
		{
			return HikeConstants.BOT;
		}
		else if (isGroupConversation(msisdn))
		{
			return HikeConstants.GROUP_CONVERSATION;
		}
		else
		{
			return HikeConstants.ONE_TO_ONE_CONVERSATION;
		}
	}

	public static boolean isBot(String msisdn)
	{
		if (HikeMessengerApp.hikeBotNamesMap != null)
		{
			return HikeMessengerApp.hikeBotNamesMap.containsKey(msisdn);
		}
		else
		{
			//Not probable
			return false;
		}
	}
	/**
	 * Returns Data Consumed in KB
	 * @param appId
	 * @return
	 */
	public static long getTotalDataConsumed(int appId)
	{
		long received = TrafficStats.getUidRxBytes(appId);  //In KB
        
		long sent = TrafficStats.getUidTxBytes(appId);   //In KB
        
		if(received != TrafficStats.UNSUPPORTED
				&& sent != TrafficStats.UNSUPPORTED)
		{
	        return received/(1024) + sent/(1024);
		}
		else
		{
			return TrafficsStatsFile.getTotalBytesManual(appId);  //In KB
		}
	}
	
	public static Bitmap viewToBitmap(View view) {
		try
		{
	    Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
	    Canvas canvas = new Canvas(bitmap);
	    view.draw(canvas);
	    return bitmap;
		}catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static Bitmap undrawnViewToBitmap(View view) {
		int measuredWidth = View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.UNSPECIFIED);
		int measuredHeight = View.MeasureSpec.makeMeasureSpec(view.getHeight(), View.MeasureSpec.UNSPECIFIED);

		// Cause the view to re-layout
		view.measure(measuredWidth, measuredHeight);
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
	    return viewToBitmap(view);
	}
	
	public static boolean isConversationMuted(String msisdn)
	{
		if ((Utils.isGroupConversation(msisdn)))
		{
			if (HikeConversationsDatabase.getInstance().isGroupMuted(msisdn))
			{
				return true;
			}
		}
		else if (Utils.isBot(msisdn))
		{
			if (HikeConversationsDatabase.getInstance().isBotMuted(msisdn))
			{
				return true;
			}
		}
		return false;
	}
}
