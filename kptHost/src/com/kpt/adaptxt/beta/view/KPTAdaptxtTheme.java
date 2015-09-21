package com.kpt.adaptxt.beta.view;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.util.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

/**
 * Class used to hold the respective drawables according to the theme selected
 * by the user. Drawables where defined will be redirected to this class where
 * the respective variable would be holding the reference for the source.
 * 
 * @author nikhil
 * 
 */

public class KPTAdaptxtTheme {

	public static final String XML_FILE_NAME = "themes_phone.xml";
	public static final String XML_PARENT_TAG = "themes";
	public static final String XML_THEME_RESOURSES_TAG = "theme_resourses";
	public static final String XML_CONTENT_ID = "id";
	public static final String XML_CONTENT_NAME = "name";

	public static final String THEME_PACKAGE_NAME = "com.kpt.adaptxt.addon.theme.noneed";
	
	private Context mContext;
	private Resources mResources;
	private static Context mThemeContext;

	public int mPrimaryTextColor;
	public int mSecondaryTextColor;
	public int mPreviewTextColor;
	public int mCandidateFontColor;
	public int mCandidateFontColorRecommended;

	public int mGlideTracePathColor;
	
	public int mSuggestionbarOpen;
	public int mSuggestionbarClose;

	public int mKeyboardBGPort;
	public int mKeyboardBGLand;
	//public int mMiniKeyboardBG;
	
	public int mCandidateSuggBG;
	public int mCandidateSuggDivider;
	public int mCandidateFeedback;
	public int mCandidatePrivateColor;
	//public int mCandidatePrivateModeRTL;
	
	public int mKeyBackground;
	public int mPopupKeyBackground;
	public int mKeyFeedbackBg;
	public int mKeyFeedbackBgMore;

	public int mSpaceKey;
	//public int mSpacePreview;
	public int mSpaceHighlight;
	public int mSpaceKeyIcon;
	
	public int mAdaptxtSettingsKey;
	public int mAdaptxtSettingsPreView;
	
	public int mCandidateDeleteWordFeedback;
	public int mAccDismissFeedback;
	
	public int mXiEnable;
	public int mXiDisable;
	
	public int mCustomKeyShapeColor;
	
	public int mPopupKeyBoardBG;
	
	private SharedPreferences sPref;
	 
	//--------------------- Start
	public static final String FONT_DEFAULT = "default.ttf"; //this is the default, for name sake putting.ttf extension
	
	public static final String FONT_AVG = "Gothic.TTF";
	public static final String FONT_BELTER = "Belter.ttf";
	public static final String FONT_AMER = "Americana.otf"; //"Typewriter.TTF";
	public static final String FONT_HUMANA = "Humana.otf";
	public static final String FONT_JOKERMAN = "Jokerman.ttf";
	//public static final String FONT_INSCRIPTION = "Inscription.ttf";
	public static final String FONT_SERPENTINE = "Serpentine.ttf";
	public static final String FONT_AMERICANA = "Americana.otf";
	
	public static final String FONT_CUSTOM = "adp.ttf";
	
	private Typeface mKBCustUserSelectedFontTypeface;
	//-------End
	
	
	public static final int ORIENTATION_LANDSCAPE = 0;
	public static final int ORIENTATION_PORTRAIT = 1;

	//-------Theme board related variables
    public static final int COLOR_PRIMARY_CHAR = 0;
	public static final int COLOR_SECONDARY_CHAR = 1;
	public static final int COLOR_KEYBOARD_BG = 2;
	public static final int COLOR_KEY_BG = 3;
	public static final int COLOR_SUGGESTION = 4;
	public static final int COLOR_SUGGESTION_BG = 5;

	public static final String DELIMITER = ";";
	
	public int[] mCustomThemeColors = new int[6];
	//public String[] currentCustomBGPaths = new String[2];

	public boolean isCustomizationInProgress = false;
	public static int mTotalCustomThemesCreated = 0;
	
    private Typeface mCustomFontTypeface;
	//---------
	
    
    //----------- Key shape related variable
	public static final int KEY_SHAPE_1 = 0;
	public static final int KEY_SHAPE_2 = 1;
	public static final int KEY_SHAPE_3 = 2;
	public static final int KEY_SHAPE_4 = 3;
	public static final int KEY_SHAPE_5 = 4;
	public static final int KEY_SHAPE_6 = 5;
	//private static final String TAG = "KPTAdaptxtTheme";
	
