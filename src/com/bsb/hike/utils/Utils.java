package com.bsb.hike.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
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
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.RawContacts;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.BuildConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.TrafficsStatsFile;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.localisation.LocalLanguage;
import com.bsb.hike.localisation.LocalLanguageUtils;
import com.bsb.hike.models.AccountData;
import com.bsb.hike.models.AccountInfo;
import com.bsb.hike.models.Birthday;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ContactInfoData.DataType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.models.Conversation.OneToNConvInfo;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.service.ConnectionChangeReceiver;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.tasks.CheckForUpdateTask;
import com.bsb.hike.tasks.StatusUpdateTask;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.timeline.view.TimelineActivity;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.ui.WebViewActivity;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.userlogs.AESEncryption;
import com.bsb.hike.voip.VoIPUtils;
import com.google.android.gms.maps.model.LatLng;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    // Precision points definition for duration logging========================================[[
    public static final class ExecutionDurationLogger {
        public static final String TAG = ExecutionDurationLogger.class.getSimpleName();

        public static final int PRECISION_UNIT_SECOND = 0;

        public static final int PRECISION_UNIT_MILLI_SECOND = 3;

        public static final int PRECISION_UNIT_MICRO_SECOND = 6;

        public static final int PRECISION_UNIT_NANO_SECOND = 9;

        public static final String sec = " s";

        public static final String ms = " ms";

        public static final String μs = " μs";

        public static final String ns = " ns";

        public static final String DELIMITER = ", ";
    }

    // ========================================Precision points definition for duration logging]]

    public static Pattern shortCodeRegex;

    public static Pattern msisdnRegex;

    public static Pattern pinRegex;

    public static float scaledDensityMultiplier = 1.0f;

    public static float densityMultiplier = 1.0f;

    public static int densityDpi;

    public static int displayWidthPixels;

    public static int displayHeightPixels;

    private static final String defaultCountryName = "India";

    /**
     * copied from {@link android.telephony.TelephonyManager}
     */
    private static final int NETWORK_TYPE_GSM = 16;

    private final static String ANDROID_DATA_STORAGE_DIR_SUFFIX = "/Android/data/";

    static {
        shortCodeRegex = Pattern.compile("\\*\\d{3,10}#");
        msisdnRegex = Pattern.compile("\\[(\\+\\d*)\\]");
        pinRegex = Pattern.compile("\\d{4,6}");
    }

    public static String join(Collection<?> s, String delimiter, String startWith, String endWith) {
        StringBuilder builder = new StringBuilder();
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            if (!TextUtils.isEmpty(startWith)) {
                builder.append(startWith);
            }
            builder.append(iter.next());
            if (!TextUtils.isEmpty(endWith)) {
                builder.append(endWith);
            }
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        Logger.d("Utils", "Joined string is: " + builder.toString());
        return builder.toString();
    }


    public static boolean isIndianMobileNumber(String number) {
        //13 is the number of chars in the phone msisdn
        if (number != null && (number.startsWith("+919") || number.startsWith("+918") || number.startsWith("+917")) && number.length() == 13) {
            return true;
        }
        return false;
    }

    public static boolean isIndianNumber(String number) {
        //13 is the number of chars in the phone msisdn
        if (number != null && number.startsWith("+91")) {
            return true;
        }
        return false;
    }


    static final private int ANIMATION_DURATION = 400;

    public static long gettingMidnightTimeinMilliseconds() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }


    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile(HikeFileType type, String orgFileName, boolean isSent) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        String path = getFileParent(type, isSent);
        if (path == null) {
            return null;
        }

        File mediaStorageDir = new File(path);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Logger.d("Hike", "failed to create directory");
                return null;
            }
        }

        if (!mediaStorageDir.isDirectory() && mediaStorageDir.canWrite()) {
            mediaStorageDir.delete();
            mediaStorageDir.mkdirs();
        }
        // File name should only be blank in case of profile images or while
        // capturing new media.
        if (TextUtils.isEmpty(orgFileName)) {
            orgFileName = getUniqueFilename(type);
        }

        // String fileName = getUniqueFileName(orgFileName, fileKey);

        return new File(mediaStorageDir, orgFileName);
    }

    public static void setSharedPrefValue(Context context, String key, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Editor prefEditor = prefs.edit();
        prefEditor.putBoolean(key, value);
        prefEditor.commit();
    }

    public static String getUniqueFilename(HikeFileType type) {
        // Create a media file name
        // String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS")
        // .format(new Date());
        String timeStamp = Long.toString(System.currentTimeMillis());
        /*
		 * We don't create files for type LOCATION and CONTACT. So file name should be empty string instead of null to avoid NullPointerException on file creation.
		 */
        String orgFileName = "";

        switch (type) {
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

        return orgFileName;
    }

    public static File createNewFile(HikeFileType type, String prefix) {
        return createNewFile(type, prefix, false);
    }

    public static File createNewFile(HikeFileType type, String prefix, boolean isSent) {
        File selectedDir = new File(Utils.getFileParent(type, isSent));
        if (!selectedDir.exists()) {
            if (!selectedDir.mkdirs()) {
                return null;
            }
        }
        String fileName = prefix + Utils.getUniqueFilename(type);
        File selectedFile = new File(selectedDir.getPath() + File.separator + fileName);
        return selectedFile;
    }

    public static String getFinalFileName(HikeFileType type) {
        return getFinalFileName(type, null);
    }

    public static String getFinalFileName(HikeFileType type, String orgName) {
        StringBuilder orgFileName = new StringBuilder();
        // String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS")
        // .format(new Date());
        String timeStamp = Long.toString(System.currentTimeMillis());
        if (TextUtils.isEmpty(orgName)) {
            switch (type) {
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
                    break;
                case APK:
                    orgFileName.append("APK_" + timeStamp + ".apk");
                    break;
            }
        } else {
            int lastDotIndex = orgName.lastIndexOf(".");

            String actualName;
            String extension = getFileExtension(orgName);

            if (lastDotIndex != -1 && lastDotIndex != orgName.length() - 1) {
                actualName = new String(orgName.substring(0, lastDotIndex));
            } else {
                actualName = orgName;
            }

            orgFileName.append(actualName + "_" + timeStamp);

            if (!TextUtils.isEmpty(extension)) {
                orgFileName.append("." + extension);
            }
        }
        return orgFileName.toString();
    }

    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");

        String extension = "";

        if (lastDotIndex != -1 && lastDotIndex != fileName.length() - 1) {
            extension = new String(fileName.substring(lastDotIndex + 1));
        }

        return extension.toLowerCase();
    }

    public static String getFileParent(HikeFileType type, boolean isSent) {
        StringBuilder path = new StringBuilder(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT);
        switch (type) {
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
        if (isSent) {
            path.append(HikeConstants.SENT_ROOT);
        }
        return path.toString();
    }

    public static void savedAccountCredentials(AccountInfo accountInfo, SharedPreferences.Editor editor) {
        AccountUtils.setToken(accountInfo.getToken());
        AccountUtils.setUID(accountInfo.getUid());
        editor.putString(HikeMessengerApp.MSISDN_SETTING, accountInfo.getMsisdn());
        editor.putString(HikeMessengerApp.TOKEN_SETTING, accountInfo.getToken());
        editor.putString(HikeMessengerApp.UID_SETTING, accountInfo.getUid());
        editor.putString(HikeMessengerApp.BACKUP_TOKEN_SETTING, accountInfo.getBackUpToken());
        editor.putInt(HikeMessengerApp.SMS_SETTING, accountInfo.getSmsCredits());
        editor.putInt(HikeMessengerApp.INVITED, accountInfo.getAllInvitee());
        editor.putInt(HikeMessengerApp.INVITED_JOINED, accountInfo.getAllInviteeJoined());
        editor.putString(HikeMessengerApp.COUNTRY_CODE, accountInfo.getCountryCode());
        editor.putString(HikeConstants.SERVER_NAME_SETTING, accountInfo.getServerName());
        editor.putString(HikeConstants.SERVER_GENDER_SETTING, accountInfo.getServerGender());

        Birthday serverDOB = accountInfo.getServerDOB();
        if (serverDOB != null) ;
        {
            editor.putInt(HikeConstants.SERVER_BIRTHDAY_DAY, serverDOB.day);
            editor.putInt(HikeConstants.SERVER_BIRTHDAY_MONTH, serverDOB.month);
            editor.putInt(HikeConstants.SERVER_BIRTHDAY_YEAR, serverDOB.year);
        }
        editor.commit();

		/*
		 * Just after pin validation we need to set self msisdn field in ContactManager
		 */
        ContactManager.getInstance().setSelfMsisdn(accountInfo.getMsisdn());
    }

    /*
	 * Extract a pin code from a specially formatted message to the application.
	 *
	 * @return null iff the message isn't an SMS pincode, otherwise return the pincode
	 */
    public static String getSMSPinCode(String body) {
        Matcher m = pinRegex.matcher(body);
        return m.find() ? m.group() : null;
    }

    public static boolean requireAuth(Activity activity) {
        SharedPreferences settings = activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

        if (!settings.getBoolean(HikeMessengerApp.ACCEPT_TERMS, false)) {
            activity.startActivity(new Intent(activity, WelcomeActivity.class));
            activity.finish();
            return true;
        }

        if (settings.getString(HikeMessengerApp.NAME_SETTING, null) == null) {
            IntentFactory.reopenSignupActivity(activity);
            activity.finish();
            return true;
        }

        if (!settings.getBoolean(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, false) || !settings.getBoolean(HikeMessengerApp.SIGNUP_COMPLETE, false)) {
            if (isUserUpgrading(activity)) {
                Editor editor = settings.edit();
                editor.putBoolean(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, true);
                editor.putBoolean(HikeMessengerApp.SIGNUP_COMPLETE, true);
                editor.commit();
                return false;
            } else {
                IntentFactory.reopenSignupActivity(activity);
                activity.finish();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the user has successfully signedup. This means user is has passed signuptask. Returns false otherwise. In this case it will open either SignupActivity or
     * WelcomeActivity.
     *
     * @param context
     * @param launchSignup -- true if you want to launch respective activity, false otherwise
     * @return
     */
    public static boolean isUserSignedUp(Context context, boolean launchSignup) {
        HikeSharedPreferenceUtil settingPref = HikeSharedPreferenceUtil.getInstance();
        if (!settingPref.getData(HikeMessengerApp.ACCEPT_TERMS, false)) {
            if (launchSignup) {
                Intent i = new Intent(context, WelcomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
            return false;
        }

        if (settingPref.getData(HikeMessengerApp.NAME_SETTING, null) == null) {
            if (launchSignup) {
                Intent i = new Intent(context, SignupActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
            return false;
        }

        if (!settingPref.getData(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, false) || !settingPref.getData(HikeMessengerApp.SIGNUP_COMPLETE, false)) {
            if (isUserUpgrading(context)) {
                settingPref.saveData(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, true);
                settingPref.saveData(HikeMessengerApp.SIGNUP_COMPLETE, true);
                return true;
            } else {
                if (launchSignup) {
                    Intent i = new Intent(context, SignupActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                }
                return false;
            }
        }
        return true;
    }

    private static boolean isUserUpgrading(Context context) {
        SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
        String currentAppVersion = settings.getString(HikeMessengerApp.CURRENT_APP_VERSION, "");
        String actualAppVersion = "";
        try {
            actualAppVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            Logger.e("Utils", "Unable to get the app version");
        }
        if (!currentAppVersion.equals("") && !currentAppVersion.equals(actualAppVersion)) {
            return true;
        }
        return false;
    }

    public static boolean isValidEmail(Editable text) {
        return (!TextUtils.isEmpty(text) && android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches());
    }

    public static void logEvent(Context context, String event) {
        logEvent(context, event, 1);
    }

    /**
     * Used for logging the UI based events from the clients side.
     *
     * @param context
     * @param event   : The event which is to be logged.
     * @param time    : This is only used to signify the time the user was on a screen for. For cases where this is not relevant we send 0.s
     */
    public static void logEvent(Context context, String event, long increment) {
        SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ANALYTICS, 0);

        long currentVal = prefs.getLong(event, 0) + increment;

        Editor editor = prefs.edit();
        editor.putLong(event, currentVal);
        editor.commit();
    }

    public static boolean validateBotMsisdn(String msisdn) {
        if (TextUtils.isEmpty(msisdn)) {
            Logger.wtf(HikePlatformConstants.TAG, "msisdn is ---->" + msisdn);
            return false;
        }
        if (!msisdn.startsWith("+")) {
            Logger.wtf(HikePlatformConstants.TAG, "msisdn does not start with +. It is ---->" + msisdn);
            return false;
        }
        return true;
    }

    public static String getConversationJoinHighlightText(JSONArray participantInfoArray, OneToNConvInfo convInfo, boolean newGrp, Context context) {
        if (newGrp) {
            return context.getString(R.string.you);
        }
        JSONObject participant = (JSONObject) participantInfoArray.opt(0);
        String highlight = convInfo.getConvParticipantName(participant.optString(HikeConstants.MSISDN));
        if (participantInfoArray.length() == 2) {
            JSONObject participant2 = (JSONObject) participantInfoArray.opt(1);
            String name2 = convInfo.getConvParticipantName(participant2.optString(HikeConstants.MSISDN));

            highlight += " " + context.getString(R.string.and) + " " + name2;
        } else if (participantInfoArray.length() > 2) {
            highlight += " " + context.getString(R.string.and) + " " + (participantInfoArray.length() - 1) + " " + context.getString(R.string.others_smallcase);
        }
        return highlight;
    }

    public static String getOneToNConversationJoinHighlightText(JSONArray participantInfoArray, OneToNConversation conversation, boolean newGrp, Context context) {
        if (newGrp) {
            return context.getString(R.string.you);
        }
        JSONObject participant = (JSONObject) participantInfoArray.opt(0);
        String highlight = conversation.getConvParticipantFirstNameAndSurname(participant.optString(HikeConstants.MSISDN));

        if (participantInfoArray.length() == 2) {
            JSONObject participant2 = (JSONObject) participantInfoArray.opt(1);
            String name2 = conversation.getConvParticipantFirstNameAndSurname(participant2.optString(HikeConstants.MSISDN));

            highlight += " " + context.getString(R.string.and) + " " + name2;
        } else if (participantInfoArray.length() > 2) {
            highlight += " " + context.getString(R.string.and) + " " + (participantInfoArray.length() - 1) + " " + context.getString(R.string.others_smallcase);
        }
        return highlight;
    }

    public static String getDeviceId(Context context) {
        String deviceId = null;
        try {
            deviceId = getHashedDeviceId(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return deviceId;
    }

    public static void recordDeviceDetails(Context context) {
        try {
            JSONObject metadata = new JSONObject();

            int height;
            int width;

			/*
			 * Doing this to avoid the ClassCastException when the context is sent from the BroadcastReceiver. As it is, we don't need to send the resolution from the
			 * BroadcastReceiver since it should have already been sent to the server.
			 */
            if (context instanceof Activity) {
                height = ((Activity) context).getWindowManager().getDefaultDisplay().getHeight();
                width = ((Activity) context).getWindowManager().getDefaultDisplay().getWidth();
                String resolution = height + "x" + width;
                metadata.put(HikeConstants.LogEvent.RESOLUTION, resolution);
            }
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            String osVersion = Build.VERSION.RELEASE;
            String os = "Android";
            String carrier = manager.getNetworkOperatorName();
            String device = Build.MANUFACTURER + " " + Build.MODEL;
            String appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

            Map<String, String> referralValues = retrieveReferralParams(context);
            if (!referralValues.isEmpty()) {
                for (Entry<String, String> entry : referralValues.entrySet()) {
                    metadata.put(entry.getKey(), entry.getValue());
                }
            }
            metadata.put(HikeConstants.LogEvent.DEVICE_ID, getDeviceId(context));
            metadata.put(HikeConstants.LogEvent.OS, os);
            metadata.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
            metadata.put(HikeConstants.LogEvent.DEVICE, device);
            metadata.put(HikeConstants.LogEvent.CARRIER, carrier);
            metadata.put(HikeConstants.LogEvent.APP_VERSION, appVersion);
            HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DEVICE_DETAILS, metadata, AnalyticsConstants.EVENT_TAG_CBS);
        } catch (JSONException e) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        } catch (NameNotFoundException e) {
            Logger.e("Utils", "Package not found", e);
        }
    }

    public static void getDeviceStats(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ANALYTICS, 0);
        Editor editor = prefs.edit();
        Map<String, ?> keys = prefs.getAll();

        JSONObject metadata = new JSONObject();

        try {
            if (!keys.isEmpty()) {
                for (String key : keys.keySet()) {
                    Logger.d("Utils", "Getting keys: " + key);
                    metadata.put(key, prefs.getLong(key, 0));
                    editor.remove(key);
                }
                editor.commit();
                HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DEVICE_STATS, metadata);
            }
        } catch (JSONException e) {
            Logger.e("Utils", "Invalid JSON", e);
        }
    }

    public static CharSequence addContactName(String firstName, CharSequence message) {
        SpannableStringBuilder messageWithName = new SpannableStringBuilder(firstName + HikeConstants.SEPARATOR + message);
        messageWithName.setSpan(new StyleSpan(Typeface.BOLD), 0, firstName.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return messageWithName;
    }

    /**
     * Used for setting the density multiplier, which is to be multiplied with any pixel value that is programmatically given
     *
     * @param activity
     */
    public static void setDensityMultiplier(DisplayMetrics displayMetrics) {
        Utils.scaledDensityMultiplier = displayMetrics.scaledDensity;
        Utils.densityDpi = displayMetrics.densityDpi;
        Utils.densityMultiplier = displayMetrics.density;
        Utils.displayWidthPixels = displayMetrics.widthPixels;
        Utils.displayHeightPixels = displayMetrics.heightPixels;
    }

    //AND-4036, Extending the high light to the next space.
    //Problem: only "aap" is getting highlighted in the "aapko" / "aapne" since the highlighted text is "aap"
    //Sol: making the highlight text extend to the end of the word.
    public static CharSequence getFormattedParticipantInfo(String info, String textToHighlight) {
        if (!info.contains(textToHighlight) || TextUtils.isEmpty(textToHighlight))
            return info;

        SpannableStringBuilder ssb = new SpannableStringBuilder(info);
        int index = info.indexOf(textToHighlight);

        int wordEndIndex = info.substring(index + textToHighlight.length()).indexOf(" ");
        int highlightLen = (wordEndIndex == -1) ? textToHighlight.length() : textToHighlight.length() + wordEndIndex;
        ssb.setSpan(new StyleSpan(Typeface.BOLD), info.indexOf(textToHighlight), info.indexOf(textToHighlight) + highlightLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return ssb;
    }


    /**
     * Used for preventing the cursor from being shown initially on the text box in touch screen devices. On touching the text box the cursor becomes visible
     *
     * @param editText
     */
    public static void hideCursor(final EditText editText, Resources resources) {
        if (resources.getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS || resources.getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            editText.setCursorVisible(false);
            editText.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        editText.setCursorVisible(true);
                    }
                    return false;
                }
            });
        }
    }

    /**
     * DEPRECATED. Use {@link #getUserContactInfo(boolean) getUserContactInfo(showNameAsYou)}
     */
    public static ContactInfo getUserContactInfo(SharedPreferences prefs) {
        return getUserContactInfo(prefs, false);
    }

    /**
     * DEPRECATED. Use {@link #getUserContactInfo(boolean) getUserContactInfo(showNameAsYou)}
     */
    public static ContactInfo getUserContactInfo(SharedPreferences prefs, boolean showNameAsYou) {

        String myMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, null);

        long userJoinTime = prefs.getLong(HikeMessengerApp.USER_JOIN_TIME, 0);

        String myName;
        if (showNameAsYou) {
            myName = "You";
        } else {
            myName = prefs.getString(HikeMessengerApp.NAME_SETTING, null);
        }

        ContactInfo contactInfo = new ContactInfo(myName, myMsisdn, myName, myMsisdn, true);
        contactInfo.setHikeJoinTime(userJoinTime);

        return contactInfo;
    }

    public static ContactInfo getUserContactInfo(boolean showNameAsYou) {
        HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance();
        String myMsisdn = prefs.getData(HikeMessengerApp.MSISDN_SETTING, null);
        long userJoinTime = prefs.getData(HikeMessengerApp.USER_JOIN_TIME, 0L);

        String myName;
        if (showNameAsYou) {
            myName = "You";
        } else {
            myName = prefs.getData(HikeMessengerApp.NAME_SETTING, null);
        }

        ContactInfo contactInfo = new ContactInfo(myName, myMsisdn, myName, myMsisdn, true);
        contactInfo.setHikeJoinTime(userJoinTime);

        return contactInfo;
    }

    public static boolean isVersionNameHigher(String oldVersion, String newVersion, Context context) throws NumberFormatException {
        StringTokenizer updateVersion = new StringTokenizer(newVersion, ".");
        StringTokenizer currentVersion = new StringTokenizer(oldVersion, ".");
        while (currentVersion.hasMoreTokens()) {
            if (!updateVersion.hasMoreTokens()) {
                return false;
            }
            int currentVersionToken = Integer.parseInt(currentVersion.nextToken());
            int updateVersionToken = Integer.parseInt(updateVersion.nextToken());
            if (updateVersionToken > currentVersionToken) {
                return true;
            } else if (updateVersionToken < currentVersionToken) {
                return false;
            }

        }
        while (updateVersion.hasMoreTokens()) {
            if (Integer.parseInt(updateVersion.nextToken()) > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUpdateRequired(String version, Context context) {
        try {
            String appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            Logger.d("AUTOAPK", "self version : " + appVersion);
            return isVersionNameHigher(appVersion, version, context);
        } catch (NumberFormatException nfe) {
            Logger.e("AUTOAPK", "version name contains strings possibly", nfe);
            return false;
        } catch (NameNotFoundException e) {
            Logger.e("AUTOAPK", "Package not found...", e);
            return false;
        }

    }

    /*
	 * Stores the referral parameters in the app's sharedPreferences.
	 */
    public static void storeReferralParams(Context context, List<NameValuePair> params) {
        SharedPreferences storage = context.getSharedPreferences(HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = storage.edit();

        for (NameValuePair nameValuePair : params) {
            String name = nameValuePair.getName();
            String value = nameValuePair.getValue();
            editor.putString(name, value);
        }

        editor.commit();
    }

    /*
	 * Returns a map with the Market Referral parameters pulled from the sharedPreferences.
	 */
    public static Map<String, String> retrieveReferralParams(Context context) {
        Map<String, String> params = new HashMap<String, String>();
        SharedPreferences storage = context.getSharedPreferences(HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE);

        for (String key : storage.getAll().keySet()) {
            String value = storage.getString(key, null);
            if (value != null) {
                params.put(key, value);
            }
        }
        // We don't need these values anymore
        Editor editor = storage.edit();
        editor.clear();
        editor.commit();
        return params;
    }

    public static boolean isUserOnline(Context context) {
        return getNetInfoFromConnectivityManager().second;
    }

    /**
     * Requests the server to send an account info packet
     */
    public static void requestAccountInfo(boolean upgrade, boolean sendbot) {
        Logger.d("Utils", "Requesting account info");
        JSONObject requestAccountInfo = new JSONObject();
        try {
            requestAccountInfo.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.REQUEST_ACCOUNT_INFO);

            JSONObject data = new JSONObject();
            data.put(HikeConstants.UPGRADE, upgrade);
            data.put(HikeConstants.SENDBOT, sendbot);
            data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis() / 1000));
            data.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
            data.put(HikeConstants.NEW_LAST_SEEN_SETTING, true);
            requestAccountInfo.put(HikeConstants.DATA, data);
            HikeMqttManagerNew.getInstance().sendMessage(requestAccountInfo, MqttConstants.MQTT_QOS_ONE);
        } catch (JSONException e) {
            Logger.e("Utils", "Invalid JSON", e);
        }
    }

    public static String ellipsizeName(String name) {
        return name.length() <= HikeConstants.MAX_CHAR_IN_NAME ? name : (name.substring(0, HikeConstants.MAX_CHAR_IN_NAME - 3) + "...");
    }

    public static String getInviteMessage(Context context, int messageResId) {
        String inviteToken = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeConstants.INVITE_TOKEN, "");
        inviteToken = "";
        // Adding the user's invite token to the invite url
        String inviteMessage = context.getString(messageResId, inviteToken);

        return inviteMessage;
    }

    public static void startShareIntent(Context context, String message) {
        Intent s = new Intent(android.content.Intent.ACTION_SEND);
        s.setType("text/plain");
        s.putExtra(Intent.EXTRA_TEXT, message);
        context.startActivity(s);
    }

    public static String drawableToString(Drawable ic) {
        if (ic != null) {
            Bitmap bitmap = ((BitmapDrawable) ic).getBitmap();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(CompressFormat.PNG, 100, outputStream);
            byte[] bitmapByte = outputStream.toByteArray();
            return Base64.encodeToString(bitmapByte, Base64.DEFAULT);
        }
        return null;
    }

    public static Bitmap getRotatedBitmap(String path, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        Bitmap rotatedBitmap = null;
        Matrix m = new Matrix();
        ExifInterface exif = null;
        int orientation = 1;

        try {
            if (path != null) {
                // Getting Exif information of the file
                exif = new ExifInterface(path);
            }
            if (exif != null) {
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        m.preRotate(270);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_90:
                        m.preRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        m.preRotate(180);
                        break;
                    default:
                        return bitmap;
                }
                // Rotates the image according to the orientation
                rotatedBitmap = HikeBitmapFactory.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rotatedBitmap;
    }

    public static byte[] bitmapToBytes(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bitmap.compress(format, quality, bao);
        return bao.toByteArray();
    }

    // If source is local file path then previous getRealPathFromUri implementation (which uses deprecated manage query) provides null, So adding this implementation to solve the
    // issue.
    public static String getRealPathFromUri(Uri uri, Context mContext) {
        String result = null;
        Cursor cursor = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        try {
            cursor = mContext.getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null) {
                result = uri.getPath();
            } else {
                if (cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    if (idx >= 0) {
                        result = cursor.getString(idx);
                    }
                } else {
                    result = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }

        try {
            if (result == null && isKitkatOrHigher() && DocumentsContract.isDocumentUri(mContext, uri)) {
                result = getPathFromDocumentedUri(uri, mContext);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Wrapper Method that covers all the known edge cases while retrieving filepath from Uri
     *
     * @param uri
     * @param mContext
     * @param checkForPicassaUri : boolena for special handling of Picassa Uri
     * @return absolute file path othe file represented by the uri
     */

    public static String getAbsolutePathFromUri(Uri uri, Context mContext, boolean checkForPicassaUri) {
        return getAbsolutePathFromUri(uri, mContext, checkForPicassaUri, true);

    }

    public static String getAbsolutePathFromUri(Uri uri, Context mContext, boolean checkForPicassaUri, boolean showToasts) {
        if (uri == null) {
            if (showToasts)
                Toast.makeText(mContext, R.string.unknown_file_error, Toast.LENGTH_SHORT).show();
            return null;
        }
        String fileUriString = uri.toString();
        String fileUriStart = "file:";

        String returnFilePath = null;
        if (fileUriString.startsWith(fileUriStart)) {
            File selectedFile = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
			/*
			 * Done to fix the issue in a few Sony devices.
			 */
            returnFilePath = selectedFile.getAbsolutePath();
        }

        if (returnFilePath == null) {
            returnFilePath = getRealPathFromUri(uri, mContext);
        }

        if (returnFilePath == null && checkForPicassaUri && isPicasaUri(fileUriString)) {
            File file = null;
            try {
                file = getCloudFile(mContext, uri);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SecurityException er) {
                er.printStackTrace();
            }
            if (file != null) {
                return file.getAbsolutePath();
            } else {
                if (showToasts)
                    Toast.makeText(mContext, R.string.cloud_file_error, Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        if (returnFilePath == null && showToasts)
            Toast.makeText(mContext, R.string.unknown_file_error, Toast.LENGTH_SHORT).show();
        return returnFilePath;

    }

    public static enum ExternalStorageState {
        WRITEABLE, READ_ONLY, NONE
    }

    public static ExternalStorageState getExternalStorageState() {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            return ExternalStorageState.WRITEABLE;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            return ExternalStorageState.READ_ONLY;
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need
            // to know is we can neither read nor write
            return ExternalStorageState.NONE;
        }
    }

    public static String getFirstName(String name) {
        return name.trim().split(" ", 2)[0];
    }

    public static String getFirstNameAndSurname(String name) {
		/*
		 * String fullname = name.trim().split(" ", 2)[0]; if(name.contains(" ")) { int spaceIndex = name.indexOf(" "); fullname.concat(" " + name.charAt(spaceIndex + 1)); }
		 */
        return name;
    }

    public static double getFreeSpace() {
        double sdAvailSize = 0.0;
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            if (isJELLY_BEAN_MR2OrHigher()) {
                sdAvailSize = (double) stat.getAvailableBlocksLong() * (double) stat.getBlockSizeLong();
            } else {
                sdAvailSize = (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
            }
            Logger.d("StickerSize", "get available blocks : " + (double) stat.getAvailableBlocks() + "  get block size : " + (double) stat.getBlockSize());

        } catch (IllegalArgumentException e) // http://stackoverflow.com/questions/23516075/invalid-path-error-get-the-external-memory-size
        {
            //returning sufficient amount of size so that download is executed
            sdAvailSize = 15 * 1024 * 1024;
        }

        return sdAvailSize;
    }

    public static boolean compressAndCopyImage(String srcFilePath, String destFilePath, Context context) {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        int imageQuality = appPrefs.getInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_DEFAULT);
        int imageCompressQuality = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SERVER_CONFIG_DEFAULT_IMAGE_SAVE_QUALITY,
                HikeConstants.HikePhotos.DEFAULT_IMAGE_SAVE_QUALITY);
        return compressAndCopyImage(srcFilePath, destFilePath, context, Bitmap.Config.ARGB_8888, imageCompressQuality, imageQuality, true);
    }

    public static boolean compressAndCopyImage(String srcFilePath, String destFilePath, Context context, Bitmap.Config config, int quality, int imageQuality,
                                               boolean toUserServerConfig) {
        InputStream src = null;
        FileOutputStream dest = null;
        try {
            String imageOrientation = Utils.getImageOrientation(srcFilePath);
            Bitmap tempBmp = null;
            int dimen;

            if (imageQuality == ImageQuality.QUALITY_MEDIUM) {
                if (toUserServerConfig) {
                    dimen = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NORMAL_IMG_SIZE, HikeConstants.SMO_MAX_DIMENSION_MEDIUM_FULL_SIZE_PX);
                } else {
                    dimen = HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX;
                }
                // Sending false as we want image smaller than actual resolution
                tempBmp = HikeBitmapFactory.scaleDownBitmap(srcFilePath, dimen, dimen, config, false, false);
            } else if (imageQuality != ImageQuality.QUALITY_ORIGINAL) {
                if (toUserServerConfig) {
                    dimen = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SUPER_COMPRESSED_IMG_SIZE, HikeConstants.SMO_MAX_DIMENSION_LOW_FULL_SIZE_PX);
                } else {
                    dimen = HikeConstants.MAX_DIMENSION_LOW_FULL_SIZE_PX;
                }
                // Sending false as we want image smaller than actual resolution
                tempBmp = HikeBitmapFactory.scaleDownBitmap(srcFilePath, dimen, dimen, config, false, false);
            }
            tempBmp = HikeBitmapFactory.rotateBitmap(tempBmp, Utils.getRotatedAngle(imageOrientation));
            if (tempBmp != null) {
                byte[] fileBytes = BitmapUtils.bitmapToBytes(tempBmp, Bitmap.CompressFormat.JPEG, quality);
                tempBmp.recycle();
                src = new ByteArrayInputStream(fileBytes);
            } else {
                src = new FileInputStream(new File(srcFilePath));
            }

            dest = new FileOutputStream(new File(destFilePath));

            byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
            int len;

            while ((len = src.read(buffer)) > 0) {
                dest.write(buffer, 0, len);
            }
            dest.flush();
            dest.getFD().sync();
            return true;
        } catch (FileNotFoundException e) {
            Logger.e("Utils", "File not found while copying", e);
            FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FTR_INIT_2_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "File", "1.Exception on Compress Image", e);
            return false;
        } catch (IOException e) {
            Logger.e("Utils", "Error while reading/writing/closing file", e);
            FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FTR_INIT_2_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "File", "2.Exception on Compress Image", e);
            return false;
        } catch (Exception ex) {
            Logger.e("Utils", "WTF Error while reading/writing/closing file", ex);
            FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FTR_INIT_2_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "File", "3.Exception on Compress Image", ex);
            return false;
        } finally {
            Utils.closeStreams(src, dest);
        }
    }

    public static String getImageOrientation(String filePath) {
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);
            return exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        } catch (IOException e) {
            Logger.e("Utils", "Error while opening file", e);
            return null;
        }
    }

    public static int getRotatedAngle(String imageOrientation) {
        if (!TextUtils.isEmpty(imageOrientation)) {
            switch (Integer.parseInt(imageOrientation)) {
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

    public static void setupUri() {
        SharedPreferences settings = HikeMessengerApp.getInstance().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
        boolean connectUsingSSL = Utils.switchSSLOn(HikeMessengerApp.getInstance());
        Utils.setupServerURL(settings.getBoolean(HikeMessengerApp.PRODUCTION, true), connectUsingSSL);
    }

    public static void setupServerURL(boolean isProductionServer, boolean ssl) {
        Logger.d("SSL", "Switching SSL on? " + ssl);

        int whichServer = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PRODUCTION_HOST_TOGGLE, AccountUtils._PRODUCTION_HOST);

        AccountUtils.ssl = (whichServer != AccountUtils._CUSTOM_HOST) ? ssl : false;

        AccountUtils.mClient = null;

        Logger.d("SSL", "Switching SSL on? " + AccountUtils.ssl);

        String httpString = AccountUtils.ssl ? AccountUtils.HTTPS_STRING : AccountUtils.HTTP_STRING;

        AccountUtils.host = isProductionServer ? AccountUtils.PRODUCTION_HOST : AccountUtils.STAGING_HOST;
        AccountUtils.port = isProductionServer ? (ssl ? AccountUtils.PRODUCTION_PORT_SSL : AccountUtils.PRODUCTION_PORT) : (ssl ? AccountUtils.STAGING_PORT_SSL
                : AccountUtils.STAGING_PORT);

        if (isProductionServer) {
            AccountUtils.base = httpString + AccountUtils.host + "/v1";
            AccountUtils.baseV2 = httpString + AccountUtils.host + "/v2";
            AccountUtils.SDK_AUTH_BASE = AccountUtils.SDK_AUTH_BASE_URL_PROD;
        } else {
            setHostAndPort(whichServer, AccountUtils.ssl);
            AccountUtils.base = httpString + AccountUtils.host + ":" + Integer.toString(AccountUtils.port) + "/v1";
            AccountUtils.baseV2 = httpString + AccountUtils.host + ":" + Integer.toString(AccountUtils.port) + "/v2";
            AccountUtils.SDK_AUTH_BASE = AccountUtils.SDK_AUTH_BASE_URL_STAGING;
        }

        AccountUtils.fileTransferHost = isProductionServer ? HttpRequestConstants.FT_PRODUCTION_API : AccountUtils.host;
        AccountUtils.fileTransferBase = httpString + AccountUtils.fileTransferHost + ":" + Integer.toString(AccountUtils.port) + "/v1";

        CheckForUpdateTask.UPDATE_CHECK_URL = httpString + (isProductionServer ? CheckForUpdateTask.PRODUCTION_URL : CheckForUpdateTask.STAGING_URL_BASE);

        AccountUtils.fileTransferBaseDownloadUrl = AccountUtils.fileTransferBase + AccountUtils.FILE_TRANSFER_DOWNLOAD_BASE;
        AccountUtils.fastFileUploadUrl = AccountUtils.fileTransferBase + AccountUtils.FILE_TRANSFER_DOWNLOAD_BASE + "ffu/";

        AccountUtils.rewardsUrl = (isProductionServer ? AccountUtils.REWARDS_PRODUCTION_BASE : AccountUtils.STAGING_HOST + AccountUtils.REWARDS_STAGING_PATH);
        AccountUtils.gamesUrl = (isProductionServer ? AccountUtils.GAMES_PRODUCTION_BASE : AccountUtils.STAGING_HOST + AccountUtils.GAMES_STAGING_PATH);
        AccountUtils.stickersUrl = (isProductionServer ? AccountUtils.HTTP_STRING + AccountUtils.STICKERS_PRODUCTION_BASE : AccountUtils.base + AccountUtils.STICKERS_STAGING_PATH);
        AccountUtils.h2oTutorialUrl = (isProductionServer ? AccountUtils.HTTP_STRING + AccountUtils.H2O_TUTORIAL_PRODUCTION_BASE : AccountUtils.base
                + AccountUtils.H2O_TUTORIAL_STAGING_PATH);
        AccountUtils.analyticsUploadUrl = AccountUtils.base + AccountUtils.ANALYTICS_UPLOAD_PATH;

        Logger.d("SSL", "Base: " + AccountUtils.base);
        Logger.d("SSL", "FTHost: " + AccountUtils.fileTransferHost);
        Logger.d("SSL", "FTUploadBase: " + AccountUtils.fileTransferBase);
        Logger.d("SSL", "UpdateCheck: " + CheckForUpdateTask.UPDATE_CHECK_URL);
        Logger.d("SSL", "FTDloadBase: " + AccountUtils.fileTransferBaseDownloadUrl);

    }

    private static void setHostAndPort(int whichServer, boolean ssl) {
        switch (whichServer) {

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
            case AccountUtils._CUSTOM_HOST:
                SharedPreferences sharedPreferences = HikeMessengerApp.getInstance().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);

                AccountUtils.host = sharedPreferences.getString(HikeMessengerApp.CUSTOM_HTTP_HOST, AccountUtils.PRODUCTION_HOST);
                AccountUtils.port = sharedPreferences.getInt(HikeMessengerApp.CUSTOM_HTTP_PORT, AccountUtils.PRODUCTION_PORT);

                break;

        }

    }

    public static boolean shouldChangeMessageState(ConvMessage convMessage, int stateOrdinal) {

        if (convMessage == null || convMessage.getTypingNotification() != null || convMessage.getUnreadCount() != -1) {
            Logger.d("BufRef", "ConvMessage is null" + convMessage);
            return false;
        }
        int minStatusOrdinal;
        int maxStatusOrdinal;
        if (stateOrdinal <= State.SENT_DELIVERED_READ.ordinal()) {
            minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
            maxStatusOrdinal = stateOrdinal;
        } else {
            minStatusOrdinal = State.RECEIVED_UNREAD.ordinal();
            maxStatusOrdinal = stateOrdinal;
        }


        int convMessageStateOrdinal = convMessage.getState().ordinal();

        Logger.d("BugRef", "Ordinal state of our ConvMessage is " + convMessageStateOrdinal);
        if (convMessageStateOrdinal <= maxStatusOrdinal && convMessageStateOrdinal >= minStatusOrdinal) {
            return true;
        }
        return false;
    }

    public static ConvMessage makeHike2SMSInviteMessage(String msisdn, Context context) {
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

    public static void sendInvite(String msisdn, Context context) {
        sendInvite(msisdn, context, false);
    }

    public static void sendInvite(String msisdn, Context context, boolean dbUpdated) {
        sendInvite(msisdn, context, dbUpdated, false);
    }

    public static void sendInvite(String msisdn, Context context, boolean dbUpdated, boolean sentMqttPacket) {

        boolean sendNativeInvite = !HikeMessengerApp.isIndianUser()
                || context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SEND_NATIVE_INVITE, false);

        ConvMessage convMessage = Utils.makeHike2SMSInviteMessage(msisdn, context);
        if (!sentMqttPacket) {
            HikeMqttManagerNew.getInstance().sendMessage(convMessage.serialize(sendNativeInvite), MqttConstants.MQTT_QOS_ONE);
        }

        if (sendNativeInvite) {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> messages = smsManager.divideMessage(convMessage.getMessage());

            ArrayList<PendingIntent> pendingIntents = new ArrayList<PendingIntent>();

			/*
			 * Adding blank pending intents as a workaround for where sms don't get sent when we pass this as null
			 */
            for (int i = 0; i < messages.size(); i++) {
                Intent intent = new Intent();
                pendingIntents.add(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
            }
			/*
			 * The try-catch block is needed for a bug in certain LG devices where it throws an NPE here.
			 */
            try {
                smsManager.sendMultipartTextMessage(convMessage.getMsisdn(), null, messages, pendingIntents, null);
            } catch (NullPointerException e) {
                Logger.d("Send invite", "NPE while trying to send SMS", e);
            }
        }

        if (!dbUpdated) {
            long time = System.currentTimeMillis() / 1000;
            ContactManager.getInstance().updateInvitedTimestamp(msisdn, time);
        }
    }

    public enum WhichScreen {
        FRIENDS_TAB, UPDATES_TAB, SMS_SECTION, OTHER
    }

    /*
	 * msisdn : mobile number to which we need to send the invite context : context of calling activity v : View of invite button which need to be set invited if not then send this
	 * as null checkPref : preference which need to set to not show this dialog. header : header text of the dialog popup body : body text message of dialog popup
	 */
    public static void sendInviteUtil(final ContactInfo contactInfo, final Context context, final String checkPref, String header, String body) {
        sendInviteUtil(contactInfo, context, checkPref, header, body, WhichScreen.OTHER);
    }

    public static void sendInviteUtil(final ContactInfo contactInfo, final Context context, final String checkPref, String header, String body, final WhichScreen whichScreen) {
        final SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

        if (!settings.getBoolean(checkPref, false) && (!HikeMessengerApp.isIndianUser() || settings.getBoolean(HikeMessengerApp.SEND_NATIVE_INVITE, false))) {
            CustomAlertDialog dialog = new CustomAlertDialog(context, -1);
            HikeDialogListener dialogListener = new HikeDialogListener() {
                @Override
                public void positiveClicked(HikeDialog hikeDialog) {
                    Editor editor = settings.edit();
                    editor.putBoolean(checkPref, ((CustomAlertDialog) hikeDialog).isChecked());
                    editor.commit();
                    invite(context, contactInfo, whichScreen);
                    hikeDialog.dismiss();
                }

                @Override
                public void neutralClicked(HikeDialog hikeDialog) {
                    hikeDialog.dismiss();
                }

                @Override
                public void negativeClicked(HikeDialog hikeDialog) {
                }
            };

            dialog.setTitle(header);
            dialog.setMessage(String.format(body, contactInfo.getFirstName()));
            dialog.setCheckBox(R.string.not_show_call_alert_msg, null, false);
            dialog.setPositiveButton(R.string.OK, dialogListener);
            dialog.setNegativeButton(R.string.CANCEL, dialogListener);

            dialog.show();
        } else {
            invite(context, contactInfo, whichScreen);
        }
    }

    private static void invite(Context context, ContactInfo contactInfo, WhichScreen whichScreen) {
        sendInvite(contactInfo.getMsisdn(), context, true);
        Toast.makeText(context, R.string.invite_sent, Toast.LENGTH_SHORT).show();

        boolean isReminding = contactInfo.getInviteTime() != 0;

        long inviteTime = System.currentTimeMillis() / 1000;
        contactInfo.setInviteTime(inviteTime);

        ContactManager.getInstance().updateInvitedTimestamp(contactInfo.getMsisdn(), inviteTime);

        HikeMessengerApp.getPubSub().publish(HikePubSub.INVITE_SENT, null);

        try {
            JSONObject md = new JSONObject();
            String msisdn = contactInfo.getMsisdn();

            switch (whichScreen) {
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

            if (!TextUtils.isEmpty(msisdn)) {
                md.put(HikeConstants.TO, msisdn);
            }
            HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, md);
        } catch (JSONException e) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        }
    }

    public static String getAddressFromGeoPoint(LatLng geoPoint, Context context) {
        try {
            Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geoCoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1);

            final StringBuilder address = new StringBuilder();
            if (!addresses.isEmpty()) {
                for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
                    address.append(addresses.get(0).getAddressLine(i) + "\n");
            }

            return address.toString();
        } catch (IOException e) {
            Logger.e("Utils", "IOException", e);
            return "";
        }
    }

    public static void addFileName(String fileName, String fileKey) {
        File hikeFileList = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT, HikeConstants.HIKE_FILE_LIST_NAME);

        JSONObject currentFiles = getHikeFileListData(hikeFileList);

        if (currentFiles == null) {
            Logger.d("Utils", "File did not exist. Will create a new one");
            currentFiles = new JSONObject();
        }
        FileOutputStream fileOutputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            Logger.d("Utils", "Adding data : " + "File Name: " + fileName + " File Key: " + fileKey);
            currentFiles.put(fileName, fileKey);
            fileOutputStream = new FileOutputStream(hikeFileList);
            byteArrayInputStream = new ByteArrayInputStream(currentFiles.toString().getBytes("UTF-8"));

            int b;
            byte[] data = new byte[8];
            while ((b = byteArrayInputStream.read(data)) != -1) {
                fileOutputStream.write(data, 0, b);
            }
            fileOutputStream.flush();
            fileOutputStream.getFD().sync();
        } catch (FileNotFoundException e) {
            Logger.e("Utils", "File not found", e);
        } catch (JSONException e) {
            Logger.e("Utils", "Invalid JSON", e);
        } catch (UnsupportedEncodingException e) {
            Logger.e("Utils", "Unsupported Encoding Exception", e);
        } catch (IOException e) {
            Logger.e("Utils", "IOException", e);
        } finally {
            Utils.closeStreams(fileOutputStream);
        }
    }

    public static void makeNewFileWithExistingData(JSONObject data) {
        File hikeFileList = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT, HikeConstants.HIKE_FILE_LIST_NAME);

        Logger.d("Utils", "Writing data: " + data.toString());

        FileOutputStream fileOutputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            fileOutputStream = new FileOutputStream(hikeFileList);
            byteArrayInputStream = new ByteArrayInputStream(data.toString().getBytes("UTF-8"));

            int b;
            byte[] d = new byte[8];
            while ((b = byteArrayInputStream.read(d)) != -1) {
                fileOutputStream.write(d, 0, b);
            }
            fileOutputStream.flush();
            fileOutputStream.getFD().sync();
        } catch (FileNotFoundException e) {
            Logger.e("Utils", "File not found", e);
        } catch (UnsupportedEncodingException e) {
            Logger.e("Utils", "Unsupported Encoding Exception", e);
        } catch (IOException e) {
            Logger.e("Utils", "IOException", e);
        } finally {
            closeStreams(fileOutputStream);
        }
    }

    private static JSONObject getHikeFileListData(File hikeFileList) {
        if (!hikeFileList.exists()) {
            return null;
        }

        FileInputStream fileInputStream = null;
        JSONObject currentFiles = null;
        BufferedReader reader = null;

        try {
            fileInputStream = new FileInputStream(hikeFileList);
            reader = new BufferedReader(new InputStreamReader(fileInputStream));

            StringBuilder builder = new StringBuilder();
            CharBuffer target = CharBuffer.allocate(10000);
            int read = reader.read(target);

            while (read >= 0) {
                builder.append(target.array(), 0, read);
                target.clear();
                read = reader.read(target);
            }

            currentFiles = new JSONObject(builder.toString());
            Logger.d("Utils", "File found: Current data: " + builder.toString());
        } catch (FileNotFoundException e) {
            Logger.e("Utils", "File not found", e);
            hikeFileList.delete();
        } catch (IOException e) {
            Logger.e("Utils", "IOException", e);
            hikeFileList.delete();
        } catch (JSONException e) {
            Logger.e("Utils", "Invalid JSON", e);
            hikeFileList.delete();
        } finally {
            closeStreams(fileInputStream, reader);
        }

        return currentFiles;
    }

    public static String normalizeNumber(String inputNumber, String countryCode) {
        if (inputNumber.startsWith("+")) {
            return inputNumber;
        } else if (inputNumber.startsWith("00")) {
			/*
			 * Doing for US numbers
			 */
            return inputNumber.replaceFirst("00", "+");
        } else if (inputNumber.startsWith("0")) {
            return inputNumber.replaceFirst("0", countryCode);
        } else {
            return countryCode + inputNumber;
        }
    }

    public static File getCloudFile(Context context, Uri uri) throws IOException, SecurityException {
        long timeStamp = System.currentTimeMillis();
        ContentResolver cR = context.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String contentType = cR.getType(uri);
        if (contentType == null)
            return null;
        HikeFileType hikeFileType = HikeFileType.fromString(contentType, false);
        String extension = mime.getExtensionFromMimeType(contentType);
        File destFile = null;
        try {
            String fileName = contentType.substring(0, contentType.indexOf("/")) + "_" + timeStamp;
            switch (hikeFileType) {
                case IMAGE:
                    destFile = File.createTempFile(fileName, "." + extension);
                    break;
                case VIDEO:
                case AUDIO:
                case OTHER:
                    String dirPath = getFileParent(hikeFileType, true);
                    if (dirPath == null) {
                        return null;
                    }
                    File dir = new File(dirPath);
                    if (!dir.exists()) {
                        if (!dir.mkdirs()) {
                            Logger.d("Hike", "failed to create directory");
                            return null;
                        }
                    }
                    destFile = new File(dir, fileName + "." + extension);
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (destFile != null)
            downloadAndSaveFile(cR, destFile, uri);
        return destFile;
    }

    public static void downloadAndSaveFile(ContentResolver cR, File destFile, Uri uri) throws IOException, SecurityException {
        InputStream is = null;
        OutputStream os = null;
        try {

            if (isPicasaUri(uri.toString()) && !uri.toString().startsWith("http")) {
                is = cR.openInputStream(uri);
            } else {
                is = new URL(uri.toString()).openStream();
            }
            os = new FileOutputStream(destFile);

            byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
            int len;

            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        } finally {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    public static boolean isPicasaUri(String picasaUriString) {
        return (picasaUriString.toString().startsWith(HikeConstants.OTHER_PICASA_URI_START) || picasaUriString.toString().startsWith(HikeConstants.JB_PICASA_URI_START)
                || picasaUriString.toString().startsWith("http") || picasaUriString.toString().startsWith(HikeConstants.GMAIL_PREFIX)
                || picasaUriString.toString().startsWith(HikeConstants.GOOGLE_PLUS_PREFIX) || picasaUriString.toString().startsWith(HikeConstants.GOOGLE_INBOX_PREFIX)
                || picasaUriString.toString().startsWith(HikeConstants.GOOGLE_DRIVE_PREFIX));
    }

    public static Uri makePicasaUriIfRequired(Uri uri) {
        if (uri.toString().startsWith("content://com.android.gallery3d.provider")) {
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
    public static boolean switchSSLOn(Context context) {
		/*
		 * If the preference itself is switched to off, we don't need to check if the wifi is on or off.
		 */
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.SSL_PREF, true)) {
            return false;
        }

        //this will ensure that no ssl(both https and mssaging) call is made for non ssl allowed countries
        if (!isSSLAllowed()) {
            return false;
        }

        NetworkInfo netInfo = getActiveNetInfo();

        if (netInfo != null && (netInfo.getType() == ConnectivityManager.TYPE_WIFI)) // there is active wifi network
        {
            return true;
        } else
        // either there is no active network or current network is not wifi
        {
            return false;
        }
    }

    public static boolean renameTempProfileImage(String msisdn) {
        String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
        String tempFileName = Utils.getTempProfileImageFileName(msisdn);
        String newFileName = Utils.getProfileImageFileName(msisdn);

        File tempFile = new File(directory, tempFileName);
        File newFile = new File(directory, newFileName);
        return tempFile.renameTo(newFile);
    }

    public static boolean removeTempProfileImage(String msisdn) {
        String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
        String tempFileName = Utils.getTempProfileImageFileName(msisdn);

        return (new File(directory, tempFileName)).delete();
    }

    public static String getTempProfileImageFileName(String msisdn) {
        return getTempProfileImageFileName(msisdn, false);
    }

    public static String getTempProfileImageFileName(String msisdn, boolean useTimeStamp) {
        String suffix = "_tmp.jpg";

        if (useTimeStamp) {
            suffix = Long.toString(System.currentTimeMillis()) + suffix;
        }

        return getValidFileNameForMsisdn(msisdn) + suffix;
    }

    public static String getProfileImageFileName(String msisdn) {
        return getValidFileNameForMsisdn(msisdn) + ".jpg";
    }

    public static String getValidFileNameForMsisdn(String msisdn) {
        return msisdn.replaceAll(":", "-");
    }

    public static void removeLargerProfileImageForMsisdn(String msisdn) {
        String path = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
        String fileName = Utils.getProfileImageFileName(msisdn);
        (new File(path, fileName)).delete();
    }

    public static boolean renameFiles(String newFilePath, String oldFilePath) {
        Logger.d(Utils.class.getSimpleName(), "inside renameUniqueTempProfileImage " + newFilePath + ", " + oldFilePath);
        if (!TextUtils.isEmpty(oldFilePath) && !TextUtils.isEmpty(newFilePath)) {
            File tempFile = new File(oldFilePath);
            File newFile = new File(newFilePath);
            if (tempFile.exists()) {
                return tempFile.renameTo(newFile);
            }
            return false;
        } else {
            Logger.d(Utils.class.getSimpleName(), "inside renameUniqueTempProfileImage, file name empty " + newFilePath + ", " + oldFilePath);
            return false;
        }
    }

    public static boolean removeFile(String tmpFilePath) {
        if (!TextUtils.isEmpty(tmpFilePath)) {
            Logger.d(Utils.class.getSimpleName(), "inside removeUniqueTempProfileImage " + tmpFilePath);
            File file = new File(tmpFilePath);
            if (file.exists()) {
                return file.delete();
            }
            return false;
        } else {
            Logger.d(Utils.class.getSimpleName(), "inside removeUniqueTempProfileImage, empty file " + tmpFilePath);
            return false;
        }
    }

    public static void vibrateNudgeReceived(Context context) {
        String VIB_OFF = context.getResources().getString(R.string.vib_off);
        if (VIB_OFF.equals(PreferenceManager.getDefaultSharedPreferences(context).getString(HikeConstants.VIBRATE_PREF_LIST, getOldVibratePref(context)))) {
            return;
        }

        if (!SoundUtils.isSilentMode(context) && !Utils.isUserInAnyTypeOfCall(context)) {
            vibrate(100);
        }
    }

    public static void vibrate(int msecs) {
        Vibrator vibrator = (Vibrator) HikeMessengerApp.getInstance().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(msecs);
        }
    }

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            }
            while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    public static String getHashedDeviceId(String deviceId) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return "and:" + SHA1(deviceId);
    }

    public static List<ContactInfoData> getContactDataFromHikeFile(HikeFile hikeFile) {
        List<ContactInfoData> items = new ArrayList<ContactInfoData>();

        JSONArray phoneNumbers = hikeFile.getPhoneNumbers();
        JSONArray emails = hikeFile.getEmails();
        JSONArray events = hikeFile.getEvents();
        JSONArray addresses = hikeFile.getAddresses();

        if (phoneNumbers != null) {
            for (int i = 0; i < phoneNumbers.length(); i++) {
                JSONObject data = phoneNumbers.optJSONObject(i);
                String key = data.names().optString(0);
                items.add(new ContactInfoData(DataType.PHONE_NUMBER, data.optString(key), key));
            }
        }

        if (emails != null) {
            for (int i = 0; i < emails.length(); i++) {
                JSONObject data = emails.optJSONObject(i);
                String key = data.names().optString(0);
                items.add(new ContactInfoData(DataType.EMAIL, data.optString(key), key));
            }
        }

        if (events != null) {
            for (int i = 0; i < events.length(); i++) {
                JSONObject data = events.optJSONObject(i);
                String key = data.names().optString(0);
                items.add(new ContactInfoData(DataType.EVENT, data.optString(key), key));
            }
        }

        if (addresses != null) {
            for (int i = 0; i < addresses.length(); i++) {
                JSONObject data = addresses.optJSONObject(i);
                String key = data.names().optString(0);
                items.add(new ContactInfoData(DataType.ADDRESS, data.optString(key), key));
            }
        }
        return items;
    }

    public static boolean killCall() {
        Context context = HikeMessengerApp.getInstance();
        try {
            // Get the boring old TelephonyManager
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Get the getITelephony() method
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            java.lang.reflect.Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);

            // Get the endCall method from ITelephony
            Class telephonyInterfaceClass = Class.forName(telephonyInterface.getClass().getName());
            java.lang.reflect.Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);

        } catch (Exception ex) { // Many things can go wrong with reflection calls
            return false;
        }
        return true;
    }

    /**
     * Get unseen status, user-status and friend request count,includes activity count as well
     *
     * @param accountPrefs     Account settings shared preference
     * @param countUsersStatus Whether to include user status count in the total
     * @return
     */
    public static int getNotificationCount(SharedPreferences accountPrefs, boolean countUsersStatus) {
        return getNotificationCount(accountPrefs, countUsersStatus, true, true, true);
    }

    /**
     * Get unseen status, user-status and friend request count,
     *
     * @param accountPrefs       Account settings shared preference
     * @param countUsersStatus   Whether to include user status count in the total
     * @param countUsersActivity Whether to include user activity count in the total
     * @param countUnseenStatus  Whether to include unseen status count in the total
     * @param friendRequestCount Whether to include friend request count in the total
     * @return
     */
    public static int getNotificationCount(SharedPreferences accountPrefs, boolean countUsersStatus, boolean countUserActivity, boolean countUnseenStatus, boolean friendRequestCount) {
        int notificationCount = 0;
        if (countUnseenStatus)
            notificationCount += accountPrefs.getInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
        if (countUserActivity)
            notificationCount += accountPrefs.getInt(HikeMessengerApp.USER_TIMELINE_ACTIVITY_COUNT, 0);
        if (countUsersStatus) {
            notificationCount += accountPrefs.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
        }
        if (friendRequestCount) {
            int frCount = accountPrefs.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0);
            notificationCount += frCount;
        }
        return notificationCount;
    }

    /*
	 * This method returns whether the device is an mdpi or ldpi device. The assumption is that these devices are low end and hence a DB call may block the UI on those devices.
	 */
    public static boolean loadOnUiThread() {
        return ((int) 10 * Utils.scaledDensityMultiplier) > 10;
    }

    public static void hideSoftKeyboard(Context context, View v) {
        if (v == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    // http://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        // Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        // If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
            return;
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showSoftKeyboard(Context context, View v) {
        if (v == null) {
            return;
        }
        showSoftKeyboard(v, InputMethodManager.RESULT_UNCHANGED_SHOWN);
    }

    public static void showSoftKeyboard(View v, int flags) {
        if (v == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(v, flags);
    }

    public static void toggleSoftKeyboard(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public static void sendLocaleToServer() {
        try {
            JSONObject mqttLanguageAnalytic = new JSONObject();

            JSONObject data = new JSONObject();

            data.put(HikeConstants.PHONE_LANGUAGE, LocalLanguageUtils.getDeviceDefaultLocale());
            //Getting APP Language
            String appLocale = LocalLanguageUtils.getApplicationLocalLanguageLocale();

            data.put(HikeConstants.APP_LANGUAGE, appLocale);
            mqttLanguageAnalytic.put(HikeConstants.DATA, data);
            mqttLanguageAnalytic.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);
            HikeMqttManagerNew.getInstance().sendMessage(mqttLanguageAnalytic, MqttConstants.MQTT_QOS_ONE);


        } catch (JSONException e) {

        }
    }


    public static void setReceiveSmsSetting(Context context, boolean value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(HikeConstants.RECEIVE_SMS_PREF, value);
        editor.commit();

        sendDefaultSMSClientLogEvent(value);
    }

    public static void setSendUndeliveredAlwaysAsSmsSetting(Context context, boolean value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, value);
        editor.commit();
    }

    public static void setSendUndeliveredAlwaysAsSmsSetting(Context context, boolean value, boolean nativeSms) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, value);
        editor.putBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, nativeSms);
        editor.commit();
    }

    public static boolean isContactInternational(String msisdn) {
        return !msisdn.startsWith("+91");
    }

    public static int getResolutionId() {
        if (densityDpi > 480) {
            return HikeConstants.XXXHDPI_ID;
        } else if (densityDpi > 320) {
            return HikeConstants.XXHDPI_ID;
        } else if (densityDpi > 240) {
            return HikeConstants.XHDPI_ID;
        } else if (densityDpi > 160) {
            return HikeConstants.HDPI_ID;
        } else if (densityDpi > 120) {
            return HikeConstants.MDPI_ID;
        } else {
            return HikeConstants.LDPI_ID;
        }
    }

    /*
	 * returns a decoded byteArray of input base64String.
	 */
    public static byte[] saveBase64StringToFile(File file, String base64String) throws IOException {
        byte[] b = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            b = Base64.decode(base64String, Base64.DEFAULT);
            if (b == null) {
                throw new IOException();
            }
            fos.write(b);
            fos.flush();
            fos.getFD().sync();
        } finally {
            if (fos != null)
                fos.close();
        }
        return b;
    }

    /**
     * Saves the byteArray to the file specified.
     */
    public static void saveByteArrayToFile(File file, byte[] byteArray) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            if (byteArray == null) {
                throw new IOException();
            }
            fos.write(byteArray);
            fos.flush();
            fos.getFD().sync();
        } finally {
            if (fos != null)
                fos.close();
        }
    }

    public static void setupFormattedTime(TextView tv, long timeElapsed) {
        if (timeElapsed < 0)
            return;
        int totalSeconds = (int) (timeElapsed);
        int minutesToShow = (int) (totalSeconds / 60);
        int secondsToShow = totalSeconds % 60;

        String time = String.format("%d:%02d", minutesToShow, secondsToShow);
        tv.setText(time);
    }

    public static boolean isUserAuthenticated(Context context) {
        return !TextUtils.isEmpty(context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.NAME_SETTING, null));
    }

    public static void appStateChanged(Context context) {
        appStateChanged(context, true, false);
    }

    public static void appStateChanged(Context context, boolean resetStealth, boolean checkIfActuallyBackgrounded) {
        appStateChanged(context, resetStealth, checkIfActuallyBackgrounded, true, false, true);
    }

    public static void appStateChanged(Context context, boolean resetStealth, boolean checkIfActuallyBackgrounded, boolean requestBulkLastSeen, boolean dueToConnect, boolean toLog) {
        if (!isUserAuthenticated(context)) {
            return;
        }

        if (checkIfActuallyBackgrounded) {
            boolean screenOn = isScreenOn(context);
            Logger.d("HikeAppState", "Screen On? " + screenOn);

            if (screenOn) {
                boolean isForegrounded = isAppForeground(context);

                if (isForegrounded) {
                    if (HikeMessengerApp.currentState != CurrentState.OPENED && HikeMessengerApp.currentState != CurrentState.RESUMED) {
                        Logger.d("HikeAppState", "Wrong state! correcting it");
                        HikeMessengerApp.currentState = CurrentState.RESUMED;
                        return;
                    }
                }
            }
        }

        sendAppState(context, requestBulkLastSeen, dueToConnect, toLog);

        StealthModeManager.getInstance()
                .appStateChange(resetStealth, HikeMessengerApp.currentState != CurrentState.OPENED && HikeMessengerApp.currentState != CurrentState.RESUMED);
    }

    public static boolean isScreenOn(Context context) {
        return ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isScreenOn();
    }

    private static void sendAppState(Context context, boolean requestBulkLastSeen, boolean dueToConnect, boolean toLog) {
        JSONObject object = new JSONObject();

        try {
            object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.APP_STATE);
            if (HikeMessengerApp.currentState == CurrentState.OPENED || HikeMessengerApp.currentState == CurrentState.RESUMED) {
                object.put(HikeConstants.SUB_TYPE, HikeConstants.FOREGROUND);

                JSONObject data = new JSONObject();
                data.put(AnalyticsConstants.JUST_OPENED, HikeMessengerApp.currentState == CurrentState.OPENED);
				/*
				 * We don't need to request for the bulk last seen from here anymore. We have the HTTP call for this.
				 */
                data.put(HikeConstants.BULK_LAST_SEEN, false);
                object.put(HikeConstants.DATA, data);

                HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.IS_HIKE_APP_FOREGROUNDED, true);
                HikeNotification.getInstance().cancelPersistNotif();
                HikeNotification.getInstance().cancelNotification(HikeNotification.NOTIF_INTERCEPT_NON_DOWNLOAD);
                HikeMessengerApp.getPubSub().publish(HikePubSub.APP_FOREGROUNDED, null);
                if (toLog) {
                    JSONObject sessionDataObject = HAManager.getInstance().recordAndReturnSessionStart();
                    sendSessionMQTTPacket(context, HikeConstants.FOREGROUND, sessionDataObject);
                }
            } else if (!dueToConnect) {
                object.put(HikeConstants.SUB_TYPE, HikeConstants.BACKGROUND);
                HikeMessengerApp.getPubSub().publish(HikePubSub.APP_BACKGROUNDED, null);
                if (toLog) {
                    JSONObject sessionDataObject = HAManager.getInstance().recordAndReturnSessionEnd();
                    sendSessionMQTTPacket(context, HikeConstants.BACKGROUND, sessionDataObject);
                    HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.IS_HIKE_APP_FOREGROUNDED, false);
                    HikeNotification.getInstance().checkAndShowUpdateNotif();
                }
            } else {
                return;
            }
            HikeMqttManagerNew.getInstance().sendMessage(object, MqttConstants.MQTT_QOS_ZERO);
        } catch (JSONException e) {
            Logger.w("AppState", "Invalid json", e);
        }
    }

    /**
     * Sends Session fg/bg Packet With MQTT_QOS_ONE
     *
     * @param context
     * @param subType
     * @param sessionMetaDataObject
     */
    public static void sendSessionMQTTPacket(Context context, String subType, JSONObject sessionMetaDataObject) {
        JSONObject sessionObject = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            sessionObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.SESSION);
            sessionObject.put(HikeConstants.SUB_TYPE, subType);

            data.put(AnalyticsConstants.EVENT_TYPE, AnalyticsConstants.SESSION_EVENT);
            data.put(AnalyticsConstants.CURRENT_TIME_STAMP, Utils.applyServerTimeOffset(context, System.currentTimeMillis() / 1000));
            data.put(AnalyticsConstants.METADATA, sessionMetaDataObject);

            sessionObject.put(HikeConstants.DATA, data);
            HikeMqttManagerNew.getInstance().sendMessage(sessionObject, MqttConstants.MQTT_QOS_ONE);
            Logger.d("sessionmqtt", "Sesnding Session MQTT Packet with qos 1, and : " + subType);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String getLastSeenTimeAsString(Context context, long lastSeenTime, int offline, boolean groupParticipant) {
        return getLastSeenTimeAsString(context, lastSeenTime, offline, groupParticipant, false);
    }

    public static String getLastSeenTimeAsString(Context context, long lastSeenTime, int offline, boolean groupParticipant, boolean fromChatThread) {
		/*
		 * This refers to the setting being turned off
		 */
        if (offline == -1) {
            return null;
        }
		/*
		 * This refers to the user being online
		 */
        if (offline == 0) {
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
        if ((lastSeenYear < nowYear) || ((nowDay - lastSeenDay) > 7)) {
            return context.getString(fromChatThread ? R.string.last_seen_while_ago_ct : R.string.last_seen_while_ago);
        }

        boolean is24Hour = android.text.format.DateFormat.is24HourFormat(context);

        String lastSeen;
		/*
		 * More than 1 day old.
		 */
        if ((nowDay - lastSeenDay) > 1) {
            String format;
            if (groupParticipant) {
                format = "dd/MM/yy";
                DateFormat df = new SimpleDateFormat(format);
                lastSeen = df.format(lastSeenDate);
            } else {
                if (is24Hour) {
                    format = "d'" + getDayOfMonthSuffix(lastSeenDayOfMonth) + "' MMM, HH:mm";
                } else {
                    format = "d'" + getDayOfMonthSuffix(lastSeenDayOfMonth) + "' MMM, h:mmaaa";
                }
                DateFormat df = new SimpleDateFormat(format);
                lastSeen = context.getString(fromChatThread ? R.string.last_seen_more_ct : R.string.last_seen_more, df.format(lastSeenDate));
            }
        } else {
            String format;
            if (is24Hour) {
                format = "HH:mm";
            } else {
                format = "h:mmaaa";
            }

            DateFormat df = new SimpleDateFormat(format);
            if (groupParticipant) {
                lastSeen = (nowDay > lastSeenDay) ? context.getString(R.string.last_seen_yesterday_group_participant) : df.format(lastSeenDate);
            } else {
                int stringRes;
                if (fromChatThread) {
                    stringRes = (nowDay > lastSeenDay) ? R.string.last_seen_yesterday_ct : R.string.last_seen_today_ct;
                } else {
                    stringRes = (nowDay > lastSeenDay) ? R.string.last_seen_yesterday : R.string.last_seen_today;
                }
                lastSeen = context.getString(stringRes, df.format(lastSeenDate));
            }
        }

        lastSeen = lastSeen.replace("AM", "am");
        lastSeen = lastSeen.replace("PM", "pm");

        return lastSeen;

    }

    private static String getDayOfMonthSuffix(int dayOfMonth) {
        if (dayOfMonth >= 11 && dayOfMonth <= 13) {
            return "th";
        }
        switch (dayOfMonth % 10) {
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

    public static long getServerTimeOffsetInMsec(Context context) {
        long timeDiff = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getLong(HikeMessengerApp.SERVER_TIME_OFFSET_MSEC, 0);
        return timeDiff;
    }

    public static long getServerTimeOffset(Context context) {
        return context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getLong(HikeMessengerApp.SERVER_TIME_OFFSET, 0);
    }

    /**
     * Applies the server time offset and ensures that the time does not go into the future
     *
     * @param context
     * @param time
     * @return
     */
    public static long applyServerTimeOffset(Context context, long time) {
        time += getServerTimeOffset(context);
        long now = System.currentTimeMillis() / 1000;
        if (time > now) {
            return now;
        } else {
            return time;
        }
    }

    /**
     * Applies the server time offset and ensures that the time becomes sync with server
     *
     * @param context
     * @param time    in seconds
     * @return time in milliseconds
     */
    public static long applyOffsetToMakeTimeServerSync(Context context, long timeInMSec) {
        timeInMSec = timeInMSec - getServerTimeOffsetInMsec(context);
        return timeInMSec;
    }

    public static void blockOrientationChange(Activity activity) {
        final int rotation = activity.getWindowManager().getDefaultDisplay().getOrientation();

        boolean isPortrait = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO || rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
            activity.setRequestedOrientation(isPortrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_270) {
            activity.setRequestedOrientation(isPortrait ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }
    }

    public static void unblockOrientationChange(Activity activity) {
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }

    public static String getMessageDisplayText(ConvMessage convMessage, Context context) {
        if (convMessage.isFileTransferMessage()) {
            HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);

            switch (hikeFile.getHikeFileType()) {
                case IMAGE:
                    String caption = convMessage.getMetadata().getCaption();
                    if (TextUtils.isEmpty(caption)) {
                        return context.getString(R.string.send_sms_img_msg);
                    } else {
                        return String.format(context.getString(R.string.image_w_caption_sms), "\"" + caption + "\"");
                    }
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

        } else if (convMessage.isStickerMessage()) {
            return context.getString(R.string.send_sms_sticker_msg);
        }
        return convMessage.getMessage();
    }

    public static void deleteFile(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteFile(f);
            }
        }
        file.delete();
    }

    public static void deleteFile(Context context, String filename, HikeFileType type) {
        if (TextUtils.isEmpty(filename)) {
            return;
        }

        HikeFile temp = new HikeFile(new File(filename).getName(), HikeFileType.toString(type), null, null, 0, false, null);
        temp.delete(context);
    }

    public static void sendLogEvent(JSONObject data) {
        sendLogEvent(data, null, null);
    }

    public static void sendLogEvent(JSONObject data, String subType, String toMsisdn) {
        sendLogEvent(data, subType, toMsisdn, HikeConstants.MqttMessageTypes.ANALYTICS_EVENT);
    }

    public static void sendLogEvent(JSONObject data, String subType, String toMsisdn, String type) {

        JSONObject object = new JSONObject();
        try {
            data.put(HikeConstants.LogEvent.TAG, HikeConstants.LOGEVENT_TAG);
            data.put(HikeConstants.C_TIME_STAMP, System.currentTimeMillis());
            data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis() / 1000));
            if (!TextUtils.isEmpty(subType)) {
                data.put(HikeConstants.SUB_TYPE, subType);
            }
            if (!TextUtils.isEmpty(toMsisdn)) {
                object.put(HikeConstants.TO, toMsisdn);
            }
            object.put(HikeConstants.TYPE, type);
            object.put(HikeConstants.DATA, data);

            HikeMqttManagerNew.getInstance().sendMessage(object, MqttConstants.MQTT_QOS_ONE);
        } catch (JSONException e) {
            Logger.w("LogEvent", e);
        }

    }

    public static void sendDefaultSMSClientLogEvent(boolean defaultClient) {
        JSONObject metadata = new JSONObject();

        try {
            metadata.put(HikeConstants.UNIFIED_INBOX, defaultClient);
            HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.SMS, metadata);
        } catch (JSONException e) {
            Logger.w(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        }

    }

    private static JSONObject jObject = null;

    public static JSONObject getJSONfromURL(String url) {
        IRequestListener requestListener = new IRequestListener() {
            @Override
            public void onRequestSuccess(Response result) {
                jObject = (JSONObject) result.getBody().getContent();
            }

            @Override
            public void onRequestProgressUpdate(float progress) {
            }

            @Override
            public void onRequestFailure(HttpException httpException) {
                jObject = null;
            }
        };

        if (TextUtils.isEmpty(url)) {
            jObject = null;
            return jObject;
        } else {
            RequestToken token = HttpRequests.getJSONfromUrl(url, requestListener);
            token.execute();
        }
        return jObject;
    }

    public static boolean isKitkatOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean isBelowLollipop() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isIceCreamOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean isJELLY_BEAN_MR2OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean isLollipopOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isJellybeanOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean isJellybeanMR1OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static boolean getSendSmsPref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.SEND_SMS_PREF, false);
    }

    public static boolean isFilenameValid(String file) {
        File f = new File(file);
        try {
            f.getCanonicalPath();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void resetUnseenStatusCount(Context context) {
        HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
        HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
    }

    public static void incrementUnseenStatusCount() {
        HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance();
        int unseenUserStatusCount = prefs.getData(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
        prefs.saveData(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, ++unseenUserStatusCount);
        prefs.saveData(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
    }

    public static void resetUnseenFriendRequestCount(Context context) {
        if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.FRIEND_REQ_COUNT, 0) > 0) {
            HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.FRIEND_REQ_COUNT, 0);
        }
        HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_COUNT_CHANGED, null);
    }

    public static boolean shouldIncrementCounter(ConvMessage convMessage) {
        return !convMessage.isSent() && convMessage.getState() == State.RECEIVED_UNREAD && convMessage.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE;
    }

    public static void createShortcut(Context activity, ConvInfo conv, boolean showToast) {
        Intent shortcutIntent;
        Intent intent = new Intent();
        if (conv instanceof BotInfo && ((BotInfo) conv).isNonMessagingBot()) {
            shortcutIntent = IntentFactory.getNonMessagingBotIntent(conv.getMsisdn(), activity);
            shortcutIntent.putExtra(HikePlatformConstants.IS_SHORTCUT, true);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            shortcutIntent = IntentFactory.createChatThreadIntentFromConversation(activity, conv, ChatThreadActivity.ChatThreadOpenSources.SHORTCUT);
        }

        if (conv instanceof BotInfo) //Adding Bot Open Source Analytics here
        {
            shortcutIntent.putExtra(AnalyticsConstants.BOT_NOTIF_TRACKER, AnalyticsConstants.BOT_OPEN_SOURCE_SHORTCUT);
        }

        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, conv.getLabel());

        Drawable avatarDrawable = Utils.getAvatarDrawableForShortcut(activity, conv.getMsisdn());

        Bitmap bitmap = HikeBitmapFactory.drawableToBitmap(avatarDrawable, Bitmap.Config.RGB_565);

        int dimension = (int) (Utils.scaledDensityMultiplier * 48);

        Bitmap scaled = HikeBitmapFactory.createScaledBitmap(bitmap, dimension, dimension, Bitmap.Config.ARGB_8888, false, true, true);
        bitmap = null;
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, scaled);
        intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        activity.sendBroadcast(intent);
        if (showToast) {
            Toast.makeText(activity, activity.getString(R.string.shortcut_created) + " for " + conv.getConversationName(), Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isVoipActivated(Context context) {
        int voipActivated = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.VOIP_ACTIVATED, 1);
        return (voipActivated == 0) ? false : true;
    }

    public static void onCallClicked(Context context, final String mContactNumber, VoIPUtils.CallSource source) {
        // Check if we have blocked this user.
        if (ContactManager.getInstance().isBlocked(mContactNumber)) {
            Toast.makeText(context, context.getString(R.string.content_des_block_overlay_message), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isUserOnline(context) && !OfflineUtils.isConnectedToSameMsisdn(mContactNumber)) {
            Toast.makeText(context, context.getString(R.string.voip_offline_error), Toast.LENGTH_SHORT).show();
            return;
        }

        context.startService(IntentFactory.getVoipCallIntent(context, mContactNumber, source));
    }

    public static void startNativeCall(Context context, String msisdn) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + msisdn));
        context.startActivity(callIntent);
    }

    public static String getFormattedDateTimeFromTimestamp(long milliSeconds, Locale current) {
        String dateFormat = "dd/MM/yyyy hh:mm:ss a";
        DateFormat formatter = new SimpleDateFormat(dateFormat, current);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds * 1000);
        return formatter.format(calendar.getTime());
    }

    public static String getFormattedDateTimeWOSecondsFromTimestamp(long milliSeconds, Locale current) {
        String dateFormat = "dd/MM/yyyy hh:mm a";
        DateFormat formatter = new SimpleDateFormat(dateFormat, current);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds * 1000);
        return formatter.format(calendar.getTime());
    }

    public static void sendUILogEvent(String key) {
        sendUILogEvent(key, null);
    }

    public static void sendUILogEvent(String key, String msisdn) {
        try {
            JSONObject data = new JSONObject();
            data.put(HikeConstants.SUB_TYPE, HikeConstants.UI_EVENT);

            JSONObject metadata = new JSONObject();
            metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.CLICK);
            metadata.put(HikeConstants.EVENT_KEY, key);

            if (!TextUtils.isEmpty(msisdn)) {
                JSONArray msisdns = new JSONArray();
                msisdns.put(msisdn);

                metadata.put(HikeConstants.TO, msisdns);
            }

            data.put(HikeConstants.METADATA, metadata);

            sendLogEvent(data);
        } catch (JSONException e) {
            Logger.w("LE", "Invalid json");
        }
    }

    public static void sendMd5MismatchEvent(String fileName, String fileKey, String md5, long recBytes, boolean downloading) {
        try {
            JSONObject metadata = new JSONObject();
            metadata.put(HikeConstants.FILE_NAME, fileName);
            metadata.put(HikeConstants.FILE_KEY, fileKey);
            metadata.put(HikeConstants.MD5_HASH, md5);
            metadata.put(HikeConstants.FILE_SIZE, recBytes);
            metadata.put(HikeConstants.DOWNLOAD, downloading);
            HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.CRC_EVENT, metadata);
        } catch (JSONException e) {
            Logger.w(AnalyticsConstants.ANALYTICS_TAG, "Invalid json");
        }
    }

    public static void resetUpdateParams(SharedPreferences prefs) {
        Editor prefEditor = prefs.edit();
        prefEditor.remove(HikeMessengerApp.DEVICE_DETAILS_SENT);
        prefEditor.remove(HikeMessengerApp.UPGRADE_RAI_SENT);
        prefEditor.putBoolean(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, true);
        prefEditor.putBoolean(HikeMessengerApp.SIGNUP_COMPLETE, true);
        prefEditor.commit();
    }

    public static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0)
                    digest.update(buffer, 0, numRead);
            }
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return null;
        } finally {
            closeStreams(inputStream);
        }
    }

    public static String StringToMD5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            if (input.length() > 0)
                digest.update(input.getBytes(), 0, input.length());
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (int i = 0; i < md5Bytes.length; i++) {
            returnVal += Integer.toString((md5Bytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal;
    }

    public static Intent getHomeActivityIntent(Context context) {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return intent;
    }

    public static Intent getPeopleActivityIntent(Context context) {
        final Intent intent = new Intent(context, PeopleActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(HikeConstants.Extras.FROM_NOTIFICATION, true);

        return intent;
    }

    public static Intent getTimelineActivityIntent(Context context) {
        return getTimelineActivityIntent(context, false, false);
    }

    public static Intent getTimelineActivityIntent(Context context, boolean openActivityFeed, boolean fromNotif) {
        final Intent intent = new Intent(context, TimelineActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(HikeConstants.Extras.FROM_NOTIFICATION, fromNotif);
        intent.putExtra(TimelineActivity.TIMELINE_SOURCE, fromNotif ? TimelineActivity.TimelineOpenSources.NOTIF : TimelineActivity.TimelineOpenSources.UNKNOWN);
        intent.putExtra(HikeConstants.Extras.OPEN_ACTIVITY_FEED, openActivityFeed);
        return intent;
    }

    public static void addCommonDeviceDetails(JSONObject jsonObject, Context context) throws JSONException {
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

    public static ConvMessage makeConvMessage(String msisdn, boolean conversationOnHike) {
        return makeConvMessage(msisdn, "", conversationOnHike);
    }

    public static ConvMessage makeConvMessage(String msisdn, String message, boolean isOnhike) {
        return makeConvMessage(msisdn, message, isOnhike, State.SENT_UNCONFIRMED);
    }

    public static ConvMessage makeConvMessage(String msisdn, String message, boolean isOnhike, State state) {
        long time = (long) System.currentTimeMillis() / 1000;
        ConvMessage convMessage = new ConvMessage(message, msisdn, time, state);
        convMessage.setSMS(!isOnhike);
        return convMessage;
    }

    public static boolean canInBitmap() {
        return true;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasJellyBeanMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean hasEnoughFreeSpaceForProfilePic() {
        double freeSpaceAvailable = getFreeSpace();
        return freeSpaceAvailable > HikeConstants.PROFILE_PIC_FREE_SPACE;
    }

    public static void addToContacts(List<ContactInfoData> items, String name, Context context) {
        Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        int phoneCount = 0;
        int emailCount = 0;
        i.putExtra(Insert.NAME, name);
        for (ContactInfoData contactData : items) {
            if (contactData.getDataType() == DataType.PHONE_NUMBER) {
                switch (phoneCount) {
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
            } else if (contactData.getDataType() == DataType.EMAIL) {
                switch (emailCount) {
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
            } else if (contactData.getDataType() == DataType.ADDRESS) {
                i.putExtra(Insert.POSTAL, contactData.getData());

            }

        }
        context.startActivity(i);
    }

    public static void addToContacts(List<ContactInfoData> items, String name, Context context, AccountData accountData, boolean showToast) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        int rawContactInsertIndex = ops.size();

        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValue(RawContacts.ACCOUNT_TYPE, (accountData != null) ? accountData.getType() : null)
                .withValue(RawContacts.ACCOUNT_NAME, (accountData != null) ? accountData.getName() : null).build());

        for (ContactInfoData contactInfoData : items) {
            switch (contactInfoData.getDataType()) {
                case ADDRESS:
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE).withValue(StructuredPostal.DATA, contactInfoData.getData())
                            .withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_CUSTOM).withValue(StructuredPostal.LABEL, contactInfoData.getDataSubType()).build());
                    break;
                case EMAIL:
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE).withValue(Email.DATA, contactInfoData.getData()).withValue(Email.TYPE, Email.TYPE_CUSTOM)
                            .withValue(Email.LABEL, contactInfoData.getDataSubType()).build());
                    break;
                case EVENT:
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE).withValue(Event.DATA, contactInfoData.getData()).withValue(Event.TYPE, Event.TYPE_CUSTOM)
                            .withValue(Event.LABEL, contactInfoData.getDataSubType()).build());
                    break;
                case PHONE_NUMBER:
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE).withValue(Phone.NUMBER, contactInfoData.getData()).withValue(Phone.TYPE, Phone.TYPE_CUSTOM)
                            .withValue(Phone.LABEL, contactInfoData.getDataSubType()).build());
                    break;
            }
        }
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE).withValue(StructuredName.DISPLAY_NAME, name).build());
        boolean contactSaveSuccessful;
        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            contactSaveSuccessful = true;
        } catch (RemoteException e) {
            e.printStackTrace();
            contactSaveSuccessful = false;
        } catch (OperationApplicationException e) {
            e.printStackTrace();
            contactSaveSuccessful = false;
        }
        if (showToast) {
            Toast.makeText(context.getApplicationContext(), contactSaveSuccessful ? R.string.contact_saved : R.string.contact_not_saved, Toast.LENGTH_SHORT).show();
        }
    }

    public static void addToContacts(List<ContactInfoData> items, String name, Context context, Spinner accountSpinner) {
        AccountData accountData = (AccountData) accountSpinner.getSelectedItem();
        addToContacts(items, name, context, accountData, true);
    }

    public static int getNumColumnsForGallery(Resources resources, int sizeOfImage) {
        return (int) (resources.getDisplayMetrics().widthPixels / sizeOfImage);
    }

    public static int getActualSizeForGallery(Resources resources, int sizeOfImage, int numColumns) {
        int remainder = resources.getDisplayMetrics().widthPixels - (numColumns * sizeOfImage);
        return (int) (sizeOfImage + (int) (remainder / numColumns));
    }

    public static void makeNoMediaFile(File root) {
        makeNoMediaFile(root, false);
    }

    /*
	 * Whenever creating a nomedia file in any dirctory and if images/videos are already present in that directory then we need to do re-scan to make them invisible from gallery.
	 */
    public static void makeNoMediaFile(File root, boolean reScan) {
        if (root == null) {
            return;
        }

        if (!root.exists()) {
            root.mkdirs();
        }
        File file = new File(root, ".nomedia");
        if (!file.exists()) {
            FileOutputStream dest = null;
            try {
                dest = new FileOutputStream(file);
				/*
				 * File content could be blank (for backwards compatibility), or have one or more of the following values separated by a newline: image|sound|video Reference -
				 * https://code.google.com/p/android/issues/detail?id=35879
				 */
                String data = "";
                dest.write(data.getBytes(), 0, data.getBytes().length);
                dest.flush();
                dest.getFD().sync();
            } catch (IOException e) {
                Logger.d("NoMedia", "Failed to make nomedia file");
            } finally {
                closeStreams(dest);
            }
            if (reScan) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    HikeMessengerApp.getInstance().getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + root)));
                } else {
                    HikeMessengerApp.getInstance().getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + root)));
                }
            }
        }
    }

    public static Set<String> getServerRecommendedContactsSelection(String serverRecommendedArrayString, String myMsisdn) {
        Set<String> msisdns = new HashSet<String>();

        if (TextUtils.isEmpty(serverRecommendedArrayString)) {
            return null;
        }
        try {
            JSONArray serverRecommendedArray = new JSONArray(serverRecommendedArrayString);
            if (serverRecommendedArray.length() == 0) {
                return null;
            }

            int i = 0;
            for (i = 0; i < serverRecommendedArray.length(); i++) {
                String msisdn = serverRecommendedArray.optString(i);
                if (!myMsisdn.equals(msisdn)) {
                    msisdns.add(msisdn);
                }
            }
            return msisdns;
        } catch (JSONException e) {
            return null;
        }
    }

    public static void startChatThread(Context context, ContactInfo contactInfo, int source) {
        Intent intent = new Intent(context, ChatThreadActivity.class);
        if (contactInfo.getName() != null) {
            intent.putExtra(HikeConstants.Extras.NAME, contactInfo.getName());
        }
        intent.putExtra(HikeConstants.Extras.MSISDN, contactInfo.getMsisdn());
        intent.putExtra(HikeConstants.Extras.SHOW_KEYBOARD, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        String whichChatThread = ChatThreadUtils.getChatThreadType(contactInfo.getMsisdn());
        intent.putExtra(HikeConstants.Extras.WHICH_CHAT_THREAD, whichChatThread);
        intent.putExtra(HikeConstants.Extras.CHAT_INTENT_TIMESTAMP, System.currentTimeMillis());
        intent.putExtra(ChatThreadActivity.CHAT_THREAD_SOURCE, source);
        context.startActivity(intent);
    }

    public static void toggleActionBarElementsEnable(View doneBtn, ImageView arrow, TextView postText, boolean enabled) {
        doneBtn.setEnabled(enabled);
        arrow.setEnabled(enabled);
        postText.setEnabled(enabled);
    }

    public static Drawable getAvatarDrawableForNotification(Context context, String msisdn, boolean isPin) {
        if (msisdn.equals(context.getString(R.string.app_name)) || msisdn.equals(HikeNotification.HIKE_STEALTH_MESSAGE_KEY)) {
            return null;
        }

        Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);

        if (isPin || drawable == null) {
            if (isPin) {
                Drawable background = context.getResources().getDrawable(BitmapUtils.getDefaultAvatarResourceId(msisdn, false));
                Drawable iconDrawable = context.getResources().getDrawable(R.drawable.ic_pin_notification);
                drawable = new LayerDrawable(new Drawable[]{background, iconDrawable});
            } else {
                drawable = HikeBitmapFactory.getDefaultTextAvatar(msisdn);
            }

        }
        return drawable;
    }

    public static Drawable getAvatarDrawableForShortcut(Context context, String msisdn) {
        if (msisdn.equals(context.getString(R.string.app_name)) || msisdn.equals(HikeNotification.HIKE_STEALTH_MESSAGE_KEY)) {
            return context.getResources().getDrawable(R.drawable.hike_avtar_protip);
        }

        Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);

        if (drawable == null) {
            drawable = HikeBitmapFactory.getRectTextAvatar(msisdn);
        }
        return drawable;
    }

    public static void getRecommendedAndHikeContacts(Context context, List<ContactInfo> recommendedContacts, List<ContactInfo> hikeContacts, List<ContactInfo> friendsList) {
        SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
        String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, "");
        friendsList.addAll(ContactManager.getInstance().getContactsOfFavoriteType(FavoriteType.FRIEND, HikeConstants.BOTH_VALUE, msisdn, false));
        friendsList.addAll(ContactManager.getInstance().getContactsOfFavoriteType(FavoriteType.REQUEST_SENT, HikeConstants.BOTH_VALUE, msisdn, false));
        friendsList.addAll(ContactManager.getInstance().getContactsOfFavoriteType(FavoriteType.REQUEST_SENT_REJECTED, HikeConstants.BOTH_VALUE, msisdn, false));

        Logger.d(TAG, " friendsList size " + friendsList.size());
        Set<String> recommendedContactsSelection = Utils.getServerRecommendedContactsSelection(settings.getString(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null), msisdn);
        Logger.d(TAG, " recommendedContactsSelection " + recommendedContactsSelection);
        if (!recommendedContactsSelection.isEmpty()) {
            recommendedContacts.addAll(ContactManager.getInstance().getHikeContacts(-1, recommendedContactsSelection, null, msisdn));
        }

        Logger.d(TAG, " size recommendedContacts = " + recommendedContacts.size());

        hikeContacts.addAll(ContactManager.getInstance().getContactsOfFavoriteType(FavoriteType.NOT_FRIEND, HikeConstants.ON_HIKE_VALUE, msisdn, false));
        hikeContacts.addAll(ContactManager.getInstance().getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED_REJECTED, HikeConstants.ON_HIKE_VALUE, msisdn, false, true));
        hikeContacts.addAll(ContactManager.getInstance().getContactsOfFavoriteType(FavoriteType.REQUEST_RECEIVED, HikeConstants.BOTH_VALUE, msisdn, false, true));
    }

    public static void addFavorite(final Context context, final ContactInfo contactInfo, final boolean isFtueContact, String addFavSource) {
        toggleFavorite(context, contactInfo, isFtueContact, addFavSource);
        if (!contactInfo.isOnhike() || HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_ADD_FAVORITE_TIP, false)) {
            return;
        }

        if (!Utils.isFavToFriendsMigrationAllowed()) {
            HikeDialogFactory.showDialog(context, HikeDialogFactory.FAVORITE_ADDED_DIALOG,
                    new HikeDialogListener() {

                        @Override
                        public void positiveClicked(HikeDialog hikeDialog) {
                            hikeDialog.dismiss();
                            HikeSharedPreferenceUtil.getInstance()
                                    .saveData(HikeMessengerApp.SHOWN_ADD_FAVORITE_TIP, true);
                        }

                        @Override
                        public void neutralClicked(HikeDialog hikeDialog) {
                        }

                        @Override
                        public void negativeClicked(HikeDialog hikeDialog) {
                            hikeDialog.dismiss();
                            HikeSharedPreferenceUtil.getInstance()
                                    .saveData(HikeMessengerApp.SHOWN_ADD_FAVORITE_TIP, true);
                        }

                    }, contactInfo.getFirstName());
        }
    }

    public static FavoriteType toggleFavorite(Context context, ContactInfo contactInfo, boolean isFtueContact, String addFavSource) {
        FavoriteType favoriteType;
        boolean isRequestSent = false;
        if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED) {
            favoriteType = FavoriteType.FRIEND;
        } else {
            favoriteType = FavoriteType.REQUEST_SENT;
            isRequestSent = true;
            Toast.makeText(context, Utils.isFavToFriendsMigrationAllowed() ? R.string.friend_request_sent : R.string.favorite_request_sent, Toast.LENGTH_SHORT).show();
        }

        //2-way friendship established. Get Historical updates here!
        if (favoriteType == FavoriteType.FRIEND) {
            fetchHistoricalUpdates(contactInfo.getMsisdn());
        }

        HikeAnalyticsEvent.recordAnalyticsForAddFriend(contactInfo.getMsisdn(), addFavSource, isRequestSent);

        Pair<ContactInfo, FavoriteType> favoriteAdded;

        if (isFtueContact) {
			/*
			 * Cloning the object since we don't want to send the ftue reference.
			 */
            ContactInfo contactInfo2 = new ContactInfo(contactInfo);
            favoriteAdded = new Pair<ContactInfo, FavoriteType>(contactInfo2, favoriteType);
        } else {
            favoriteAdded = new Pair<ContactInfo, FavoriteType>(contactInfo, favoriteType);
        }

        HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED, favoriteAdded);
        return favoriteType;
    }

    public static void addToContacts(Context context, String msisdn) {
        Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        i.putExtra(Insert.PHONE, msisdn);
        context.startActivity(i);
    }

    public static void addToContacts(Context context, String msisdn, String name, String address) {
        Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
        intent.putExtra(Insert.PHONE, msisdn);
        intent.putExtra(Insert.NAME, name);
        if (address != null) {
            intent.putExtra(Insert.POSTAL, address);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static final void cancelScheduledStealthReset() {
        HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME);
    }

    public static long getOldTimestamp(int min) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -min);
        long old = cal.getTimeInMillis();
        return old;
    }

    ;

    public static boolean isAppForeground(Context context) {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> l = mActivityManager.getRunningAppProcesses();
        // TODO. need review if we should return true or false.crash#46.
        if (isEmpty(l))
            return false;

        Iterator<RunningAppProcessInfo> i = l.iterator();
        while (i.hasNext()) {
            RunningAppProcessInfo info = i.next();

            if (info.uid == context.getApplicationInfo().uid && info.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && info.importanceReasonCode == 0) {

                Field field = null;
                try {
                    field = RunningAppProcessInfo.class.getDeclaredField("processState");
                } catch (NoSuchFieldException e) {
                    Logger.d(ChatHeadUtils.class.getSimpleName(), "processState field not found");
                    return true;
                }

                if (field != null) {

                    Integer state = null;
                    try {
                        state = field.getInt(info);
                    } catch (IllegalAccessException e) {
                        Logger.d(ChatHeadUtils.class.getSimpleName(), "illegal access of processState");
                        return true;
                    } catch (IllegalArgumentException e) {
                        Logger.d(ChatHeadUtils.class.getSimpleName(), "illegal argument of processState");
                        return true;
                    }
                    // its a hidden api and no value is defined
                    if (state != null && state == ChatHeadUtils.PROCESS_STATE_TOP) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String replaceUrlSpaces(String fileUriString) {
		/*
		 * In some phones URI is received with spaces in file path we should first replace all these spaces with %20 than pass it on to URI.create() method. URI.create() method
		 * treats space as an invalid charactor in URI.
		 */
        return fileUriString.replace(" ", "%20");
    }

    /*
	 * This function is to respect old vibrate preference before vib list pref , if previous was on send, VIB Default else return VIB_OFF
	 */
    public static String getOldVibratePref(Context context) {
        SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = context.getResources();
        String vibOff = res.getString(R.string.vib_off);
        String vibDef = res.getString(R.string.vib_default);

        if (preferenceManager.getBoolean(HikeConstants.VIBRATE_PREF, true)) {
            return vibDef;
        } else {
            return vibOff;
        }
    }


    public static void handleBulkLastSeenPacket(Context context, JSONObject jsonObj) throws JSONException {
		/*
		 * {"t": "bls", "ts":<server timestamp>, "d": {"lastseens":{"+919818149394":<last_seen_time_in_epoch> ,"+919810335374":<last_seen_time_in_epoch>}}}
		 */
        JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
        JSONObject lastSeens = null;
        if (data != null)
            lastSeens = data.getJSONObject(HikeConstants.BULK_LAST_SEEN_KEY);
        // Iterator<String> iterator = lastSeens.keys();

        if (lastSeens != null) {
            for (Iterator<String> iterator = lastSeens.keys(); iterator.hasNext(); ) {
                String msisdn = iterator.next();
                int isOffline;
                long lastSeenTime = lastSeens.getLong(msisdn);
                if (lastSeenTime > 0) {
                    isOffline = 1;
                    lastSeenTime = Utils.applyServerTimeOffset(context, lastSeenTime);
                } else {
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

    public static void updateLastSeenTimeInBulk(List<ContactInfo> contactList) {
        for (ContactInfo contactInfo : contactList) {
            String msisdn = contactInfo.getMsisdn();
            if (HikeMessengerApp.lastSeenFriendsMap.containsKey(msisdn)) {
                Pair<Integer, Long> lastSeenValuePair = HikeMessengerApp.lastSeenFriendsMap.get(msisdn);

                int isOffline = lastSeenValuePair.first;

                long updatedLastSeenValue = lastSeenValuePair.second;
                long previousLastSeen = contactInfo.getLastSeenTime();

                if (updatedLastSeenValue > previousLastSeen) {
                    contactInfo.setLastSeenTime(updatedLastSeenValue);
                }
                contactInfo.setOffline(isOffline);
            }
        }
    }

    public static boolean isListContainsMsisdn(List<ContactInfo> contacts, String msisdn) {
        for (ContactInfo contactInfo : contacts) {
            if (contactInfo.getMsisdn().equals(msisdn)) {
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
    public static int updateHomeOverflowToggleCount(SharedPreferences accountPref, boolean defaultValue) {
        int overallCount = 0;
        if (!(accountPref.getBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, defaultValue)) && accountPref.getBoolean(HikeMessengerApp.SHOW_GAMES, false) && !TextUtils.isEmpty(accountPref.getString(HikeMessengerApp.REWARDS_TOKEN, ""))) {
            overallCount++;
        }
        if (!(accountPref.getBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, defaultValue)) && accountPref.getBoolean(HikeMessengerApp.SHOW_REWARDS, false) && !TextUtils.isEmpty(accountPref.getString(HikeMessengerApp.REWARDS_TOKEN, ""))) {
            overallCount++;
        }
        return overallCount;
    }

    public static void incrementOrDecrementFriendRequestCount(SharedPreferences accountPref, int count) {
        int currentCount = accountPref.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0);

        currentCount += count;
        if (currentCount >= 0) {
            Editor editor = accountPref.edit();
            editor.putInt(HikeMessengerApp.FRIEND_REQ_COUNT, currentCount);
            editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
            editor.commit();
        }
        HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_COUNT_CHANGED, null);
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<String> getPackagesMatchingIntent(String action, String category, String mimeType) {
        Intent shareIntent = new Intent(action);
        if (!TextUtils.isEmpty(category)) {
            shareIntent.addCategory(category);
        }
        if (!TextUtils.isEmpty(mimeType)) {
            shareIntent.setType(mimeType);
        }
        List<ResolveInfo> resolveInfoList = HikeMessengerApp.getInstance().getPackageManager().queryIntentActivities(shareIntent, 0);

        List<String> matchedPackages = new ArrayList<String>(resolveInfoList.size());
        if (!resolveInfoList.isEmpty()) {
            for (ResolveInfo ri : resolveInfoList) {
                matchedPackages.add(ri.activityInfo.packageName);
            }
        }
        return matchedPackages;
    }

    public static void clearJar(Context c) {
        HashMap<URL, JarFile> jarCache = null;
        try {
            Class<?> jarURLConnectionImplClass;

            jarURLConnectionImplClass = Class.forName("libcore.net.url.JarURLConnectionImpl");

            final Field jarCacheField = jarURLConnectionImplClass.getDeclaredField("jarCache");
            jarCacheField.setAccessible(true);
            jarCache = (HashMap<URL, JarFile>) jarCacheField.get(null);
        } catch (Exception e) {
            Logger.e("clearJar", "Exception while getting jarCacheField : " + e);
        }

        if (jarCache != null) {
            try {
                for (final Iterator<Map.Entry<URL, JarFile>> iterator = jarCache.entrySet().iterator(); iterator.hasNext(); ) {
                    final Map.Entry<URL, JarFile> e = iterator.next();
                    final URL url = e.getKey();
                    if (url.toString().endsWith(".apk") && url.toString().contains(c.getPackageName())) {
                        Logger.i("clearJar", "Removing static hashmap entry for " + url);
                        try {
                            final JarFile jarFile = e.getValue();
                            jarFile.close();
                            iterator.remove();
                        } catch (Exception f) {
                            Logger.e("clearJar", "Exception in removing hashmap entry for " + url, f);
                        }
                    }
                }
            } catch (Exception e) {
                Logger.e("clearJar", "Exception when traversing through hashmap" + e);
            }
        }
    }

    public static String combineInOneSmsString(Context context, boolean resetTimestamp, Collection<ConvMessage> convMessages, boolean isFreeHikeSms) {
        String combinedMessageString = "";
        int count = 0;
        for (ConvMessage convMessage : convMessages) {
            if (!convMessage.isSent()) {
                break;
            }

            if (resetTimestamp && convMessage.getState().ordinal() < State.SENT_CONFIRMED.ordinal()) {
                convMessage.setTimestamp(System.currentTimeMillis() / 1000);
            }

            combinedMessageString += Utils.getMessageDisplayText(convMessage, context);

            if (++count >= HikeConstants.MAX_FALLBACK_NATIVE_SMS) {
                break;
            }

			/*
			 * Added line enters among messages
			 */
            if (count != convMessages.size()) {
                combinedMessageString += "\n\n";
            }
        }

        if (isFreeHikeSms) {
            combinedMessageString += "\n\n" + "- " + context.getString(R.string.sent_by_hike);
        }

        return combinedMessageString;
    }

    // @GM
    // The following methods returns the user readable size when passed the bytes in size
    public static String getSizeForDisplay(long bytes) {
        if (bytes <= 0)
            return ("");
        if (bytes >= 1000 * 1024 * 1024) {
            long gb = bytes / (1024 * 1024 * 1024);
            long gbPoint = bytes % (1024 * 1024 * 1024);
            gbPoint /= (1024 * 1024 * 102);
            return (Long.toString(gb) + "." + Long.toString(gbPoint) + " GB");
        } else if (bytes >= (1000 * 1024)) {
            long mb = bytes / (1024 * 1024);
            long mbPoint = bytes % (1024 * 1024);
            mbPoint /= (1024 * 102);
            return (Long.toString(mb) + "." + Long.toString(mbPoint) + " MB");
        } else if (bytes >= 1000) {
            long kb;
            if (bytes < 1024) // To avoid showing "1000KB"
                kb = bytes / 1000;
            else
                kb = bytes / 1024;
            return (Long.toString(kb) + " KB");
        } else
            return (Long.toString(bytes) + " B");
    }

    public static Intent getIntentForPrivacyScreen(Context context) {
        Intent intent = new Intent(context, HikePreferences.class);
        intent.putExtra(HikeConstants.Extras.PREF, R.xml.privacy_preferences);
        intent.putExtra(HikeConstants.Extras.TITLE, R.string.privacy);
        return intent;
    }

    public static Intent getIntentForHiddenSettings(Context context) {
        Intent intent = new Intent(context, HikePreferences.class);
        intent.putExtra(HikeConstants.Extras.PREF, R.xml.stealth_preferences);
        intent.putExtra(HikeConstants.Extras.TITLE, R.string.stealth_mode_title);
        return intent;
    }

    public static boolean isCompressed(byte[] bytes) {
        try {
            if ((bytes == null) || (bytes.length < 2)) {
                return false;
            } else {
                return ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static byte[] uncompressByteArray(byte[] bytes) throws IOException {

        int DEFAULT_BUFFER_SIZE = 1024 * 4;

        if (!isCompressed(bytes)) {
            return bytes;
        }

        ByteArrayInputStream bais = null;
        GZIPInputStream gzis = null;
        ByteArrayOutputStream baos = null;

        try {
            bais = new ByteArrayInputStream(bytes);
            gzis = new GZIPInputStream(bais);
            baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);

            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int n = 0;
            while (-1 != (n = gzis.read(buffer))) {
                baos.write(buffer, 0, n);
            }
            gzis.close();
            bais.close();

            byte[] uncompressedByteArray = baos.toByteArray();
            baos.close();

            return uncompressedByteArray;
        } catch (IOException ioex) {
            throw ioex;
        } finally {
            if (gzis != null) {
                gzis.close();
            }
            if (bais != null) {
                bais.close();
            }
            if (baos != null) {
                baos.close();
            }
        }
    }

    public static void emoticonClicked(Context context, int emoticonIndex, EditText composeBox) {
        HikeConversationsDatabase.getInstance().updateRecencyOfEmoticon(emoticonIndex, System.currentTimeMillis());

        // We don't add an emoticon if the compose box is near its maximum
        // length of characters
        if (composeBox.length() >= context.getResources().getInteger(R.integer.max_length_message) - 20) {
            return;
        }

        String emoji = SmileyParser.getInstance().addSmiley(composeBox, emoticonIndex);

        StickerManager.getInstance().logEmoticonUsageAnalytics(emoji);
    }

    public static Animation getNotificationIndicatorAnim() {
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
                                            final ArrayList<String> countriesArray, final HashMap<String, String> countriesMap, final HashMap<String, String> codesMap, final HashMap<String, String> languageMap) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().getAssets().open("countries.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] args = line.split(";");
                countriesArray.add(0, args[1]);
                countriesMap.put(args[1], args[2]);
                codesMap.put(args[2], args[1]);
                languageMap.put(args[0], args[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(countriesArray, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.compareTo(rhs);
            }
        });

        String prevCode = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).getString(HikeMessengerApp.TEMP_COUNTRY_CODE, "");
        if (TextUtils.isEmpty(countryCode)) {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String countryIso = TextUtils.isEmpty(prevCode) ? manager.getNetworkCountryIso().toUpperCase() : prevCode;
            String countryName = languageMap.get(countryIso);

            if (countryName == null || selectCountry(countryName, countriesMap, countriesArray, countryCode, countryCodeEditor, countryNameEditor)) {
                selectCountry(defaultCountryName, countriesMap, countriesArray, countryCode, countryCodeEditor, countryNameEditor);
            }
        }

        countryCodeEditor.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                String text = countryCodeEditor.getText().toString();
                String countryName = codesMap.get(text);
                if (countryName != null) {
                    int index = countriesArray.indexOf(countryName);
                    if (index != -1) {
                        countryNameEditor.setText(countryName);
                    } else {
                        countryNameEditor.setText(R.string.wrong_country);
                    }
                } else {
                    countryNameEditor.setText(R.string.wrong_country);
                }
            }
        });
    }

    public static boolean selectCountry(String countryName, HashMap<String, String> countriesMap, ArrayList<String> countriesArray, String countryCode, TextView countryCodeEditor,
                                        TextView countryNameEditor) {
        int index = countriesArray.indexOf(countryName);
        if (index != -1) {
            countryCode = countriesMap.get(countryName);
            countryCodeEditor.setText(countryCode);
            countryNameEditor.setText(countryName);
        }
        return !TextUtils.isEmpty(countryCode);
    }

    // added for db query
    public static String getMsisdnStatement(Collection<String> msisdnList) {
        if (null == msisdnList) {
            return null;
        } else {
            if (msisdnList.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder("(");
            for (String msisdn : msisdnList) {
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

    public static void startWebViewActivity(Context context, String url, String title) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, url);
        intent.putExtra(HikeConstants.Extras.TITLE, title);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static Drawable getChatTheme(ChatTheme chatTheme, Context context) {
		/*
		 * for xhdpi and above we should not scale down the chat theme nodpi asset for hdpi and below to save memory we should scale it down
		 */
        int inSampleSize = 1;
        if (!chatTheme.isTiled() && Utils.scaledDensityMultiplier < 2) {
            inSampleSize = 2;
        }

        Bitmap b = HikeBitmapFactory.decodeSampledBitmapFromResource(context.getResources(), chatTheme.bgResId(), inSampleSize);

        BitmapDrawable bd = HikeBitmapFactory.getBitmapDrawable(context.getResources(), b);

        Logger.d(context.getClass().getSimpleName(), "chat themes bitmap size= " + BitmapUtils.getBitmapSize(b));

        if (bd != null && chatTheme.isTiled()) {
            bd.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
        }

        return bd;
    }

    public static void resetPinUnreadCount(OneToNConversation conv) {
        if (conv.getMetadata() != null) {
            try {
                conv.getMetadata().setUnreadPinCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN, 0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_PIN_METADATA, conv);
            HikeMessengerApp.getPubSub().publish(HikePubSub.UNREAD_PIN_COUNT_RESET, conv);
        }
    }

    public static void handleFileForwardObject(JSONObject multiMsgFwdObject, HikeFile hikeFile) throws JSONException {
        multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_KEY, hikeFile.getFileKey());
        if (hikeFile.getHikeFileType() == HikeFileType.LOCATION) {
            multiMsgFwdObject.putOpt(HikeConstants.Extras.ZOOM_LEVEL, hikeFile.getZoomLevel());
            multiMsgFwdObject.putOpt(HikeConstants.Extras.LATITUDE, hikeFile.getLatitude());
            multiMsgFwdObject.putOpt(HikeConstants.Extras.LONGITUDE, hikeFile.getLongitude());
        } else if (hikeFile.getHikeFileType() == HikeFileType.CONTACT) {
            multiMsgFwdObject.putOpt(HikeConstants.Extras.CONTACT_METADATA, hikeFile.serialize().toString());
        } else {
            multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_PATH, hikeFile.getFilePath());
            multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_TYPE, hikeFile.getFileTypeString());
            if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING) {
                multiMsgFwdObject.putOpt(HikeConstants.Extras.RECORDING_TIME, hikeFile.getRecordingDuration());
            }
        }

    }

    public static String getFormattedDate(Context context, long timestamp) {
        if (timestamp < 0) {
            return "";
        }
        Date date = new Date(timestamp * 1000);
        String format;
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            format = "d MMM ''yy";
        } else {
            format = "d MMM ''yy";
        }

        DateFormat df = new SimpleDateFormat(format);
        return df.format(date);
    }

    public static String getFormattedTime(boolean pretty, Context context, long timestampInSeconds) {
        if (timestampInSeconds < 0) {
            return "";
        }
        if (pretty) {
            return getFormattedPrettyTime(context, timestampInSeconds);
        } else {
            return getFormattedTime(context, timestampInSeconds * 1000);
        }
    }

    public static String getFormattedTime(Context context, long timestampInMillis) {
        String format;
        Date givenDate = new Date(timestampInMillis);
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            format = "HH:mm";
        } else {
            format = "h:mm aaa";
        }

        DateFormat df = new SimpleDateFormat(format);
        return df.format(givenDate);
    }

    public static String getFormattedPrettyTime(Context context, long timestampInSeconds) {
        try {
            if (timestampInSeconds < 0) {
                return "";
            }

            long givenTimeStampInMillis = timestampInSeconds * 1000;
            Calendar givenCalendar = Calendar.getInstance();
            givenCalendar.setTimeInMillis(givenTimeStampInMillis);

            long currentTime = System.currentTimeMillis();
            Calendar currentCalendar = Calendar.getInstance();

            if (givenCalendar.before(currentCalendar)) {
                long timeDiff = currentTime - givenTimeStampInMillis;

                if (timeDiff < 60 * 1000) {
                    // until 1 minute
                    return context.getResources().getString(R.string.now);
                } else if (givenCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
                    // Show date in relative format. eg. 2 hours ago, yesterday, 2 days ago etc.
                    return HikeDateUtils.getRelativeTimeSpanString(context, givenTimeStampInMillis, currentTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_MONTH).toString();
                } else {
                    // Shows date in numeric format
                    return HikeDateUtils.getRelativeTimeSpanString(context, givenTimeStampInMillis, currentTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_NUMERIC_DATE).toString();
                }
            } else {
                if (givenCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
                    if (givenCalendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR)) {
                        // Show time in non relate default time format
                        return getFormattedTime(context, givenTimeStampInMillis);
                    } else {
                        // Show date in MMM dd format eg. Apr 21, May 13 etc.
                        return HikeDateUtils.getRelativeTimeSpanString(context, givenTimeStampInMillis, currentTime, DateUtils.YEAR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE).toString();
                    }
                } else {
                    // Show date in numeric format
                    return HikeDateUtils.getRelativeTimeSpanString(context, givenTimeStampInMillis, currentTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_NUMERIC_DATE).toString();
                }
            }
        } catch (Exception e) {
            return getFallBackPrettyTime(context, timestampInSeconds);
        }

    }

    private static String getFallBackPrettyTime(Context context, long timestampInSeconds) {
        try {
            long givenTimeStampInMillis = timestampInSeconds * 1000;
            Calendar givenCalendar = Calendar.getInstance();
            givenCalendar.setTimeInMillis(givenTimeStampInMillis);

            Calendar currentCalendar = Calendar.getInstance();

            if (givenCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
                if (givenCalendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR)) {
                    // Show time in non relate default time format
                    return getFormattedTime(context, givenTimeStampInMillis);
                } else {
                    // Show date in MMM dd format eg. Apr 21, May 13 etc.
                    return DateUtils.formatDateRange(context, givenTimeStampInMillis, givenTimeStampInMillis, DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR);
                }
            } else {
                // Show date in numeric format
                return DateUtils.formatDateRange(context, givenTimeStampInMillis, givenTimeStampInMillis, DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR);
            }
        } catch (Exception e) {
            return "";
        }
    }

    public static Pair<String[], String[]> getMsisdnToNameArray(Conversation conversation) {
        if (conversation instanceof GroupConversation) {
            Map<String, PairModified<GroupParticipant, String>> groupParticipants = ((GroupConversation) conversation).getConversationParticipantList();
            String[] msisdnArray = new String[groupParticipants.size()];
            String[] nameArray = new String[groupParticipants.size()];

            int i = 0;
            for (PairModified<GroupParticipant, String> groupParticipant : groupParticipants.values()) {
                msisdnArray[i] = groupParticipant.getFirst().getContactInfo().getMsisdn();
                nameArray[i++] = groupParticipant.getSecond();
            }
            return new Pair<String[], String[]>(msisdnArray, nameArray);
        }
        return new Pair<String[], String[]>(null, null);
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return String.format("%d B", size);
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0f);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / 1024.0f / 1024.0f);
        } else {
            return String.format("%.1f GB", size / 1024.0f / 1024.0f / 1024.0f);
        }
    }

    public static AlertDialog showNetworkUnavailableDialog(Context context) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.no_internet_try_again);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    /**
     * @param c        - contact info object
     * @param myMsisdn - self msisdn
     * @return <br>
     * false if</br>
     * <p/>
     * <li>contact msisdn equals myMsisdn</li> <li>contact favorite state is FRIENDS</li> <li>contact favorite state is REQUEST_RECIEVED</li> <li>contact favorite state is
     * REQUEST_RECIEVED_REJECTED</li>
     * <p/>
     * <p>
     * true otherwise
     * </p>
     */
    public static boolean shouldDeleteIcon(ContactInfo c, String myMsisdn) {
        String msisdn = c.getMsisdn();
        if (msisdn.equalsIgnoreCase(myMsisdn) || c.getFavoriteType().equals(FavoriteType.FRIEND) || c.getFavoriteType().equals(FavoriteType.REQUEST_RECEIVED)
                || c.getFavoriteType().equals(FavoriteType.REQUEST_RECEIVED_REJECTED)) {
            return false;
        } else {
            return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.DELETE_IC_ON_CONTACT_REMOVE, true);
        }
    }

    @SuppressWarnings("deprecation")
    public static void setClipboardText(String str, Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", str);
        clipboard.setPrimaryClip(clip);
    }

    public static String getClipboardText(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            String text = item.getText().toString();
            return text;
        } catch (NullPointerException | IndexOutOfBoundsException ex) {
            ex.printStackTrace();

        }
        return null;
    }

    /**
     * This method is used to remove a contact as a favorite based on existing favorite type. It returns either FavoriteType.REQUEST_RECEIVED_REJECTED or FavoriteType.NOT_FRIEND
     *
     * @param contactInfo
     */

    public static FavoriteType checkAndUnfriendContact(ContactInfo contactInfo) {
        FavoriteType favoriteType;
        if (contactInfo.getFavoriteType() == FavoriteType.FRIEND) {
            favoriteType = FavoriteType.REQUEST_RECEIVED_REJECTED;
        } else {
            favoriteType = FavoriteType.NOT_FRIEND;
        }

        Pair<ContactInfo, FavoriteType> favoriteRemoved = new Pair<ContactInfo, FavoriteType>(contactInfo, favoriteType);
        HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED, favoriteRemoved);
        return favoriteType;
    }

    public static String loadJSONFromAsset(Context context, String jsonFileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(jsonFileName + ".json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    /**
     * Returns the device Orientation as either ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
     *
     * @param ctx
     * @return ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
     */
    public static int getDeviceOrientation(Context ctx) {
        return ctx.getResources().getConfiguration().orientation;
    }

    public static List<AccountData> getAccountList(Context context) {
        Account[] a = AccountManager.get(context).getAccounts();
        // Clear out any old data to prevent duplicates
        List<AccountData> accounts = new ArrayList<AccountData>();

        // Get account data from system
        AuthenticatorDescription[] accountTypes = AccountManager.get(context).getAuthenticatorTypes();

        // Populate tables
        for (int i = 0; i < a.length; i++) {
            // The user may have multiple accounts with the same name, so we
            // need to construct a
            // meaningful display name for each.
            String type = a[i].type;
			/*
			 * Only showing the user's google accounts
			 */
            if (!AnalyticsConstants.ACCOUNT_TYPE_GOOGLE.equals(type)) {
                continue;
            }
            String systemAccountType = type;
            AuthenticatorDescription ad = getAuthenticatorDescription(systemAccountType, accountTypes);
            AccountData data = new AccountData(a[i].name, ad, context);
            accounts.add(data);
        }

        return accounts;
    }

    /**
     * Obtain the AuthenticatorDescription for a given account type.
     *
     * @param type       The account type to locate.
     * @param dictionary An array of AuthenticatorDescriptions, as returned by AccountManager.
     * @return The description for the specified account type.
     */
    private static AuthenticatorDescription getAuthenticatorDescription(String type, AuthenticatorDescription[] dictionary) {
        for (int i = 0; i < dictionary.length; i++) {
            if (dictionary[i].type.equals(type)) {
                return dictionary[i];
            }
        }
        // No match found
        throw new RuntimeException("Unable to find matching authenticator");
    }


    public static short getNetworkShort(String networkType) {
        switch (networkType) {
            case "wifi":
                return 1;
            case "2g":
                return 4;
            case "3g":
                return 3;
            case "4g":
                return 2;
            case "off":
                return -1;
            case "unknown":
                return 0;
            default:
                return 0;
        }
    }

    /**
     * Fetches the network connection using connectivity manager
     *
     * @param context
     * @return <li>-1 in case of no network</li> <li>0 in case of unknown network</li> <li>1 in case of wifi</li> <li>2 in case of 2g</li> <li>3 in case of 3g</li> <li>4 in case of
     * 4g</li>
     */
    public static short getNetworkType(Context context) {
        return getNetworkType(context, null);
    }

    public static short getNetworkType(Context context, NetworkInfo info) {
        int networkType = -1;

        // Contains all the information about current connection
        if (null == info) {
            info = getActiveNetInfo();
        }

        if (info != null) {
            if (!info.isConnected())
                return -1;
            // If device is connected via WiFi
            if (info.getType() == ConnectivityManager.TYPE_WIFI)
                return 1; // return 1024 * 1024;
            else
                networkType = info.getSubtype();
        } else {
            return -1;
        }

        // There are following types of mobile networks
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_LTE: // ~ 10+ Mbps // API level 11
                return 4;
            case TelephonyManager.NETWORK_TYPE_EVDO_0: // ~ 400-1000 kbps
            case TelephonyManager.NETWORK_TYPE_EVDO_A: // ~ 600-1400 kbps
            case TelephonyManager.NETWORK_TYPE_HSDPA: // ~ 2-14 Mbps
            case TelephonyManager.NETWORK_TYPE_HSPA: // ~ 700-1700 kbps
            case TelephonyManager.NETWORK_TYPE_UMTS: // ~ 400-7000 kbps
            case TelephonyManager.NETWORK_TYPE_EHRPD: // ~ 1-2 Mbps // API level 11
            case TelephonyManager.NETWORK_TYPE_HSPAP: // ~ 10-20 Mbps // API level 13
            case TelephonyManager.NETWORK_TYPE_EVDO_B: // ~ 5 Mbps // API level 9
            case TelephonyManager.NETWORK_TYPE_HSUPA: // ~ 1-23 Mbps
                return 3;
            case TelephonyManager.NETWORK_TYPE_1xRTT: // ~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_CDMA: // ~ 14-64 kbps
            case TelephonyManager.NETWORK_TYPE_EDGE: // ~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_GPRS: // ~ 100 kbps
            case TelephonyManager.NETWORK_TYPE_IDEN: // ~25 kbps // API level 8
            case NETWORK_TYPE_GSM:
                return 2;
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default:
                return 0;
        }
    }

    public static void sendDetailsAfterSignup(Context context, boolean upgrade, boolean sendBot) {
        sendDeviceDetails(context, upgrade, sendBot);
        SharedPreferences accountPrefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
        if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) > -1) {
            try {
                JSONObject metadata = new JSONObject();

                if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) == HikeConstants.WelcomeTutorial.STICKER_VIEWED.ordinal()) {
                    metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_TUTORIAL_STICKER_VIEWED);
                } else if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) == HikeConstants.WelcomeTutorial.CHAT_BG_VIEWED.ordinal()) {
                    metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_TUTORIAL_CBG_VIEWED);
                }
                HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);

                Editor editor = accountPrefs.edit();
                editor.remove(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED);
                editor.commit();
            } catch (JSONException e) {
                Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
            }
        }
    }

    public static void sendDeviceDetails(Context context, boolean upgrade, boolean sendBot) {
        recordDeviceDetails(context);
        requestAccountInfo(upgrade, sendBot);
        sendLocaleToServer();
    }

    /**
     * @param calendar
     * @param hour     hour value in 24 hour format eg. 2PM = 14
     * @param minutes
     * @param seconds
     */
    public static long getTimeInMillis(Calendar calendar, int hour, int minutes, int seconds, int milliseconds) {
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, milliseconds);

        return calendar.getTimeInMillis();
    }

    /**
     * Get time in millisecond from given time-stamp represented in format HH:mm:ss.SSS
     *
     * @param calendar    Instance of calendar to be checked
     * @param timeStamp   Parseable time-stamp as string value
     * @param default_hh  Default hour element as integer value
     * @param default_mm  Default minute element as integer value
     * @param default_ss  Default second element as integer value
     * @param default_SSS Default element milliSecond element as integer value
     * @return System-level clock value represented by long value.
     * @author Ved Prakash Singh [ved@hike.in]
     */
    public static long getTimeInMillis(Calendar calendar, String timeStamp, int default_hh, int default_mm, int default_ss, int default_SSS) {
        if (!isBlank(timeStamp)) {
            try {
                int date = calendar.get(Calendar.DATE);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);

                calendar.setTime((new SimpleDateFormat(HikeConstants.FORMAT_TIME_OF_THE_DAY, Locale.ENGLISH)).parse(timeStamp));

                // Preserve supplied date from calendar instance, so that only time elements of a day are set from given time-stamp
                calendar.set(Calendar.DATE, date);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.YEAR, year);

                return calendar.getTimeInMillis();
            } catch (ParseException e) {
                Logger.e("TimeStampParsing", "Error while parsing given time-stamp string...", e);
            }
        }

        return getTimeInMillis(calendar, default_hh, default_mm, default_ss, default_SSS);
    }

    public static void disableNetworkListner(Context context) {
        ComponentName mmComponentName = new ComponentName(context, ConnectionChangeReceiver.class);

        context.getPackageManager().setComponentEnabledSetting(mmComponentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

    }

    public static void enableNetworkListner(Context context) {
        ComponentName mmComponentName = new ComponentName(context, ConnectionChangeReceiver.class);

        context.getPackageManager().setComponentEnabledSetting(mmComponentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

    }

    public static JSONObject getPostDeviceDetails(Context context) {
        String osVersion = Build.VERSION.RELEASE;
        String devType = HikeConstants.ANDROID;
        String os = HikeConstants.ANDROID;
        String deviceVersion = Build.MANUFACTURER + " " + Build.MODEL;
        String appVersion = "";
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            Logger.e("AccountUtils", "Unable to get app version");
        }

        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceKey = manager.getDeviceId();

        JSONObject data = new JSONObject();
        try {
            data.put(HikeConstants.DEV_TYPE, devType);
            data.put(HikeConstants.APP_VERSION, appVersion);
            data.put(HikeConstants.LogEvent.OS, os);
            data.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
            data.put(HikeConstants.DEVICE_VERSION, deviceVersion);
            data.put(HikeConstants.DEVICE_KEY, deviceKey);
            Utils.addCommonDeviceDetails(data, context);
        } catch (JSONException e) {
            Logger.e("Exception", "Invalid JSON", e);
        }
        return data;
    }

    /**
     * Checks if is user signed up. Works with application context.
     *
     * @return true, if is user signed up
     */
    public static boolean requireAuth(Context appContext, boolean allowOpeningActivity) {
        appContext = appContext.getApplicationContext();

        HikeSharedPreferenceUtil settingPref = HikeSharedPreferenceUtil.getInstance();

        if (!settingPref.getData(HikeMessengerApp.ACCEPT_TERMS, false)) {
            if (allowOpeningActivity) {
                IntentFactory.openWelcomeActivity(appContext);
            }
            return false;
        }

        if (settingPref.getData(HikeMessengerApp.NAME_SETTING, null) == null) {
            if (allowOpeningActivity) {
                IntentFactory.openSignupActivity(appContext);
            }
            return false;
        }
        return true;
    }

    /**
     * Tells if User is on Telephonic/Audio/Vedio/Voip Call Return whether response received is valid or not.
     *
     * @param response
     * @return <li>false if either response is null if we get "stat":"fail" in response or "stat" key is missing</li> <li>true otherwise</li>
     */
    public static boolean isResponseValid(JSONObject response) {
        if (response == null || HikeConstants.FAIL.equals(response.optString(HikeConstants.STATUS))) {
            return false;
        }
        return true;
    }

    /**
     * Tells if User is on 1) Between any Telephonic/Audio/Vedio/Voip Call 2) Any Telephonic call is ringing
     *
     * @param context
     * @return
     */
    public static boolean isUserInAnyTypeOfCall(Context context) {

        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        boolean callMode = manager.getMode() == AudioManager.MODE_IN_COMMUNICATION || manager.getMode() == AudioManager.MODE_IN_CALL
                || manager.getMode() == AudioManager.MODE_RINGTONE;

        return callMode;
    }

    /**
     * Fetches the network connection using connectivity manager
     *
     * @param context
     * @return <li>-1 in case of no network</li> <li>0 in case of unknown network</li> <li>1 in case of wifi</li> <li>2 in case of 2g</li> <li>3 in case of 3g</li> <li>4 in case of
     * 4g</li>
     */
    public static String getNetworkTypeAsString(Context context) {
        String networkType = "";
        switch (getNetworkType(context)) {
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

    /*
	 * Returns the name of the device owner.
	 *
	 * @param context
	 *
	 * @return The device owner's name, or an empty string
	 */
    @SuppressLint("InlinedApi")
    public static String getOwnerName(Context context) {
        String name = "";

        if (isIceCreamOrHigher() && context != null) {
            try {
                Cursor c = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
                if (c != null) {
                    if (c.moveToFirst()) {
                        name = c.getString(c.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
                    }
                    c.close();
                }
            } catch (SecurityException e) {
                Logger.e("Utils", "Security exception while trying to getOwnerName");
            }
        }

        return name;
    }

    public static String conversationType(String msisdn) {
        if (BotUtils.isBot((msisdn))) {
            return HikeConstants.BOT;
        } else if (OneToNConversationUtils.isGroupConversation(msisdn)) {
            return HikeConstants.GROUP_CONVERSATION;
        } else {
            return HikeConstants.ONE_TO_ONE_CONVERSATION;
        }
    }

    /**
     * Returns Data Consumed in KB
     *
     * @param appId
     * @return
     */
    public static long getTotalDataConsumed(int appId) {
        long received = TrafficStats.getUidRxBytes(appId); // In KB

        long sent = TrafficStats.getUidTxBytes(appId); // In KB

        if (received != TrafficStats.UNSUPPORTED && sent != TrafficStats.UNSUPPORTED) {
            return received / (1024) + sent / (1024);
        } else {
            return TrafficsStatsFile.getTotalBytesManual(appId); // In KB
        }
    }

    public static Bitmap viewToBitmap(View view) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        } catch (Exception e) {
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

    public static boolean isConversationMuted(String msisdn) {
        if ((OneToNConversationUtils.isGroupConversation(msisdn))) {
            if (HikeConversationsDatabase.getInstance().isGroupMuted(msisdn)) {
                return true;
            }
        } else if (BotUtils.isBot(msisdn)) {
            if (HikeConversationsDatabase.getInstance().isBotMuted(msisdn)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLastSeenSetToFavorite() {
        Context appContext = HikeMessengerApp.getInstance().getApplicationContext();
        String defValue = appContext.getString(R.string.privacy_my_contacts);
        return PreferenceManager.getDefaultSharedPreferences(appContext).getString(HikeConstants.LAST_SEEN_PREF_LIST, defValue)
                .equals(appContext.getString(R.string.privacy_favorites));
    }

    public static boolean isOkHttp() {
        return HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.TOGGLE_OK_HTTP, true);
    }

    public static boolean isAddressbookCallsThroughHttpMgrEnabled() {
        return HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.ENABLE_ADDRESSBOOK_THROUGH_HTTP_MGR, false);
    }

    /**
     * Returns active network info
     *
     * @return
     */
    public static NetworkInfo getActiveNetInfo() {
        NetworkInfo netInfo = getNetInfoFromConnectivityManager().first;
        return netInfo;
    }

    /**
     * Now we might say network is there even if we don't have a NetworkInfo object that is why we returning NetworkInfo and NeworkAvailable states seprately. this is basically
     * done to tackle some exception scenarios where getActiveNetworkInfo unexpectedly throws an error.
     *
     * @return Pair<NetworkInfo, Boolean>.first ==> NeworkInfo object of current available network ; Pair<NetworkInfo, Boolean>.second ==> boolean indicating wheather network is
     * available or not
     */
    public static Pair<NetworkInfo, Boolean> getNetInfoFromConnectivityManager() {
        try {
            JSONObject blockedStateData = new JSONObject();
            ConnectivityManager cm = (ConnectivityManager) HikeMessengerApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getDetailedState() == NetworkInfo.DetailedState.BLOCKED) {
                blockedStateData.put(HikeConstants.LogEvent.NET_INFO, netInfo.getDetailedState());
            }

            if (netInfo != null && netInfo.isConnected()) {
                Logger.d("getNetInfoFromConnectivityManager", "Trying to connect using getActiveNetworkInfo");
                return new Pair<NetworkInfo, Boolean>(netInfo, true);
            }

            netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (netInfo != null && netInfo.getDetailedState() == NetworkInfo.DetailedState.BLOCKED) {
                blockedStateData.put(HikeConstants.LogEvent.NET_INFO_MOBILE, netInfo.getDetailedState());
            }

            if (netInfo != null && netInfo.isConnected()) {
                Logger.d("getNetInfoFromConnectivityManager", "Trying to connect using TYPE_MOBILE NetworkInfo");
                return new Pair<NetworkInfo, Boolean>(netInfo, true);
            } else {
                netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (netInfo != null && netInfo.getDetailedState() == NetworkInfo.DetailedState.BLOCKED) {
                    blockedStateData.put(HikeConstants.LogEvent.NET_INFO_WIFI, netInfo.getDetailedState());
                }
                if (netInfo != null && netInfo.isConnected()) {
                    Logger.d("getNetInfoFromConnectivityManager", "Trying to connect using TYPE_WIFI NetworkInfo");
                    return new Pair<NetworkInfo, Boolean>(netInfo, true);
                }
            }
            recordBlockedNetworkState(blockedStateData);
        } catch (Exception e) {
            Logger.e("getNetInfoFromConnectivityManager", "Got expection while trying to get NetworkInfo from ConnectivityManager", e);
            recordGetActiveNetworkInfoException(e.getMessage());
            return new Pair<NetworkInfo, Boolean>(null, true);
        }
        return new Pair<NetworkInfo, Boolean>(null, false);
    }

    private static void recordGetActiveNetworkInfoException(String exceptionMessage) {
        if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.EXCEPTION_ANALYTIS_ENABLED, true)) {
            return;
        }
        try {
            JSONObject metadata = new JSONObject();

            metadata.put(HikeConstants.PAYLOAD, exceptionMessage);

            Logger.w("Utils", "recording getActiveNetworkInfo exception message = " + exceptionMessage);
            HAManager.getInstance().record(HikeConstants.EXCEPTION, HikeConstants.LogEvent.GET_ACTIVE_NETWORK_INFO, metadata);
        } catch (JSONException e) {
            Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        }
    }

    private static void recordBlockedNetworkState(JSONObject data) {
        if (!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NET_BLOCKED_STATE_ANALYTICS, true)) {
            return;
        }
        try {
            Logger.w("Utils", "Recording network blocked state = " + data);
            HAManager.getInstance().record(HikeConstants.EXCEPTION, HikeConstants.NET_BLOCKED_STATE_ANALYTICS, data);
        } catch (Exception e) {
            Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        }
    }

    public static String valuesToCommaSepratedString(ArrayList<Long> entries) {
        StringBuilder result = new StringBuilder("(");
        for (Long entry : entries) {
            result.append(DatabaseUtils.sqlEscapeString(String.valueOf(entry)) + ",");
        }
        int idx = result.lastIndexOf(",");
        if (idx >= 0) {
            result.replace(idx, result.length(), ")");
        }
        return result.toString();
    }

    public static Long getMaxLongValue(ArrayList<Long> values) {
        if (values == null || values.isEmpty()) {
            return Long.MIN_VALUE;
        }

        Long maxVal = values.get(0);
        for (Long value : values) {
            if (value > maxVal) {
                maxVal = value;
            }
        }

        return maxVal;
    }

    public static boolean isOnProduction() {
        return HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PRODUCTION, true);
    }

    public static void setSSLAllowed(String countryCode) {
        for (String code : HikeConstants.SSL_NOT_ALLOWED_COUNTRIES) {
            if (countryCode.equalsIgnoreCase(code)) {
                HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SSL_ALLOWED, false);
                return;
            }
        }

        HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SSL_ALLOWED, true);
    }

    public static boolean isSSLAllowed() {
        return HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SSL_ALLOWED, true);
    }

    public static String extractFullFirstName(String fullName) {
        String fullFirstName = null;

        if (TextUtils.isEmpty(fullName)) {
            return "";
        }

        String[] args = fullName.trim().split(" ", 3);

        if (args.length > 1) {
            // if contact has some prefix, name would be prefix + first-name else first-name + first word of last name
            fullFirstName = args[0] + " " + args[1];
        } else {
            fullFirstName = fullName;
        }
        return fullFirstName;
    }

    public static int getLayoutIdFromName(String layoutName) {
        if (!TextUtils.isEmpty(layoutName)) {
            Context context = HikeMessengerApp.getInstance().getApplicationContext();
            int resID = context.getResources().getIdentifier(layoutName, "layout", context.getPackageName());
            return resID;
        } else {
            return -1;
        }
    }

    /**
     * Making the profile pic change a status message
     *
     * @param response json packet received from server
     * @return StatusMessage created
     */
    public static StatusMessage createTimelinePostForDPChange(JSONObject response, boolean setIcon) {
        StatusMessage statusMessage = null;
        JSONObject data = response.optJSONObject("status");

        if (data == null) {
            return null;
        }

        // parse status params
        String mappedId = data.optString(HikeConstants.STATUS_ID);
        String msisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, "");
        String name = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.NAME_SETTING, "");
        long time = (long) System.currentTimeMillis() / 1000;

        // saving mapped status id for this dp change. delete update will clear this pref later
        // this pref's current value will decide whether to give option to user to delete dp post from favourites timelines or not
        Editor ed = HikeSharedPreferenceUtil.getInstance().getPref().edit();
        ed.putString(HikeMessengerApp.DP_CHANGE_STATUS_ID, mappedId);
        ed.commit();

        // save to db
        statusMessage = new StatusMessage(0, mappedId, msisdn, name, "", StatusMessageType.PROFILE_PIC, time, -1, 0);
        HikeConversationsDatabase.getInstance().addStatusMessage(statusMessage, true);

		/*
		 * Making a status update file so we don't need to download this file again.
		 */
        String srcFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + msisdn + ".jpg";
        String destFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + mappedId + ".jpg";
        int imageCompressQuality = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SERVER_CONFIG_DEFAULT_IMAGE_SAVE_QUALITY,
                HikeConstants.HikePhotos.DEFAULT_IMAGE_SAVE_QUALITY);
        Utils.copyFile(srcFilePath, destFilePath);

        if (setIcon) {
			/* the server only needs a smaller version */
            final Bitmap smallerBitmap = HikeBitmapFactory.scaleDownBitmap(destFilePath, HikeConstants.PROFILE_IMAGE_DIMENSIONS, HikeConstants.PROFILE_IMAGE_DIMENSIONS,
                    Bitmap.Config.RGB_565, true, false);

            byte[] bytes = null;

            if (smallerBitmap != null) {
                bytes = BitmapUtils.bitmapToBytes(smallerBitmap, Bitmap.CompressFormat.JPEG, 100);
            }
            ContactManager.getInstance().setIcon(mappedId, bytes, true);
        }

        return statusMessage;
    }

    public static StatusMessage createTimelinePostForDPChange(JSONObject response) {
        return createTimelinePostForDPChange(response, true);
    }

    public static boolean isDeviceRooted() {
        return RootUtil.isDeviceRooted();
    }

    private static class RootUtil {
        public static boolean isDeviceRooted() {
            return checkRootMethod1() || checkRootMethod2() || checkRootMethod3() || checkRootMethod4();
        }

        private static boolean checkRootMethod1() {
            String buildTags = android.os.Build.TAGS;
            return buildTags != null && buildTags.contains("test-keys");
        }

        private static boolean checkRootMethod2() {
            return new File("/system/app/Superuser.apk").exists();
        }

        private static boolean checkRootMethod3() {
            String[] paths = {"/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su",
                    "/data/local/su"};
            for (String path : paths) {
                if (new File(path).exists())
                    return true;
            }
            return false;
        }

        private static boolean checkRootMethod4() {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                if (in.readLine() != null)
                    return true;
                return false;
            } catch (Throwable t) {
                return false;
            } finally {
                if (process != null)
                    process.destroy();
            }
        }
    }

    public static boolean isPhotosEditEnabled() {
        if (!Utils.isUserSignedUp(HikeMessengerApp.getInstance().getApplicationContext(), false)) {
            return false;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.ENABLE_PHOTOS, true);
        } else {
            return false;
        }
    }

    public static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return new ThreadFactory() {
            private AtomicInteger i = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                int threadCount = i.getAndIncrement();
                Thread result = new Thread(runnable);
                result.setName(name + "-" + threadCount);
                result.setDaemon(daemon);
                result.setPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE + android.os.Process.THREAD_PRIORITY_BACKGROUND);
                return result;
            }
        };
    }

    public static RejectedExecutionHandler rejectedExecutionHandler() {
        return new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

            }
        };
    }

    public static boolean isSendLogsEnabled() {
        HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance();

        if (prefs != null) {
            return prefs.getData(HikeConstants.Extras.ENABLE_SEND_LOGS, false);
        }

        return false;
    }

    public static boolean isTimelineShowCountEnabled() {
        HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance();

        if (prefs != null) {
            return prefs.getData(HikeConstants.Extras.STATUS_UPDATE_SHOW_COUNTS, true);
        }

        return false;
    }

    public static boolean isTimelineShowLikesEnabled() {
        HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance();

        if (prefs != null) {
            return prefs.getData(HikeConstants.Extras.STATUS_UPDATE_SHOW_LIKES, true);
        }

        return false;
    }

    public static boolean isGCViaLinkEnabled() {
        HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance();

        if (prefs != null) {
            return prefs.getData(HikeConstants.ENABLE_GC_VIA_LINK_SHARING, false);
        }

        return false;
    }

    public static boolean moveFile(File inputFile, File outputFile) {
        Logger.d("Utils", "Input file path - " + inputFile.getPath());
        Logger.d("Utils", "Output file path - " + outputFile.getPath());
        boolean result = false;
        InputStream in = null;
        OutputStream out = null;
        try {
            if (outputFile.exists()) {
                outputFile.delete();
            }

            in = new FileInputStream(inputFile);
            out = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            inputFile.delete();
            result = true;
        } catch (FileNotFoundException e1) {
            result = false;
            Logger.e("Utils", "1Failed due to - " + e1.getMessage());
            FTAnalyticEvents.logDevException(FTAnalyticEvents.DOWNLOAD_RENAME_FILE, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "File", "1.Exception on moving file", e1);
        } catch (Exception e2) {
            result = false;
            Logger.e("Utils", "2Failed due to - " + e2.getMessage());
            FTAnalyticEvents.logDevException(FTAnalyticEvents.DOWNLOAD_RENAME_FILE, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "File", "2.Exception on moving file", e2);
        } finally {
            closeStreams(in, out);
        }
        return result;
    }

    public static boolean resetUnreadCounterForConversation(ConvInfo convInfo) {
        ConvMessage lastMessage = convInfo.getLastConversationMsg();
        if (lastMessage != null && lastMessage.getState() == State.RECEIVED_UNREAD) {
            lastMessage.setState(State.RECEIVED_READ);
            convInfo.setUnreadCount(0);
            HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_LAST_MSG_STATE, new Pair<Integer, String>(lastMessage.getState().ordinal(), convInfo.getMsisdn()));
            return true;
        }

        return false;
    }

    public static String getCameraResultFile() {
        HikeSharedPreferenceUtil sharedPreference = HikeSharedPreferenceUtil.getInstance();
        final String capturedFilepath = sharedPreference.getData(HikeMessengerApp.FILE_PATH, null);
        sharedPreference.removeData(HikeMessengerApp.FILE_PATH);

        if (capturedFilepath != null) {
            File imageFile = new File(capturedFilepath);

            if (imageFile != null && imageFile.exists()) {
                HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable() {
                    @Override
                    public void run() {
                        MediaScannerConnection.scanFile(HikeMessengerApp.getInstance(), new String[]{capturedFilepath}, null, null);
                    }
                }, 0);
                return capturedFilepath;
            } else {
                Logger.e("Hike Camera Image", "Image File does not exists");
                return null;
            }

        } else {
            Logger.e("Hike Camera Image", "Image path is null");
            return null;
        }
    }

    private static String getPathFromDocumentedUri(Uri uri, Context context) {
        String result = null;

        if (isExternalStorageDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            if ("primary".equalsIgnoreCase(type)) {
                result = Environment.getExternalStorageDirectory() + "/" + split[1];
            }
        } else if (isDownloadsDocument(uri)) {
            final String id = DocumentsContract.getDocumentId(uri);
            final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

            result = getDataColumn(context, contentUri, null, null);
        } else if (isMediaDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            final String selection = "_id=?";
            final String[] selectionArgs = new String[]{split[1]};

            result = getDataColumn(context, contentUri, selection, selectionArgs);
        }
        return result;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * Determine whether supplied String is actually empty or not.
     *
     * @param s String to be checked
     * @return True, if string contains only white spaces or it is empty. False, if string containes at least one non-white space character.
     * @author Ved Prakash Singh [ved@hike.in]
     */
    public static boolean isBlank(final CharSequence s) {
        boolean result = true;
        int length = ((s == null) ? 0 : s.length());

        for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                result = false;
                break;
            }
        }

        return result;
    }

    public static void closeStreams(Closeable... closableStreams) {
        for (Closeable closeable : closableStreams) {
            try {
                if (closeable != null)
                    closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
                Logger.d("Utils", "Exception on closing stream : " + e);
            }
        }
    }

    /**
     * Copies File from scrFilePath to DesFilePath
     *
     * @param srcFilePath
     * @param destFilePath
     * @return
     */
    public static boolean copyFile(String srcFilePath, String destFilePath) {
		/*
		 * If source and destination have the same path, just return.
		 */
        if (srcFilePath.equals(destFilePath)) {
            return true;
        }
        try {
            File destFile = new File(destFilePath);
            InputStream src = new FileInputStream(new File(srcFilePath));
            FileOutputStream dest = new FileOutputStream(destFile);

            File parentFolder = destFile.getParentFile();
            if (parentFolder != null && !parentFolder.exists()) {
                parentFolder.mkdirs();
            }

            byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
            int len;

            while ((len = src.read(buffer)) > 0) {
                dest.write(buffer, 0, len);
            }

            dest.flush();
            dest.getFD().sync();
            src.close();
            dest.close();

            return true;
        } catch (FileNotFoundException e) {
            Logger.e("Utils", "File not found while copying", e);
            return false;
        } catch (IOException e) {
            Logger.e("Utils", "Error while reading/writing/closing file", e);
            return false;
        } catch (Exception ex) {
            Logger.e("Utils", "WTF Error while reading/writing/closing file", ex);
            return false;
        }
    }

    public static boolean isFileInSameDirectory(String filePath1, String filePath2) {
        File file1 = new File(filePath1);
        File file2 = new File(filePath2);

        if (!file1.exists() || !file2.exists()) {
            return false;
        }

        if (file1.getParent().equals(file2.getParent())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns Total RAM in bytes for HIKE
     *
     * @return
     */
    public static double getTotalRAMForHike() {
        long maxAvailableSize = 0L;
        try {
            Runtime info = Runtime.getRuntime();
            maxAvailableSize = info.maxMemory();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e("image_config", "Could not get Total RAM from Runtime");
        }
        return maxAvailableSize * 8;
    }

    public static int getDeviceScreenArea() {
        int screenWidth = HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().widthPixels;
        int screenHeight = HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().heightPixels;
        Logger.d("image_config", "Screen dimens are :- " + screenWidth + ", " + screenHeight);
        return screenHeight * screenHeight;
    }

    public static int getDeviceWidth() {
        return HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().widthPixels;
    }

    public static int getDeviceHeight() {
        return HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().heightPixels;
    }

    public static int getDeviceDensityDPI() {
        return HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().densityDpi;
    }

    public static String getStackTrace(Throwable ex) {
        if (ex == null) {
            return "";
        }
        StringWriter errorTrace = new StringWriter();
        ex.printStackTrace(new PrintWriter(errorTrace));
        return errorTrace.toString();
    }

    public static Uri getFormedUri(Context context, String unformedUrl, String token) {
        // this RE checks for starting characters followed by :// to match http:// or https://
        if (!unformedUrl.toLowerCase().matches("^\\w+://.*")) {
            // making it a valid http URL
            unformedUrl = AccountUtils.HTTP_STRING + unformedUrl;
        }
        Uri formedUri = Uri.parse(unformedUrl).buildUpon().scheme((Utils.switchSSLOn(context) || URLUtil.isHttpsUrl(unformedUrl)) ? "https" : "http")
                .appendPath(HikeConstants.ANDROID).appendPath(token).build();
        return formedUri;
    }

    public static int getOverflowMenuWidth(Context context) {
        Resources res = context.getResources();

        return (res.getDimensionPixelSize(R.dimen.overflow_menu_width) + (2 * res.getDimensionPixelSize(R.dimen.overflow_menu_shadow_padding)));
    }

    /**
     * Utility method to verify the presence of bottom nav bar in Android phones
     *
     * @param context
     * @return
     */
    public static boolean hasBottomNavBar(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point realPoint = new Point();
        Display display = wm.getDefaultDisplay();
        display.getRealSize(realPoint);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        return (metrics.heightPixels != realPoint.y) || (metrics.widthPixels != realPoint.x);
    }

    /**
     * Utility method to calculate the bottom navBar height
     *
     * @param context
     * @return
     */
    public static int getBottomNavBarHeight(Context context) {
        if (hasBottomNavBar(context)) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Point realPoint = new Point();
            Display display = wm.getDefaultDisplay();
            display.getRealSize(realPoint);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);

            return Math.abs(metrics.heightPixels - realPoint.y);
        }

        return 0;
    }

    public static boolean isWindowFlagEnabled(int whichFlag, Window window) {
        if (window == null)
            return false;

        return (window.getAttributes().flags & whichFlag) != 0;
    }

    /**
     * Utility method to calculate the bottom navBar width in landscape mode
     *
     * @param context
     * @return
     */
    public static int getBottomNavBarWidth(Context context) {
        if (hasBottomNavBar(context)) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Point realPoint = new Point();
            Display display = wm.getDefaultDisplay();
            display.getRealSize(realPoint);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);

            return Math.abs(metrics.widthPixels - realPoint.x);
        }

        return 0;

    }

    /**
     * Checks that an Iterable is both non-null and non-empty. This method does not check individual elements in the Iterable, it just checks that the Iterable has at least one
     * element.
     *
     * @param argument the argument to validate
     * @return true is argument is empty. false otherwise
     */
    public static <S, T extends Iterable<S>> boolean isEmpty(T argument) {
        return (argument == null) || !argument.iterator().hasNext();
    }

    public static boolean isEmpty(JSONArray jsonArray) {
        return (jsonArray == null) || (jsonArray.length() == 0);
    }

    /**
     * Determine whether supplied module is being tested.
     *
     * @param moduleName String name of the module being analysed
     * @return True, if test mode is enabled for given module. False, otherwise.
     * @author Ved Prakash Singh [ved@hike.in]
     */
    public static boolean isTestMode(String moduleName) {
        boolean result = false;

        if (BuildConfig.DEBUG && (getExternalStorageState() != ExternalStorageState.NONE)) {
            String testFolderName = HikeMessengerApp.getInstance().getPackageName() + "_test_" + HikeConstants.APP_TEST_UID;
            File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
            File listRootFile[] = root.listFiles();

            if ((listRootFile != null) && (listRootFile.length > 0)) {
                for (int i = 0; i < listRootFile.length; i++) {
                    if (listRootFile[i].isDirectory() && listRootFile[i].getName().equalsIgnoreCase(testFolderName)) {
                        File listTestFile[] = listRootFile[i].getAbsoluteFile().listFiles();

                        if (listTestFile != null) {
                            for (int j = 0; j < listTestFile.length; j++) {
                                if (listTestFile[j].isDirectory()) {
                                    if (listTestFile[j].getName().equalsIgnoreCase(moduleName)) {
                                        result = true;

                                        break;
                                    }
                                }
                            }
                        }

                        break;
                    }
                }
            }
        }
        return result;
    }

    public static void preFillArrayList(List<?> list, int capacity) {
        for (int i = 0; i < capacity; i++) {
            list.add(null);
        }
    }

    public static void deleteFiles(Context context, ArrayList<String> fileNames, HikeFileType type) {
        if (fileNames == null || fileNames.isEmpty()) {
            return;
        }

        for (String filepath : fileNames) {
            deleteFile(context, filepath, type);
        }

    }

    public static boolean isColumnExistsInTable(SQLiteDatabase db, String tableName, String givenColumnName) {
        if (db != null) {
            Cursor cursor = db.rawQuery("pragma table_info(" + tableName + ")", null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String columnName = cursor.getString(1);
                    if (givenColumnName.equals(columnName)) {
                        Logger.e("ColumnExistsCheck", givenColumnName + " column exists in " + tableName + " table.");
                        return true;
                    }
                }
            }
        }

        Logger.w("ColumnExistsCheck", givenColumnName + " column does not exist in " + tableName + " table.");
        return false;
    }

    /**
     * Determine whether databse recognized by given instance contains given table or not.
     *
     * @param db        Instance of SQLiteDatabase, which possibly contains given table.
     * @param tableName String name of table to check whether such table exists in database or not.
     * @return True, if given table exists in database recognized by given instance. False, otheriwse.
     * @author Ved Prakash Singh [ved@hike.in]
     */
    public static boolean isTableExists(SQLiteDatabase db, String tableName) {
        if ((tableName != null) && (db != null) && db.isOpen()) {
            Cursor c = null;
            try {
                c = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type=? AND name=?", new String[]{"table", tableName});
                if ((c != null) && c.moveToFirst()) {
                    return (c.getInt(0) > 0);
                }
            } catch (Exception e) {
                Logger.e("TableExistsCheck", "Erron while checking " + tableName + " exists...", e);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        } else {
            Logger.w("TableExistsCheck", "Can not check if " + tableName + " exists.");
        }

        return false;
    }

    public static JSONObject cloneJsonObject(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        String names[] = new String[jsonObject.length()];

        // get mapping keys
        Iterator<String> iterator = jsonObject.keys();
        int i = 0;
        while (iterator.hasNext()) {
            names[i++] = (String) iterator.next();
        }

        // create a new copy
        JSONObject cloneJson = null;
        try {
            cloneJson = new JSONObject(jsonObject, names);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return cloneJson;
    }

    public static void deleteFileFromHikeDir(Context context, File file, HikeFileType hikeFileType) {
        if (file.getPath().startsWith(getFileParent(hikeFileType, true))) {
            String[] retCol = new String[]{MediaStore.Video.Media._ID};
            Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            int id = HikeFile.getMediaId(file.getPath(), retCol, uri, context);
            if (id != -1) {
                context.getContentResolver().delete(ContentUris.withAppendedId(uri, id), null, null);
            }
            if (file.exists())
                file.delete();
        }
    }

    public static String getAppVersionName() {
        String appVersionName = "";
        try {
            appVersionName = HikeMessengerApp.getInstance().getApplicationContext().getPackageManager()
                    .getPackageInfo(HikeMessengerApp.getInstance().getApplicationContext().getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return appVersionName;
    }

    public static int getAppVersionCode() {
        int appVersionCode = Integer.MIN_VALUE;
        try {
            appVersionCode = HikeMessengerApp.getInstance().getApplicationContext().getPackageManager()
                    .getPackageInfo(HikeMessengerApp.getInstance().getApplicationContext().getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            appVersionCode = BuildConfig.VERSION_CODE;
        }

        return appVersionCode;
    }

    public static boolean showContactsUpdates(ContactInfo contactInfo) {
        if (isFavToFriendsMigrationAllowed()) {
            return contactInfo.isMyTwoWayFriend();
        } else {
            return ((contactInfo.getFavoriteType() == FavoriteType.FRIEND) ||
                    (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED) ||
                    (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED)) &&
                    (contactInfo.isOnhike());
        }
    }


    public static boolean isSelfMsisdn(String argMsisdn) {
        return getUserContactInfo(false).getMsisdn().equals(argMsisdn);
    }

    public static boolean appInstalledOrNot(Context context, String uri) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }

        return app_installed;
    }


    /**
     * Call this method to post a status update to timeline.
     *
     * @param status
     * @param moodId        : Pass -1 if no mood
     * @param imageFilePath : Path of the image on the client. Image should only be of jpeg format and compressed.
     *                      <p/>
     *                      Status = null, moodId < 0 & imageFilePath = null should not hold together
     *                      <p/>
     *                      List of moods:
     *                      {@link com.bsb.hike.utils.EmoticonConstants#moodMapping}
     */
    public static void postStatusUpdate(String status, int moodId, String imageFilePath) {
        if (TextUtils.isEmpty(status) && moodId < 0 && TextUtils.isEmpty(imageFilePath)) {
            Logger.e("Utils", "postStatusUpdate : status = null/empty, moodId < 0 & imageFilePath = null conditions hold together. Returning.");
            return;
        }

        StatusUpdateTask task = new StatusUpdateTask(status, moodId, imageFilePath);
        task.execute();
    }

    public static float currentBatteryLevel() {
        Context context = HikeMessengerApp.getInstance().getApplicationContext();
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float) scale;

        return batteryPct * 100;
    }

    public static String appendTokenInURL(String url) {
        if (!TextUtils.isEmpty(url)) {
            HikeSharedPreferenceUtil mmHikeSharedPreferenceUtil = HikeSharedPreferenceUtil.getInstance();
            url = url.replace("$reward_token", mmHikeSharedPreferenceUtil.getData(HikeMessengerApp.REWARDS_TOKEN, ""));
            url = url.replace("$msisdn", mmHikeSharedPreferenceUtil.getData(HikeMessengerApp.MSISDN_SETTING, ""));
            url = url.replace("$uid", mmHikeSharedPreferenceUtil.getData(HikeMessengerApp.UID_SETTING, ""));
            url = url.replace("$invite_token", mmHikeSharedPreferenceUtil.getData(HikeConstants.INVITE_TOKEN, ""));
            url = url.replace("$resId", Utils.getResolutionId() + "");
            url = url.replace("$platform_token", mmHikeSharedPreferenceUtil.getData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, ""));
            url = url.replace("$platform_uid", mmHikeSharedPreferenceUtil.getData(HikeMessengerApp.PLATFORM_UID_SETTING, ""));
        }

        return url;
    }

    public static String getCommaSeperatedStringFromArray(String[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        return sb.toString();
    }

    public static String getNewImagePostFilePath() {
        String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return directory + File.separator + Utils.getUniqueFilename(HikeFileType.IMAGE);
    }

    public static void sendFreeSms(String number) {
        Intent intent = IntentFactory.createChatThreadIntentFromMsisdn(HikeMessengerApp.getInstance(), number, true, false, ChatThreadActivity.ChatThreadOpenSources.STICKEY_CALLER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        HikeMessengerApp.getInstance().startActivity(intent);
    }

    public static boolean isOnHike(String number) {
        ContactInfo contactInfo = ContactManager.getInstance().getContactInfoFromPhoneNoOrMsisdn(number);
        if (contactInfo != null && contactInfo.isOnhike()) {
            return true;
        }

        return false;
    }

    /**
     * Determine whether a time-stamp represents correct clock time of a day.
     *
     * @param HH  Hour element of the day
     * @param mm  Minute element of the day
     * @param ss  Second element of the day
     * @param SSS MilliSecond element of the day
     * @return True, if given combination represents valid time of the day in 24 hours format. False, otherwise.
     * @author Ved Prakash Singh [ved@hike.in]
     */
    public static boolean isValidTimeStampOfTheDay(int HH, int mm, int ss, int SSS) {
        if ((HH < 0) || (HH >= 24)) {
            return false;
        }

        if ((mm < 0) || (mm >= 60)) {
            return false;
        }

        if ((ss < 0) || (ss >= 60)) {
            return false;
        }

        if ((SSS < 0) || (SSS >= 1000)) {
            return false;
        }

        return true;
    }

    /**
     * Get differential time logging upto nano second considering maximum significant time unit reference as second.
     *
     * @param start                       Start time of operation as long value
     * @param end                         End time of operation as long value
     * @param precisionOfTimeUnitInSecond Count of precision points in time unit per second for start and end parameters
     * @return Human-readable string of time logging.
     * @author Ved Prakash Singh [ved@hike.in]
     */
    public static String getExecutionTimeLog(long start, long end, int precisionOfTimeUnitInSecond) {
        StringBuilder timeLogBuilder = new StringBuilder();

        long diff = end - start;
        if (diff < 0) {
            Logger.wtf(ExecutionDurationLogger.TAG, "End time can not be less then start time.");
            diff = 0;
        }

        switch (precisionOfTimeUnitInSecond) {
            case ExecutionDurationLogger.PRECISION_UNIT_SECOND: {
                timeLogBuilder.append(diff).append(ExecutionDurationLogger.sec);
                break;
            }

            case ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND: {
                int unitInSecond = (int) Math.pow(10, ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND);
                long sec = diff / unitInSecond;
                timeLogBuilder.append(sec).append(ExecutionDurationLogger.sec).append(ExecutionDurationLogger.DELIMITER);
                long milliSec = diff - (sec * unitInSecond);
                timeLogBuilder.append(milliSec).append(ExecutionDurationLogger.ms);
                break;
            }

            case ExecutionDurationLogger.PRECISION_UNIT_MICRO_SECOND: {
                int unitInSecond = (int) Math.pow(10, ExecutionDurationLogger.PRECISION_UNIT_MICRO_SECOND);
                long sec = diff / unitInSecond;
                timeLogBuilder.append(sec).append(ExecutionDurationLogger.sec).append(ExecutionDurationLogger.DELIMITER);
                diff = diff - (sec * unitInSecond);
                int unitInMilliSecond = (int) Math.pow(10, (ExecutionDurationLogger.PRECISION_UNIT_MICRO_SECOND - ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND));
                long milliSec = diff / unitInMilliSecond;
                timeLogBuilder.append(milliSec).append(ExecutionDurationLogger.ms).append(ExecutionDurationLogger.DELIMITER);
                long microSec = diff - (milliSec * unitInMilliSecond);
                timeLogBuilder.append(microSec).append(ExecutionDurationLogger.μs);
                break;
            }

            case ExecutionDurationLogger.PRECISION_UNIT_NANO_SECOND: {
                int unitInSecond = (int) Math.pow(10, ExecutionDurationLogger.PRECISION_UNIT_NANO_SECOND);
                long sec = diff / unitInSecond;
                timeLogBuilder.append(sec).append(ExecutionDurationLogger.sec).append(ExecutionDurationLogger.DELIMITER);
                diff = diff - (sec * unitInSecond);
                int unitInMilliSecond = (int) Math.pow(10, (ExecutionDurationLogger.PRECISION_UNIT_NANO_SECOND - ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND));
                long milliSec = diff / unitInMilliSecond;
                timeLogBuilder.append(milliSec).append(ExecutionDurationLogger.ms).append(ExecutionDurationLogger.DELIMITER);
                diff = diff - (milliSec * unitInMilliSecond);
                int unitInMicroSecond = (int) Math.pow(10, (ExecutionDurationLogger.PRECISION_UNIT_NANO_SECOND - ExecutionDurationLogger.PRECISION_UNIT_MICRO_SECOND));
                long microSec = diff / unitInMicroSecond;
                timeLogBuilder.append(microSec).append(ExecutionDurationLogger.μs).append(ExecutionDurationLogger.DELIMITER);
                long nanoSec = diff - (microSec * unitInMicroSecond);
                timeLogBuilder.append(nanoSec).append(ExecutionDurationLogger.ns);
                break;
            }

            default: {
                Logger.w(ExecutionDurationLogger.TAG, "Unable to determine time units.");
            }
        }

        return timeLogBuilder.toString();
    }

    public static byte[] readBytes(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }


    public static TypedArray getDefaultAvatarBG() {
        if (HikeConstants.DEFAULT_AVATAR_BG_COLOR_ARRAY == null) {
            HikeConstants.DEFAULT_AVATAR_BG_COLOR_ARRAY = HikeMessengerApp.getInstance().getApplicationContext().getResources().obtainTypedArray(R.array.dp_bg);
        }
        return HikeConstants.DEFAULT_AVATAR_BG_COLOR_ARRAY;
    }

    /**
     * Call this method to find the total size of a folder
     *
     * @param folder
     * @return size of the folder in bytes
     */
    public static long folderSize(File folder) {
        long length = 0;

        // Precautionary check to prevent NPE from empty list files.
        // Saving folder.listFiles() in a temp array to avoid null pointer exception arising because of race condition
        File[] directory = folder.listFiles();
        if (directory == null)
            return length;
        for (File file : directory) {
            if (file.isDirectory())
                length += folderSize(file);
            else if (file.isFile())
                length += file.length();
        }
        return length;
    }

    /**
     * Call this method to get the total available internal storage space
     *
     * @return
     */
    public static double getFreeInternalStorage() {
        double internalSpace = 0.0;

        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        if (isJELLY_BEAN_MR2OrHigher()) {
            internalSpace = (double) stat.getBlockSizeLong() * (double) stat.getAvailableBlocksLong();
        } else {
            internalSpace = (double) stat.getBlockSize() * (double) stat.getAvailableBlocks();
        }
        double megsAvailable = internalSpace / (1024 * 1024);

        return megsAvailable;
    }

    /**
     * Utility method to rearrange chat and update the unread counter if needed
     *
     * @param destination       : Msisdn
     * @param rearrangeChat     : Whether to shift the chat up or not
     * @param updateUnreadCount : Whether to update the unread counter or not
     */
    public static void rearrangeChat(String destination, boolean rearrangeChat, boolean updateUnreadCount) {
        if (updateUnreadCount) {
            HikeConversationsDatabase convDb = HikeConversationsDatabase.getInstance();
            convDb.incrementUnreadCounter(destination);
            int unreadCount = convDb.getConvUnreadCount(destination);
            Message ms = Message.obtain();
            ms.arg1 = unreadCount;
            ms.obj = destination;
            HikeMessengerApp.getPubSub().publish(HikePubSub.CONV_UNREAD_COUNT_MODIFIED, ms);
        }

        if (rearrangeChat) {
            Pair<String, Long> pair = new Pair<String, Long>(destination, System.currentTimeMillis() / 1000);
            HikeMessengerApp.getPubSub().publish(HikePubSub.CONVERSATION_TS_UPDATED, pair);
        }
    }

    public static boolean isLocationEnabled(Context context) {
        LocationManager locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        boolean hasGps = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);

        if (!hasGps) {
            return false;
        } else if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return true;
        } else if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return false;
        }

        return false;

    }

    public static boolean isMarshmallowOrHigher() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    /**
     * returns true if the filePath starts with android data storage directory path
     *
     * @param filePath
     */
    public static boolean isAndroidDataStorageDir(String filePath) {
        boolean isAndroidDataStorageDir = false;
        if (TextUtils.isEmpty(filePath)) {
            isAndroidDataStorageDir = false;
        } else if (getExternalStorageState() == ExternalStorageState.WRITEABLE) {
            isAndroidDataStorageDir = filePath.startsWith(Environment.getExternalStorageDirectory() + ANDROID_DATA_STORAGE_DIR_SUFFIX);
        }
        return isAndroidDataStorageDir;
    }

    /*
     * Returns a interval added string from a given list
     */
    public static String listToString(List<String> list, String strInterVal) {
        if (list == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        boolean flag = false;
        for (String str : list) {
            if (flag) {
                result.append(strInterVal);
            }
            if (null == str) {
                str = "";
            }
            result.append(str);
            flag = true;
        }
        return result.toString();
    }


    public static Locale getCurrentLanguageLocale() {
        if (LocalLanguageUtils.isLocalLanguageSelected()) {
            return new Locale(LocalLanguageUtils.getApplicationLocalLanguageLocale());
        } else {
            return Locale.getDefault();
        }
    }

    public static JSONObject getDataBasedOnAppLanguage(String json) {

        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {


            JSONArray array = new JSONArray(json);

            if (array == null || array.length() <= 0) {
                return null;
            }

            String deviceLocale = LocalLanguageUtils.getApplicationLocalLanguageLocale();

            JSONObject data = null;

            for (int i = 0; i < array.length(); i++) {
                data = array.getJSONObject(i);

                if (data.optString(HikeConstants.LANGUAGE).equalsIgnoreCase(deviceLocale)) {
                    return data.getJSONObject(HikeConstants.DATA);
                }
            }
        } catch (JSONException e) {
            Logger.e("productpopup", "JSON Exception in JSON Array language");
        }
        return null;
    }

    public static void setLocalizationEnable(boolean enable) {
        if (!enable)
            LocalLanguageUtils.setApplicationLocalLanguage(LocalLanguage.PhoneLangauge, HikeConstants.APP_LANG_CHANGED_SERVER_SWITCH);
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.LOCALIZATION_ENABLED, enable);
    }

    public static String getInitialsFromContactName(String contactName) {
        if (contactName == null || TextUtils.isEmpty(contactName.trim())) {
            return "#";
        }

        contactName = contactName.trim();

        char first = contactName.charAt(0);

        if (Character.isLetter(first)) {
            return Character.toString(first);
        } else {
            return "#";
        }
    }

    /**
     * Sample logging JSON :
     * {"ek":"micro_app","event":"db_corrupt","fld1":"\/data\/data\/com.bsb.hike\/databases\/chats","fld4":"db_error","fld5":50880512 }
     *
     * @param dbObj
     */
    public static void recordDatabaseCorrupt(SQLiteDatabase dbObj) {
        JSONObject json = new JSONObject();
        try {

            json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
            json.put(AnalyticsConstants.EVENT, "db_corrupt");
            json.put(AnalyticsConstants.LOG_FIELD_1, dbObj.getPath());
            json.put(AnalyticsConstants.LOG_FIELD_4, "db_corrupt");
            json.put(AnalyticsConstants.LOG_FIELD_5, (new File(dbObj.getPath())).length());

            Logger.d("db", json.toString());

            HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.APP_CRASH_EVENT, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static boolean kptDictionaryDownloaded(Context context) {

        File hikeDir = context.getExternalFilesDir(null);
        File hikeLanguageDir = new File(hikeDir + HikeConstants.KPTConstants.KPT_LANGUAGE_DIR_ROOT);

        if (hikeLanguageDir != null && hikeLanguageDir.exists() && hikeLanguageDir.list() != null && hikeLanguageDir.list().length > 0) {

            return true;
        }

        return false;
    }

    public static void removeKptDictionaries(Context context) {

        File hikeDir = context.getExternalFilesDir(null);
        File hikeLanguageDir = new File(hikeDir + HikeConstants.KPTConstants.KPT_LANGUAGE_DIR_ROOT);

        if (hikeLanguageDir != null && hikeLanguageDir.exists()) {

            try {

                delete(hikeLanguageDir);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void delete(File file) throws IOException {

        if (file.isDirectory()) {

            //directory is empty, then delete it
            if (file.list().length == 0) {

                file.delete();
                System.out.println("Directory is deleted : "
                        + file.getAbsolutePath());

            } else {

                //list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);

                    //recursive delete
                    delete(fileDelete);
                }

                //check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    file.delete();
                    System.out.println("Directory is deleted : "
                            + file.getAbsolutePath());
                }
            }

        } else {
            //if file, then delete it
            file.delete();
            System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }

    // Use this method to encrypt a string
    public static String encrypt(String input) {
        if (input == null)
            return null;
        HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
        String key = settings.getData(HikeMessengerApp.MSISDN_SETTING, null);
        //for the case when AI packet will not send us the backup Token
        String salt = settings.getData(HikeMessengerApp.BACKUP_TOKEN_SETTING, null);
        AESEncryption aesObj = new AESEncryption(key + salt, "MD5");
        return aesObj.encrypt(input);
    }

    //Use this method to decrypt a string encrypted using the method above

    public static String decrypt(String input) {
        if (input == null)
            return null;
        HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
        String key = settings.getData(HikeMessengerApp.MSISDN_SETTING, null);
        //for the case when AI packet will not send us the backup Token
        String salt = settings.getData(HikeMessengerApp.BACKUP_TOKEN_SETTING, null);
        AESEncryption aesObj = new AESEncryption(key + salt, "MD5");
        return aesObj.decrypt(input);
    }

    /**
     * Method to return network type as short in descending order
     */
    public static short getNetworkShortinOrder(String networkType) {
        switch (networkType) {
            case "wifi":
                return 1;
            case "2g":
                return 4;
            case "3g":
                return 3;
            case "4g":
                return 2;
            case "off":
                return -1;
            case "unknown":
                return 5;
            default:
                return 0;
        }
    }

    public static long calculateDiskCacheSize(File dir) {
        long size = HikeConstants.MIN_DISK_CACHE_SIZE;

        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
            // Target 2% of the total space.
            size = available / 50;
        } catch (IllegalArgumentException ignored) {
        }

        // Bound inside min/max size for disk cache.
        size = Math.max(Math.min(size, HikeConstants.MAX_DISK_CACHE_SIZE), HikeConstants.MIN_DISK_CACHE_SIZE);
        size = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.DISK_CACHE_SIZE, -1L) != -1 ? HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.DISK_CACHE_SIZE, -1L) : size;
        return size;
    }

    public static int dpToPx(float dp) {
        return (int) (dp * Utils.densityMultiplier);
    }

    public static int spToPx(float sp) {
        return (int) (sp * Utils.scaledDensityMultiplier);
    }

    public static void deleteDiskCache() {
        deleteFile(new File(HikeMessengerApp.getInstance().getExternalFilesDir(null).getPath() + HikeConstants.DISK_CACHE_ROOT));
        HikeMessengerApp.clearDiskCache();
    }

    /**
     * This method checks whether we should connect to MQTT or not
     * Among the cases to check, there can be  : <br> 1. User's db was corrupt previously. 2. User is not signed up.
     * <p/>
     * In simple terms, we should connect to MQTT if Db is not corrupt and User is Signed up.
     *
     * @return
     */
    public static boolean shouldConnectToMQTT() {
        return (!isDBCorrupt()) && (isUserSignedUp(HikeMessengerApp.getInstance(), false));
    }

    public static boolean isDBCorrupt() {
        return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.DB_CORRUPT, false);
    }

    /**
     * WARNING : Do not ever call this method unless you have a valid reason to do so
     */
    public static void disconnectFromMQTT() {
        HikeMessengerApp.getInstance().getApplicationContext().sendBroadcast(new Intent(MqttConstants.MQTT_CONNECTION_CHECK_ACTION).putExtra("destroy", true));
    }

    public static void connectToMQTT() {
        HikeMqttManagerNew.getInstance().init(); // Init and then connect
        HikeMessengerApp.getInstance().getApplicationContext().sendBroadcast(new Intent(MqttConstants.MQTT_CONNECTION_CHECK_ACTION).putExtra("connect", true));
    }

    public static void makeFavFriendsTransition() {
        if (isFavToFriendsMigrationAllowed()) {
            changeFavToFriends();
        } else {
            revertFavToFriendsChange();
        }
    }

    public static void changeFavToFriends() {
        if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.FAVORITES_TO_FRIENDS_TRANSITION_STATE, 0) != 1) {
            Context context = HikeMessengerApp.getInstance().getApplicationContext();
            // Change last seen pref to friends if its is not already set to friends or noone.
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            String currentValue = settings.getString(HikeConstants.LAST_SEEN_PREF_LIST, context.getString(R.string.privacy_favorites));
            if (!currentValue.equals(context.getString(R.string.privacy_favorites))
                    && !currentValue.equals(context.getString(R.string.privacy_nobody))) {
                Editor settingEditor = settings.edit();
                settingEditor.putString(HikeConstants.LAST_SEEN_PREF_LIST, context.getString(R.string.privacy_favorites));
                HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.LAST_SEEN_TEMP_PREF, currentValue);
                int slectedPrivacyId = Integer.parseInt(context.getString(R.string.privacy_favorites));
                try {
                    HikePreferences.sendNLSToServer(slectedPrivacyId, true);
                    settingEditor.commit();
                    HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.FAVORITES_TO_FRIENDS_TRANSITION_STATE, 1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void revertFavToFriendsChange() {
        if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.FAVORITES_TO_FRIENDS_TRANSITION_STATE, 0) == 1) {
            Context context = HikeMessengerApp.getInstance().getApplicationContext();
            // Change last seen pref to friends if its is not already set to friends or noone.
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            String currentValue = settings.getString(HikeConstants.LAST_SEEN_PREF_LIST, context.getString(R.string.privacy_favorites));
            if (!currentValue.equals(context.getString(R.string.privacy_my_contacts))
                    && !currentValue.equals(context.getString(R.string.privacy_everyone))) {

                String oldLsValue = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.LAST_SEEN_TEMP_PREF, context.getString(R.string.privacy_my_contacts));
                Editor settingEditor = settings.edit();
                settingEditor.putString(HikeConstants.LAST_SEEN_PREF_LIST, oldLsValue);
                int slectedPrivacyId = Integer.parseInt(oldLsValue);
                try {
                    HikePreferences.sendNLSToServer(slectedPrivacyId, true);
                    settingEditor.commit();
                    HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.FAVORITES_TO_FRIENDS_TRANSITION_STATE, 0); //Resetting the flag, so that when the packet might be sent again, it is able to alter the prefs
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean isFavToFriendsMigrationAllowed() {
        return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.FAV_TO_FRIENDS_MIGRATION, false);
    }

    public static ConvMessage generateAddFriendSystemMessage(String msisdn, String message, boolean isOnHike, State state) {
        ConvMessage convMessage = makeConvMessage(msisdn, message, isOnHike, state);
        convMessage.setParticipantInfoState(ParticipantInfoState.FRIEND_REQUSET_STATUS);
        try {
            JSONObject obj = new JSONObject();
            obj.put(HikeConstants.TYPE, HikeConstants.FRIENDS_SYSTEM_MESSAGE);
            convMessage.setMetadata(obj);
        } catch (JSONException e) {
            Logger.wtf("Utils", "getting exception while creating convmessage : " + e.toString());
        }

        return convMessage;
    }

    public static boolean isNotMyOneWayFriend(ContactInfo contactInfo) {
        return Utils.isFavToFriendsMigrationAllowed() && !contactInfo.isMyOneWayFriend();
    }

    public static String getExternalFilesDirPath(String type) {
        File externalDir = HikeMessengerApp.getInstance().getExternalFilesDir(type);
        String externalDirPath = externalDir == null ? null : externalDir.getPath();
        Logger.d(TAG, "external dir path : " + externalDirPath);
        return externalDirPath;
    }

    public static boolean doesExternalDirExists() {
        boolean exists = !TextUtils.isEmpty(getExternalFilesDirPath(null));
        Logger.d(TAG, "external dir exists : " + exists);
        return exists;
    }

    public static boolean isSettingsBackupEnabled() {
        if (!Utils.isUserSignedUp(HikeMessengerApp.getInstance().getApplicationContext(), false)) {
            return false;
        }

        return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.ENABLE_CLOUD_SETTING_BACKUP, true);
    }

    private static String getCursorString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index != -1) return cursor.getString(index);
        return null;
    }


    public static PairModified<String, String> doesContactContainHikeCustomPhoneType(Context context, Uri contactUri, String oldName) {
        PairModified<String, String> returnContactIds = null;

        String mRawContactId, mDataId;
        Cursor mContactCursor = context.getContentResolver().query(contactUri, null, null, null, null);
        Logger.v("Contact", "Got Contact Cursor");

        try {
            if (mContactCursor.moveToFirst()) {
                String mContactId = getCursorString(mContactCursor,
                        ContactsContract.Contacts._ID);

                Cursor mRawContactCursor = context.getContentResolver().query(
                        RawContacts.CONTENT_URI,
                        null,
                        Data.CONTACT_ID + " = ?",
                        new String[]{mContactId},
                        null);

                Logger.v("RawContact", "Got RawContact Cursor");

                try {
                    ArrayList<String> mRawContactIds = new ArrayList<String>();
                    while (mRawContactCursor.moveToNext()) {
                        String rawId = getCursorString(mRawContactCursor, RawContacts._ID);
                        Logger.v("RawContact", "ID: " + rawId);
                        mRawContactIds.add(rawId);
                    }

                    for (String rawId : mRawContactIds) {
                        // Make sure the "last checked" RawContactId is set locally for use in insert & update.
                        mRawContactId = rawId;
                        Cursor mDataCursor = context.getContentResolver().query(
                                Data.CONTENT_URI,
                                null,
                                Data.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ? AND " + StructuredName.DISPLAY_NAME + " = ?",
                                new String[]{mRawContactId, StructuredName.CONTENT_ITEM_TYPE, oldName},
                                null);

                        if (mDataCursor.getCount() > 0) {
                            mDataCursor.moveToFirst();
                            mDataId = getCursorString(mDataCursor, Data._ID);
                            Logger.v("Data", "Found data item with MIMETYPE and Phone.TYPE");
                            mDataCursor.close();
                            returnContactIds = new PairModified<>(mRawContactId, mDataId);
                            break;
                        } else {
                            Logger.v("Data", "Data doesn't contain MIMETYPE and Phone.TYPE");
                            mDataCursor.close();
                        }
                        returnContactIds = null;
                    }
                } finally {
                    mRawContactCursor.close();
                }
            }
        } catch (Exception e) {
            Logger.w("UpdateContact", e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                Logger.w("UpdateContact", "\t" + ste.toString());
            }
            throw new RuntimeException();
        } finally {
            mContactCursor.close();
        }

        return returnContactIds;
    }


    public static PairModified<String, String> doesContactContainHikeCustomPhoneType(Context context, Uri contactUri) {
        PairModified<String, String> returnContactIds = null;

        String mRawContactId, mDataId;
        Cursor mContactCursor = context.getContentResolver().query(contactUri, null, null, null, null);
        Logger.v("Contact", "Got Contact Cursor");

        try {
            if (mContactCursor.moveToFirst()) {
                String mContactId = getCursorString(mContactCursor,
                        ContactsContract.Contacts._ID);

                Cursor mRawContactCursor = context.getContentResolver().query(
                        RawContacts.CONTENT_URI,
                        null,
                        Data.CONTACT_ID + " = ?",
                        new String[]{mContactId},
                        null);

                Logger.v("RawContact", "Got RawContact Cursor");

                try {
                    ArrayList<String> mRawContactIds = new ArrayList<String>();
                    while (mRawContactCursor.moveToNext()) {
                        String rawId = getCursorString(mRawContactCursor, RawContacts._ID);
                        Logger.v("RawContact", "ID: " + rawId);
                        mRawContactIds.add(rawId);
                    }

                    for (String rawId : mRawContactIds) {
                        // Make sure the "last checked" RawContactId is set locally for use in insert & update.
                        mRawContactId = rawId;
                        Cursor mDataCursor = context.getContentResolver().query(
                                Data.CONTENT_URI,
                                null,
                                Data.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ? AND " + Phone.TYPE + " = ? AND " + Phone.LABEL + " = ?",
                                new String[]{mRawContactId, Phone.CONTENT_ITEM_TYPE, String.valueOf(Phone.TYPE_CUSTOM), HikeConstants.HIKE_CUSTOM_PHONE_TYPE},
                                null);

                        if (mDataCursor.getCount() > 0) {
                            mDataCursor.moveToFirst();
                            mDataId = getCursorString(mDataCursor, Data._ID);
                            Logger.v("Data", "Found data item with MIMETYPE and Phone.TYPE");
                            mDataCursor.close();
                            returnContactIds = new PairModified<>(mRawContactId, mDataId);
                            break;
                        } else {
                            Logger.v("Data", "Data doesn't contain MIMETYPE and Phone.TYPE");
                            mDataCursor.close();
                        }
                        returnContactIds = null;
                    }
                } finally {
                    mRawContactCursor.close();
                }
            }
        } catch (Exception e) {
            Logger.w("UpdateContact", e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                Logger.w("UpdateContact", "\t" + ste.toString());
            }
            throw new RuntimeException();
        } finally {
            mContactCursor.close();
        }

        return returnContactIds;
    }

    public static void updateContactWithHikeCustomPhoneType(Context context, String mRawContactId, String mDataId, String msisdn) {
        try {
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + " = ?", new String[]{mRawContactId})
                    .withSelection(Data._ID + " = ?", new String[]{mDataId})
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, msisdn)
                    .withValue(Phone.TYPE, Phone.TYPE_CUSTOM)
                    .withValue(Phone.LABEL, HikeConstants.HIKE_CUSTOM_PHONE_TYPE)
                    .build());
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

        } catch (Exception e) {
            // Display warning
            Logger.w("UpdateContact", e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                Logger.w("UpdateContact", "\t" + ste.toString());
            }
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, "Update failed", duration);
            toast.show();
        }
    }

    /**
     * @param context
     * @param mRawContactId
     * @param mDataId
     * @param msisdn
     */
    public static void updateNameWithHikeCustomPhoneType(Context context, String mRawContactId, String mDataId, String name) {
        try {
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + " = ?", new String[]{mRawContactId})
                    .withSelection(Data._ID + " = ?", new String[]{mDataId})
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.DISPLAY_NAME, name)
                    .build());
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

        } catch (Exception e) {
            // Display warning
            Logger.w("UpdateContact", e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                Logger.w("UpdateContact", "\t" + ste.toString());
            }
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, "Update failed", duration);
            toast.show();
        }
    }

    /**
     * method to get a JSON for media click events. It assumes that the events for media differ only
     * in uniqueKey and order column
     *
     * @param uniqueKey
     * @param order
     * @return
     */
    public static JSONObject getCoreChatClickJSON(String uniqueKey, String species, boolean isStealth) {
        try {
            JSONObject json = new JSONObject();
            json.put(AnalyticsConstants.V2.UNIQUE_KEY, uniqueKey);
            json.put(AnalyticsConstants.V2.KINGDOM, ChatAnalyticConstants.ACT_CORE_LOGS);
            json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
            json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
            json.put(AnalyticsConstants.V2.ORDER, uniqueKey);
            json.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());
            json.put(AnalyticsConstants.V2.SPECIES, species);
            if (isStealth)
                json.put(AnalyticsConstants.V2.VARIETY, ChatAnalyticConstants.STEALTH_CHAT_THREAD);
            return json;

        } catch (JSONException e) {
            e.toString();
            return null;
        }
    }

    /**
     * This does a rename of entire folder (recursive) to new path.
     * Use this if old and new paths are on same mount point since its practically instantaneous.
     *
     * @param oldRootDir
     * @param newRootDir
     * @return true, if the operation was successful
     */
    public static boolean moveDirectoryByRename(File oldRootDir, File newRootDir) {
        boolean result = true;

        // param check
        if (oldRootDir == null || newRootDir == null) {
            return false;
        }

        if (!newRootDir.exists()) {
            result = result && newRootDir.mkdirs();
        }

        if (!oldRootDir.exists() || (oldRootDir.listFiles() == null)) {
            Logger.d("StickerMigration", "Migration unsuccessful but new folder created");
            return true; //Migration unsuccessful but new folder created
        }

        if (result) {
            for (File f : oldRootDir.listFiles()) {
                if (f.isDirectory()) {
                    File newDir = new File(newRootDir, f.getName());
                    result = result && moveDirectoryByRename(f, newDir);
                } else {
                    File newFile = new File(newRootDir, f.getName());
                    if (newFile.exists()) {
                        result = result && newFile.delete();
                    }
                    result = result && f.renameTo(newFile);
                }
            }

            // Delete residual dirs
            Utils.deleteFile(oldRootDir);
        }


        return result;
    }

    public static void recordCoreAnalyticsForShare(String uniqueKey_order, String species,
                                                   String toUser_msisdn, boolean isStealth, String genus, String family) {
        try {
            JSONObject json = new JSONObject();
            json.put(AnalyticsConstants.V2.UNIQUE_KEY, uniqueKey_order);
            json.put(AnalyticsConstants.V2.KINGDOM, ChatAnalyticConstants.ACT_CORE_LOGS);
            json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
            json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
            json.put(AnalyticsConstants.V2.ORDER, uniqueKey_order);
            json.put(AnalyticsConstants.V2.SPECIES, species);
            json.put(AnalyticsConstants.V2.TO_USER, toUser_msisdn);
            json.put(AnalyticsConstants.V2.NETWORK, Utils.getNetworkTypeAsString(
                    HikeMessengerApp.getInstance().getApplicationContext()));
            if (isStealth)
                json.put(AnalyticsConstants.V2.VARIETY, ChatAnalyticConstants.STEALTH_CHAT_THREAD);
            if (!TextUtils.isEmpty(genus))
                json.put(AnalyticsConstants.V2.GENUS, genus);
            if (!TextUtils.isEmpty(family))
                json.put(AnalyticsConstants.V2.FAMILY, family);

            HAManager.getInstance().recordV2(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void recordEventMaxSizeToastShown(String uniqueKey_order, String species, String toUser_msisdn, long fileSize) {
        try {
            JSONObject json = new JSONObject();
            json.put(AnalyticsConstants.V2.UNIQUE_KEY, uniqueKey_order);
            json.put(AnalyticsConstants.V2.KINGDOM, ChatAnalyticConstants.ACT_CORE_LOGS);
            json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
            json.put(AnalyticsConstants.V2.ORDER, uniqueKey_order);
            json.put(AnalyticsConstants.V2.SPECIES, species);
            json.put(AnalyticsConstants.V2.TO_USER, toUser_msisdn);
            json.put(AnalyticsConstants.V2.VAL_INT, fileSize);
            HAManager.getInstance().recordV2(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private static void fetchHistoricalUpdates(String msisdn) {
        RequestToken token = HttpRequests.getHistoricSUToken(msisdn, new IRequestListener() {
            @Override
            public void onRequestFailure(HttpException httpException) {

            }

            @Override
            public void onRequestSuccess(Response result) {

            }

            @Override
            public void onRequestProgressUpdate(float progress) {

            }
        });

        if (!token.isRequestRunning()) {
            token.execute();
        }
    }

    /**
     * Validate the hike root directory is corrupted or not. If it is corrupted then rename the corrupt dir.
     */
    public static void validateDirectory(String dirPath) {
        File rootDir = new File(dirPath);
		/*
		 * On re-install hike, sometimes the hike directory get corrupted and converted into a file. Due to which operation related to that directory stopped working.
		 * Renaming the corrupted hike directory and creating the new one to solve this issue.
		 * Caused mainly by app like clean master, native memory optimization etc.
		 */
        if (rootDir != null && rootDir.exists()) {
            if (!rootDir.isDirectory() && rootDir.isFile()) {
                int count = 0;
                File mFile = new File(dirPath + "_" + count);
                while (mFile.exists()) {
                    mFile = new File(dirPath + "_" + ++count);
                }
                rootDir.renameTo(mFile);
            }
        }
    }

    public static String formatDOB(String dobString) {
        if (TextUtils.isEmpty(dobString)) {
            return "";
        }

        Birthday dob = new Birthday(dobString);
        return String.format("%d/%d/%d", dob.day, dob.month, dob.year);
    }
}