	//private KPTThemeItem kptThemeItem;
	
	public int currentThemeID;
	public String currentThemeName;
	public String currentThemePkgName;
	public int currentThemeType;
	public int currentThemeEnabled;
	public String currentCustomBGPath;
	public int customKeyShape;
	public int currentThemeBaseID;
	
	public int getCurrentThemeBaseID() {
		return currentThemeBaseID;
	}

	public void setCurrentThemeBaseID(int currentThemeBaseID) {
		this.currentThemeBaseID = currentThemeBaseID;
	}

	public int getCurrentThemeID() {
		return currentThemeID;
	}

	public void setCurrentThemeID(int currentThemeID) {
		this.currentThemeID = currentThemeID;
	}

	public String getCurrentThemeName() {
		return currentThemeName;
	}

	public void setCurrentThemeName(String currentThemeName) {
		this.currentThemeName = currentThemeName;
	}

	public String getCurrentThemePkgName() {
		return currentThemePkgName;
	}

	public void setCurrentThemePkgName(String currentThemePkgName) {
		this.currentThemePkgName = currentThemePkgName;
	}

	public int getCurrentThemeType() {
		return currentThemeType;
	}

	public void setCurrentThemeType(int currentThemeType) {
		this.currentThemeType = currentThemeType;
	}

	public int getCurrentThemeEnabled() {
		return currentThemeEnabled;
	}

	public void setCurrentThemeEnabled(int currentThemeEnabled) {
		this.currentThemeEnabled = currentThemeEnabled;
	}

	public String getCurrentCustomBGPath() {
		return currentCustomBGPath;
	}

	public void setCurrentCustomBGPath(String currentCustomBGPath) {
		this.currentCustomBGPath = currentCustomBGPath;
	}




	// Tag names in the theme.xml file. These are the tag names represents the
	// resource
	private enum key_atributes {
		key_background, preview_background, primary_text_color, secondary_text_color, 
		preview_text_color, space_key, space_highlight, keyboard_bg_port,
		keyboard_bg_land, key_feedback, key_feedback_more, candidate_sugg_bg, 
		suggestion_divider, candidate_color, candidaterecommended_color, kpt_keyboard_adaptxt_settings_feedback,
		candidate_background_feedback, candidate_private_color, space_key_icon, kpt_keyboard_adaptxt_settings,
		candidate_delete_word_feedback, acc_dismiss, suggest_cheveron_open, suggest_cheveron_close,
		xi_enable, xi_disable, custom_keyshape_default_color, pop_kb_bg, glide_trace_color
	};

	public Resources getResources(){
		return mThemeContext.getResources();
	}

	public KPTAdaptxtTheme(Context context, int theme) {
		mContext = context;
		mResources = context.getResources();
		
		loadTheme(context, theme);
		sPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		mCustomFontTypeface = Utility.getTypeface(FONT_CUSTOM, context);//Typeface.createFromAsset(context.getResources().getAssets(), FONT_CUSTOM);
		
		String fontName = sPref.getString(KPTConstants.PREF_CUST_FONT_STYLE, FONT_DEFAULT);
		setUserSelectedFontTypeface(fontName);
	}

	/*public void loadCurrentThemeValues(int themeId) {
		kptThemeItem = themeDB.getThemeValues(themeId);
		
		setCurrentThemeID(kptThemeItem.getThemeID());
		setCurrentThemeName(kptThemeItem.getThemeName());
		setCurrentThemeType(kptThemeItem.getThemeType());
		setCurrentThemePkgName(kptThemeItem.getPackageName());
		setCurrentThemeEnabled(kptThemeItem.getThemeEnabled());
		setCurrentCustomBGPath(kptThemeItem.getCustomThemeBGPath());
		setCurrentThemeBaseID(kptThemeItem.getBaseId());
		
	}*/
	
	public void loadTheme(Context context, int themeId) {
		sPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		
		InputStream inputStream = null;
		//loadCurrentThemeValues(themeId);
		mThemeContext = /*createContext(themeId);*/context;
		if (mThemeContext == null) {
			return;
		}
		mResources = mThemeContext.getResources();

		try {
			AssetManager am = mContext.getAssets();
			inputStream = am.open(KPTConstants.ATX_ASSETS_FOLDER+XML_FILE_NAME);
              
			XmlPullParserFactory xmlfactory = XmlPullParserFactory
					.newInstance();
			XmlPullParser parser = xmlfactory.newPullParser();
			parser.setInput(inputStream, null);

			boolean parsingDone = false;
			try {
				int eventType = parser.getEventType();

				do {
					switch (eventType) {
					case XmlPullParser.START_TAG:
						if (parser.getName() != null
								&& parser.getName().equals(
										XML_THEME_RESOURSES_TAG)) {
							int themeType = Integer.parseInt(parser.getAttributeValue(null, XML_CONTENT_ID));

							if (themeType == themeId) {
								parser.nextTag();
								parsingDone = parseInnerTags(parser);
								
							} else {
								skipInnerTags(parser);
							}
						}

						break;
					case XmlPullParser.END_TAG:
						if (parser.getName() != null
								&& parser.getName().equals(XML_PARENT_TAG)) {
							parsingDone = true;
						}
						break;
					}
					eventType = parser.next();

				} while (!parsingDone
						&& eventType != XmlPullParser.END_DOCUMENT);
				

			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Parse the inner tags for the parent.
	 * 
	 * @param parser
	 * @return
	 */
	private boolean parseInnerTags(XmlPullParser parser) {
		boolean temp = false;
		try {

			int eventType = parser.getEventType();
			mPopupKeyBoardBG = 0;

			do {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					String text = parser.getName();
					if (text == null) {
						break;
					}
					
					switch (key_atributes.valueOf(text)) {
					case key_background:
						mKeyBackground = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
					case preview_background:
						mPopupKeyBackground = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
					case primary_text_color:
						mPrimaryTextColor = mResources.getIdentifier(parser.nextText(), "color", mThemeContext.getPackageName());
						break;
					case secondary_text_color:
						mSecondaryTextColor = mResources.getIdentifier(parser.nextText(), "color", mThemeContext.getPackageName());
						break;
					case preview_text_color:
						mPreviewTextColor = mResources.getIdentifier(parser.nextText(), "color", mThemeContext.getPackageName());
						break;

					case space_key:
						mSpaceKey = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
					/*case space_preview:
						mSpacePreview = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;*/
					case space_highlight:
						mSpaceHighlight = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
						
					case kpt_keyboard_adaptxt_settings:
						mAdaptxtSettingsKey = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
					
					case keyboard_bg_port:
						mKeyboardBGPort = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
					case keyboard_bg_land:
						mKeyboardBGLand = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
					
					case key_feedback:
						mKeyFeedbackBg = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
					case key_feedback_more:
						mKeyFeedbackBgMore = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
					case candidate_sugg_bg:
						mCandidateSuggBG = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;

					case suggestion_divider:
						mCandidateSuggDivider = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;

					case candidate_color:
						mCandidateFontColor = mResources.getIdentifier(parser.nextText(), "color", mThemeContext.getPackageName());
						break;

					case candidaterecommended_color:
						mCandidateFontColorRecommended = mResources.getIdentifier(parser.nextText(), "color", mThemeContext.getPackageName());
						break;

					case kpt_keyboard_adaptxt_settings_feedback:
						mAdaptxtSettingsPreView = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;

					case candidate_background_feedback:
						mCandidateFeedback = mResources.getIdentifier(parser.nextText(), "drawable",mThemeContext.getPackageName());
						break;
					
					case candidate_private_color:
						mCandidatePrivateColor = mResources.getIdentifier(parser.nextText(), "color",mThemeContext.getPackageName());
						break;
					
					case space_key_icon:
						mSpaceKeyIcon = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
						
					case candidate_delete_word_feedback:
						mCandidateDeleteWordFeedback = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
						
					case acc_dismiss:
						mAccDismissFeedback = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
						
					case suggest_cheveron_close:
						mSuggestionbarClose = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
						
					case suggest_cheveron_open:
						mSuggestionbarOpen = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
						
					case xi_enable:
						mXiEnable = mResources.getIdentifier(parser.nextText(), "color", mThemeContext.getPackageName());
						break;
						
					case xi_disable:
						mXiDisable = mResources.getIdentifier(parser.nextText(), "color", mThemeContext.getPackageName());
						break;
						
					case custom_keyshape_default_color:
						mCustomKeyShapeColor = mResources.getIdentifier(parser.nextText(), "color", mThemeContext.getPackageName());
						break;
						
					case pop_kb_bg:
						mPopupKeyBoardBG = mResources.getIdentifier(parser.nextText(), "drawable", mThemeContext.getPackageName());
						break;
						
					case glide_trace_color:
						mGlideTracePathColor = mResources.getIdentifier(parser.nextText(), "color", mThemeContext.getPackageName());
						break;
					default:
						break;
					}

					break;
				case XmlPullParser.END_TAG:
					//count = 0;
					if (parser.getName() != null
							&& parser.getName().equals(XML_THEME_RESOURSES_TAG)) {
						temp = true;
					}
					break;
				}

				eventType = parser.nextTag();

			} while (eventType != XmlPullParser.END_DOCUMENT);

		} catch (Exception e) {
			//e.printStackTrace();
		}

		return temp;

	}

	/**
	 * Skip the rest of the theme related tags which are not needed
	 * 
	 * @param parser
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private void skipInnerTags(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		int event;
		while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (event == XmlPullParser.END_TAG && parser.getName() != null
					&& parser.getName().equals(XML_THEME_RESOURSES_TAG)) {
				break;
			}
		}
	}

	
	/**
	 * these are the custom values modified by the user.
	 * These values are used by drawing classes like keyboardview, candidate view
	 * to apply the custom colors
	 * 
	 * @param color
	 * @param whatToChange
	 */
	public void setCustomThemeValues(int color, int whatToChange) {
		switch (whatToChange) {
		case COLOR_PRIMARY_CHAR:
			mCustomThemeColors[COLOR_PRIMARY_CHAR] = color;
			break;
		case COLOR_SECONDARY_CHAR:
			mCustomThemeColors[COLOR_SECONDARY_CHAR] = color;
			break;
		case COLOR_KEY_BG:
			mCustomThemeColors[COLOR_KEY_BG] = color;
			break;
		case COLOR_KEYBOARD_BG:
			mCustomThemeColors[COLOR_KEYBOARD_BG] = color;
			break;
		case COLOR_SUGGESTION:
			mCustomThemeColors[COLOR_SUGGESTION] = color;
			break;
		case COLOR_SUGGESTION_BG:
			mCustomThemeColors[COLOR_SUGGESTION_BG] = color;
			break;
		}
	}

	/*public String getCustomBGPath(int orientation){
		return currentCustomBGPaths[orientation];
	}*/
	
	public int getColorValueOf(int valueOfWhat) {
		return mCustomThemeColors[valueOfWhat];
	}

	/**
	 * This is the custom typeface used to drawing custom theme texts.
	 * The ttf file is in the assets
	 *   
	 * @return custom typeface
	 */
	public Typeface getCustomFontTypeface() {
		return mCustomFontTypeface;
	}
	
	public Context getThemeContext(){
		return mThemeContext;
	}
	
	public void setUserSelectedFontTypeface(String fontName) {	
		if(fontName == null ) {
			mKBCustUserSelectedFontTypeface = Typeface.DEFAULT; 
		} else {
			if(fontName.equals(FONT_DEFAULT)) {
				mKBCustUserSelectedFontTypeface = Typeface.DEFAULT;
			} else {
				try {
					//fix for 6_3 to 7_2 update release
					if("Humana.TTF".equals(fontName)) {
						fontName = FONT_HUMANA;
					}
					if("Belter.otf".equals(fontName)) {
						fontName = FONT_BELTER;
					}
					mKBCustUserSelectedFontTypeface =Utility.getTypeface("fonts/" + fontName, mContext);// Typeface.createFromAsset(mContext.getResources().getAssets(), "fonts/" + fontName);
				}catch(Exception e) {
					mKBCustUserSelectedFontTypeface = Typeface.DEFAULT;
					//e.printStackTrace();
				}
			}
		}
	}
	
	public Typeface getUserSelectedFontTypeface() {
		return mKBCustUserSelectedFontTypeface;
	}
	
}
