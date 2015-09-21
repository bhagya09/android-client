package com.kpt.adaptxt.beta.database;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.KPTAdaptxtTheme;

public class KPTAdaptxtDBHandler  /*implements KPTAdaptxtDBInterface*/{
	private String TAG = "KPTAdaptxtDBHandler";
	private SQLiteDatabase mDataBase;
	private Context mContext;
	private DBHelper dBHelper;
	private boolean mIsDefaultThemeModePrefUpdated;

	private static final String THEME_ADDON_DB = "kpt_theme_db.db";
	private static final String THEME_TABLE = "kpt_themes";
	private static final int DB_VERSION = 2;
	
	private static String THEME_ID = "theme_id";
	private static String THEME_NAME = "theme_name";
	private static String PACKAGE_NAME = "package_name";
	private static String THEME_TYPE = "theme_type";
	private static String THEME_ENABLED = "theme_enabled";
	private static String BASE_ID = "base_id";
	private static String CUSTOM_FONT_COLORS = "custom_theme_font_colors";
	private static String CUSTOM_KEY_SHAPES = "custom_theme_key_shape";
	private static String THEME_IMAGE = "theme_image";
	private static String CUSTOM_THEME_BG = "custom_theme_picture_bg_path";
	private static String CUSTOM_TRANPARENCY = "custom_tranparency";
	private static String CUSTOM_BG_PREFS = "custom_bg_prefs";

	private static final String CREATE_THEME_TABLE = "CREATE TABLE IF NOT EXISTS " + THEME_TABLE + " (" +
			THEME_ID 			+" INTEGER PRIMARY KEY AUTOINCREMENT, " +
			THEME_NAME 			+" VARBINARY(64) NOT NULL UNIQUE, " +
			PACKAGE_NAME 		+" VARCHAR2(64), " +
			THEME_TYPE 			+" INTEGER, " +
			THEME_ENABLED 		+" INTEGER, " +
			BASE_ID 			+" INTEGER, " +
			CUSTOM_FONT_COLORS 	+" VARCHAR2(64), " +
			CUSTOM_KEY_SHAPES 	+" INTEGERS, " +
			THEME_IMAGE 		+" BLOB NOT NULL, " +
			CUSTOM_THEME_BG 	+" VARCHAR(256), "+
			CUSTOM_TRANPARENCY	+" INTEGER, "+
			CUSTOM_BG_PREFS		+" INTEGER);";

	public KPTAdaptxtDBHandler(Context context) {
		//super(context, THEME_ADDON_DB, null, DB_VERSION);
		dBHelper = new DBHelper(context);

		mContext = context;
	}


	public KPTAdaptxtDBHandler open() throws SQLException 
	{
		mDataBase = dBHelper.getWritableDatabase();
		return this;
	}

	//---closes the database---    
	public void close() 
	{
		dBHelper.close();
	}



	/**
	 * Upload the database with Default themes for the first launch only.
	 */
	public void initDB(SQLiteDatabase db){


		TypedArray imgs = mContext.getResources().obtainTypedArray(R.array.kpt_theme_image_entries);
		String themeNames[] = mContext.getResources().getStringArray(R.array.kpt_theme_options_entries);

		try {
			for(int i = 0; i< themeNames.length; i++ ){
				SQLiteStatement setStatusStatement = db.compileStatement("INSERT INTO "+ 
						THEME_TABLE +" ("+THEME_ID+","+THEME_NAME+","+PACKAGE_NAME+","+THEME_TYPE+","+
						THEME_ENABLED+","+BASE_ID+","+CUSTOM_FONT_COLORS+","+CUSTOM_KEY_SHAPES+","+THEME_IMAGE+","+
						CUSTOM_TRANPARENCY+","+CUSTOM_BG_PREFS+")"+
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

				setStatusStatement.bindLong(1, i);
				setStatusStatement.bindString(2, themeNames[i]);
				setStatusStatement.bindString(3, mContext.getPackageName());
				setStatusStatement.bindLong(4, KPTConstants.THEME_INTERNAL);
				setStatusStatement.bindLong(5, KPTConstants.THEME_ENABLE);
				setStatusStatement.bindLong(6, -1);
				setStatusStatement.bindString(7, "");
				setStatusStatement.bindLong(8, -1);
				setStatusStatement.bindBlob(9, drawableToByteArray(mContext.getResources().getDrawable(imgs.getResourceId(i, -1))));
				setStatusStatement.bindLong(10, -1);
				setStatusStatement.bindLong(11, -1);
				
				setStatusStatement.execute();
			}
			//db.close();
		} catch (SQLiteException e) {
			//e.printStackTrace();
			//db.close();
		}
	}
	
	/**
	 * Updating the database with backup custom themes when adaptxt DB Version is changed.
	 */
	public void initDB(SQLiteDatabase db, Cursor cursor){


		
		try {
			if (cursor.moveToFirst()) {

				do{
					SQLiteStatement setStatusStatement = db.compileStatement("INSERT INTO "+ 
							THEME_TABLE +" ("+THEME_ID+","+THEME_NAME+","+PACKAGE_NAME+","+THEME_TYPE+","+
							THEME_ENABLED+","+BASE_ID+","+CUSTOM_FONT_COLORS+","+CUSTOM_KEY_SHAPES+","+THEME_IMAGE+","+
							CUSTOM_THEME_BG+","+CUSTOM_TRANPARENCY+","+CUSTOM_BG_PREFS+")"+
							"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					setStatusStatement.bindString(2, cursor.getString(1));
					setStatusStatement.bindString(3, cursor.getString(2));
					setStatusStatement.bindLong(4, cursor.getInt(3));
					setStatusStatement.bindLong(5, cursor.getInt(4));
					if(cursor.getInt(5) > 2){
						setStatusStatement.bindLong(6, (cursor.getInt(5) + 2));
					}else{
						setStatusStatement.bindLong(6, cursor.getInt(5));
					}
					
					if(cursor.getInt(3) == KPTConstants.THEME_CUSTOM){
						setStatusStatement.bindString(7,cursor.getString(6));
					}else{
						setStatusStatement.bindString(7,"");
					}
					setStatusStatement.bindLong(8, cursor.getInt(7));
					setStatusStatement.bindBlob(9, cursor.getBlob(8));
					if(cursor.getString(9)== null){
						setStatusStatement.bindString(10, "null;null");
					}else{
					setStatusStatement.bindString(10, cursor.getString(9));
					}
					
					setStatusStatement.bindLong(11, cursor.getInt(10));
					setStatusStatement.bindLong(12, cursor.getInt(11));
					setStatusStatement.execute();
				}while(cursor.moveToNext());
			}
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
	}
	
	private boolean checkThemsisExist(Context context, String pkgName, boolean isMatched, SharedPreferences sharedPrefs) {
		if (pkgName == null) {
			return false;
		}
		// TODO Auto-generated method stub
		PackageManager pm = context.getPackageManager();
		ArrayList<PackageInfo> pInfoList = (ArrayList<PackageInfo>) pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
		for(int i=0;i<pInfoList.size();i++) {
			if(pInfoList.get(i).packageName.contains(KPTAdaptxtTheme.THEME_PACKAGE_NAME)) {
				if (pkgName.equalsIgnoreCase(pInfoList.get(i).packageName)) {
					return true;
				}
			}
		}
		
			//not in device 
		if (isMatched) {
			//set default theme preferemce 
			mIsDefaultThemeModePrefUpdated = true;
			sharedPrefs.edit().putString(KPTConstants.PREF_THEME_MODE, KPTConstants.BRIGHT_THEME+"").commit();
		}
		
		return false;
	}
	
	
	/**
	 * Updating the xml from database with backup custom themes when adaptxt DB Version is changed.
	 */
	public void initXMLfromDB(Context context){

		try {
			ArrayList<String>  theme_names = getAllThemeNames();//getAllCustomThemeNames();

			if(theme_names != null){
				File directory = new File(Environment.getExternalStorageDirectory()+File.separator+KPTConstants.BACKUP_FOLDER);
				directory.mkdirs();
				String destXMLfilepath = directory +KPTConstants.BACKUP_THEME_XML_FILE;

				File destXMLfile = new File(destXMLfilepath);
				try {
					destXMLfile.createNewFile();
				} catch (Exception e1) {
					e1.printStackTrace();                        
				}
				try{
					Document xmlDoc = createDocument();
					Element root = xmlDoc.createElement(THEME_TABLE);
					xmlDoc.appendChild(root);
					Cursor cursor;
					for (int i = 0; i <  theme_names.size(); i++) {

						cursor = mDataBase.rawQuery(
								"SELECT * FROM " + THEME_TABLE
								+ " WHERE "+THEME_NAME +"= \'" + theme_names.get(i)+"\'", null);
						cursor.moveToFirst();
						
						try {
							String themeType = cursor.getString(cursor.getColumnIndex(THEME_TYPE));
							if(themeType != null){
								int type = Integer.parseInt(themeType);
								if(type == KPTConstants.THEME_INTERNAL) {
									//Log.e("kpt","this is an internal theme so continue");
									continue;
								}
							}
						}catch(Exception e) {
							Log.e("kpt","Failed while getting the theme type");
						}
						
						Element theme = xmlDoc.createElement(THEME_NAME);
						root.appendChild(theme);

						Attr attr = xmlDoc.createAttribute("name");
						attr.setValue(theme_names.get(i));
						theme.setAttributeNode(attr);

						Element themeId = xmlDoc.createElement(THEME_ID);
						if(cursor.getString(cursor.getColumnIndex(THEME_ID))!=null){
							themeId.appendChild(xmlDoc.createTextNode(cursor.getString(cursor.getColumnIndex(THEME_ID))));
						}
						theme.appendChild(themeId);

						Element themePackageName = xmlDoc.createElement(PACKAGE_NAME);
						if(cursor.getString(cursor.getColumnIndex(PACKAGE_NAME))!=null){
							if(cursor.getString(cursor.getColumnIndex(PACKAGE_NAME)).equalsIgnoreCase(KPTConstants.APP_PACKAGE_NAME)){
								themePackageName.appendChild(xmlDoc.createTextNode("com.kpt.adaptxt.premium"));
							}else{
								themePackageName.appendChild(xmlDoc.createTextNode(cursor.getString(cursor.getColumnIndex(PACKAGE_NAME))));
							}
						}
						theme.appendChild(themePackageName);

						Element themeType = xmlDoc.createElement(THEME_TYPE);
						if(cursor.getString(cursor.getColumnIndex(THEME_TYPE))!=null){
							themeType.appendChild(xmlDoc.createTextNode(cursor.getString(cursor.getColumnIndex(THEME_TYPE))));
						}
						theme.appendChild(themeType);

						Element themeEnabled = xmlDoc.createElement(THEME_ENABLED);
						if(cursor.getString(cursor.getColumnIndex(THEME_ENABLED))!=null){
							themeEnabled.appendChild(xmlDoc.createTextNode(cursor.getString(cursor.getColumnIndex(THEME_ENABLED))));
						}
						theme.appendChild(themeEnabled);

						Element baseId = xmlDoc.createElement(BASE_ID);
						if(cursor.getString(cursor.getColumnIndex(BASE_ID))!=null){
							baseId.appendChild(xmlDoc.createTextNode(cursor.getString(cursor.getColumnIndex(BASE_ID))));
						}
						theme.appendChild(baseId);

						Element themeFontColors = xmlDoc.createElement(CUSTOM_FONT_COLORS);
						if(cursor.getString(cursor.getColumnIndex(CUSTOM_FONT_COLORS))!=null){
							themeFontColors.appendChild(xmlDoc.createTextNode(cursor.getString(cursor.getColumnIndex(CUSTOM_FONT_COLORS))));
						}
						theme.appendChild(themeFontColors);

						Element themeKeyShape = xmlDoc.createElement(CUSTOM_KEY_SHAPES);
						if(cursor.getString(cursor.getColumnIndex(CUSTOM_KEY_SHAPES))!=null){
							themeKeyShape.appendChild(xmlDoc.createTextNode(cursor.getString(cursor.getColumnIndex(CUSTOM_KEY_SHAPES))));
						}
						theme.appendChild(themeKeyShape);

						Element themeImage = xmlDoc.createElement(THEME_IMAGE);
						byte[] image =cursor.getBlob(cursor.getColumnIndex(THEME_IMAGE));
						BitmapFactory.Options options = new BitmapFactory.Options();
						Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length, options);

						String path = Environment.getExternalStorageDirectory()+File.separator+KPTConstants.BACKUP_FOLDER;
						File file = new File(path, theme_names.get(i)+".png");
						OutputStream fOut = new FileOutputStream(file);
						bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
						fOut.flush();
						fOut.close();
						themeImage.appendChild(xmlDoc.createTextNode(theme_names.get(i)+".png"));
						theme.appendChild(themeImage);

						Element themeBgPath = xmlDoc.createElement(CUSTOM_THEME_BG);
						if(cursor.getString(cursor.getColumnIndex(CUSTOM_THEME_BG))!=null){
							themeBgPath.appendChild(xmlDoc.createTextNode(cursor.getString(cursor.getColumnIndex(CUSTOM_THEME_BG))));
						}
						theme.appendChild(themeBgPath);

						Element themeTransperency = xmlDoc.createElement(CUSTOM_TRANPARENCY);
						if(cursor.getString(cursor.getColumnIndex(CUSTOM_TRANPARENCY))!=null){
							themeTransperency.appendChild(xmlDoc.createTextNode(cursor.getString(cursor.getColumnIndex(CUSTOM_TRANPARENCY))));
						}
						theme.appendChild(themeTransperency);

						Element themeBgPrefs = xmlDoc.createElement(CUSTOM_BG_PREFS);
						if(cursor.getString(cursor.getColumnIndex(CUSTOM_BG_PREFS))!=null){
							themeBgPrefs.appendChild(xmlDoc.createTextNode(cursor.getString(cursor.getColumnIndex(CUSTOM_BG_PREFS))));
						}
						theme.appendChild(themeBgPrefs);
					}
					commitChangesInDestXML(xmlDoc,destXMLfile);

				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}catch (SQLiteException e) {
				e.printStackTrace();
			}
		
		//restoreThemesIntoDB(context);
	}
	
	//Committing all the changes into destination color xml file.
	private static void commitChangesInDestXML(Document xmlDoc,File destXMLfile) throws TransformerException{
		
		 TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(xmlDoc);
			StreamResult result = new StreamResult(destXMLfile);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.transform(source, result);
		
	}
	//creating document object for xml file.
	private static Document createDocument() throws ParserConfigurationException, IOException{
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		Document doc = docBuilder.newDocument();
		
		return doc;
	}
	
	//creating document object for xml file.
	private static Document parseDocument(File filepath) throws ParserConfigurationException, IOException{
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		Document doc = null;
		try {
			doc = docBuilder.parse(filepath);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return doc;
	}
	
	public static byte[] drawableToByteArray(Drawable d) {

		if (d != null) {
			Bitmap imageBitmap = ((BitmapDrawable) d).getBitmap();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
			byte[] byteData = baos.toByteArray();
			return byteData;
		} else
			return null;

	}

	/**
	 * Retrieves themes values for a particular themeID.
	 * @param themeId
	 * @return
	 */
	public KPTThemeItem getThemeValues(int themeId) {
		//mDataBase = dBHelper.getWritableDatabase();
		KPTThemeItem theme = new KPTThemeItem();
		try {
			Cursor cursor = mDataBase.rawQuery(
					"SELECT * FROM " + THEME_TABLE
					+ " WHERE "+THEME_ID +"= " + themeId, null);
			if (cursor.moveToFirst()) {

				theme.themeID = cursor.getInt(0);
				theme.themeName = cursor.getString(1);
				theme.packageName = cursor.getString(2);
				theme.themeType = cursor.getInt(3);
				theme.themeEnabled = cursor.getInt(4);
				theme.baseId = cursor.getInt(5);
				theme.customThemeFontColors = cursor.getString(6);
				theme.customThemeKeyShape = cursor.getInt(7);
				theme.themeImage = cursor.getBlob(8);
				theme.customThemeBGPath = cursor.getString(9);
				theme.customTransparency = cursor.getInt(10);
				theme.customBGPrefs = cursor.getInt(11);

				cursor.close();
			}
		} catch (CursorIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		return theme;
	}

	/**
	 * Populates the list of all theme names
	 * @return list of theme names for irrespective theme type
	 */
	public ArrayList<String> getAllThemeNames(){

		ArrayList<String> themeNames = new ArrayList<String>();

		//mDataBase = dBHelper.getWritableDatabase();

		try{
			Cursor cursorInternal = mDataBase.rawQuery(
					"SELECT "+THEME_NAME+" FROM " + THEME_TABLE + " WHERE "+THEME_TYPE+" = " + KPTConstants.THEME_INTERNAL, null);
			/*Cursor cursorExternal = mDataBase.rawQuery(
					"SELECT "+THEME_NAME+" FROM " + THEME_TABLE + " WHERE "+THEME_TYPE+" = " + KPTConstants.THEME_EXTERNAL, null);
			Cursor cursorCustom = mDataBase.rawQuery(
					"SELECT "+THEME_NAME+" FROM " + THEME_TABLE + " WHERE "+THEME_TYPE+" = " + KPTConstants.THEME_CUSTOM, null);*/

			if (cursorInternal.moveToFirst()) {

				do{
					themeNames.add(cursorInternal.getString(0));
				}while(cursorInternal.moveToNext());
			}

			/*if (cursorExternal.moveToFirst()) {

				do{
					themeNames.add(cursorExternal.getString(0));
				}while(cursorExternal.moveToNext());
			}

			if (cursorCustom.moveToFirst()) {

				do{
					themeNames.add(cursorCustom.getString(0));
				}while(cursorCustom.moveToNext());
			}*/
			//mDataBase.close();

		}catch (SQLException e) {
			//mDataBase.close();
		}
		return themeNames;
	}

	public ArrayList<String> getAllCustomThemeNames(){
		ArrayList<String> themeNames = new ArrayList<String>();
		//mDataBase = dBHelper.getReadableDatabase();
		try{
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+THEME_NAME+" FROM " + THEME_TABLE	+ " WHERE "+THEME_TYPE+" = " + KPTConstants.THEME_CUSTOM, null);
			if (cursor.moveToFirst()) {
				do{
					themeNames.add(cursor.getString(0));
				}while(cursor.moveToNext());
			}
		}catch (SQLException e) {
		}
		return themeNames;
	}
	
	
	public Drawable[] getAllThemeDrawable(){
		Drawable[] drawable_list = null;
		//String whereDelete = PACKAGE_NAME+" = '"+ themePackageName+"'";
		String whereClause = " WHERE "+THEME_ENABLED+" = "+KPTConstants.THEME_ENABLE;
		//mDataBase = dBHelper.getReadableDatabase();
		try {
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+THEME_IMAGE+" FROM " + THEME_TABLE+whereClause, null);
			drawable_list = new Drawable[cursor.getCount()];
			if (cursor != null) {
				cursor.moveToFirst();
				for(int i = 0; i <cursor.getCount() ; i++){ 
					drawable_list[i] = byteToDrawable(cursor.getBlob(0));
					cursor.moveToNext();
				}
				cursor.close(); 
			}


		} catch (CursorIndexOutOfBoundsException e) {
			e.printStackTrace();
			//mDataBase.close();
		}

		return drawable_list;
	}
	
	/**
	 * Populates the list of all enabled theme names.
	 * @param themeEnabled
	 * @return list of theme names for enabled theme
	 */
	public ArrayList<String> getThemeNames(int themeEnabled){
		ArrayList<String> themeNames = new ArrayList<String>();

		//mDataBase = dBHelper.getWritableDatabase();
		try{
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+THEME_NAME+" FROM " + THEME_TABLE
					+ " WHERE "+THEME_ENABLED+" = " + themeEnabled, null);
			if (cursor.moveToFirst()) {

				do{
					themeNames.add(cursor.getString(0));

				}while(cursor.moveToNext());
			}
			cursor.close();

		}catch (SQLException e) {
			
		}
		return themeNames;
	}

	/**
	 * Populates the list of External Themes.
	 * @param 
	 * @return list of external theme names 
	 */
	public ArrayList<String> getExternalThemeNames(){
		ArrayList<String> themeNames = new ArrayList<String>();
		
		if(!mDataBase.isOpen()){
			mDataBase = dBHelper.getReadableDatabase();
		}
		//mDataBase = dBHelper.getReadableDatabase();
		
		try{
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+THEME_NAME+" FROM " + THEME_TABLE
							+ " WHERE "+THEME_TYPE+" = "+KPTConstants.THEME_EXTERNAL  , null);
			if (cursor.moveToFirst()) {
				
				do{
					themeNames.add(cursor.getString(0));
					
				}while(cursor.moveToNext());
			}
			//mDataBase.close();
			
		}catch (SQLException e) {
			//mDataBase.close();
		}
		return themeNames;
	}

	public boolean isCurrentThemeDeleted(int themeTobeDeletedId, int currentTheme) {
		String whereClause = " WHERE "+BASE_ID+" = "+themeTobeDeletedId;
		
		try{
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+THEME_ID+" FROM " + THEME_TABLE +whereClause , null);
			if (cursor.moveToFirst()) {
				
				do{
					if(currentTheme == Integer.parseInt(cursor.getString(0))){
						return true;
					}
					
				}while(cursor.moveToNext());
			}
			
			
		}catch (SQLException e) {
			
		}
		
		return false;
	}
	
	/**
	 * Returns all the External(Addon) Themes installed.
	 * @return
	 */
	public ArrayList<KPTThemeItem> getAllExternalThemes(){
		ArrayList<KPTThemeItem> externalThemes = new ArrayList<KPTThemeItem>();

		//mDataBase = dBHelper.getReadableDatabase();

		try{
			Cursor cursor = mDataBase.rawQuery(
					"SELECT * FROM " + THEME_TABLE
					+ " WHERE "+THEME_TYPE+" = " + KPTConstants.THEME_EXTERNAL, null);
			if (cursor.moveToFirst()) {

				do{
					KPTThemeItem themeItem = new KPTThemeItem();
					themeItem.setThemeID(cursor.getInt(0));
					themeItem.setThemeName(cursor.getString(1));
					themeItem.setPackageName(cursor.getString(2));
					themeItem.setThemeType(cursor.getInt(3));
					themeItem.setThemeEnabled(cursor.getInt(4));
					themeItem.setBaseId(cursor.getInt(5));
					themeItem.setCustomThemeFontColors(cursor.getString(6));
					themeItem.setCustomThemeKeyShape(cursor.getInt(7));
					themeItem.setThemeImage(cursor.getBlob(8));
					themeItem.setCustomThemeBGPath(cursor.getString(9));
					themeItem.setCustomTransparency(cursor.getInt(10));
					themeItem.setCustomBGPrefs(cursor.getInt(11));

					externalThemes.add(themeItem);
				}while(cursor.moveToNext());
			}
			//mDataBase.close();

		}catch (SQLException e) {
			//mDataBase.close();
		}

		return externalThemes;
	}

	/**
	 * Returns all the External(Addon) Theme package names installed.
	 * @return
	 */
	public ArrayList<String> getAllExternalThemePackageNames(){
		ArrayList<String> externalThemePackages = new ArrayList<String>();

		try{
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+PACKAGE_NAME +" FROM " + THEME_TABLE
					+ " WHERE "+THEME_TYPE+" = " + KPTConstants.THEME_EXTERNAL, null);
			
			if (cursor.moveToFirst()) {
				do{
					externalThemePackages.add(cursor.getString(cursor.getColumnIndex(PACKAGE_NAME)));
				}while(cursor.moveToNext());
			}			
		}catch (SQLException e) {
			
		}
		return externalThemePackages;
	}
	
	
	/**
	 * Insert the new theme details
	 * @param themeItem
	 * @return
	 */
	public long insertTheme(KPTThemeItem themeItem){
		long rowId = -1;
		if (mContext == null) {
			return rowId;
		}
		if (themeItem == null) {
			return rowId;
		}
		//mDataBase = dBHelper.getWritableDatabase();
		try{
			Context context = mContext.createPackageContext(themeItem.getPackageName(),0);
			ContentValues themeValues = new ContentValues(); 

			//themeValues.put("theme_id", newRowId);
			themeValues.put(THEME_NAME, themeItem.getThemeName());
			themeValues.put(PACKAGE_NAME, themeItem.getPackageName());
			themeValues.put(THEME_TYPE, themeItem.getThemeType());
			themeValues.put(THEME_ENABLED, themeItem.getThemeEnabled());
			themeValues.put(BASE_ID, themeItem.getBaseId()); 
			themeValues.put(CUSTOM_FONT_COLORS, themeItem.getCustomThemeFontColors());
			themeValues.put(CUSTOM_KEY_SHAPES, themeItem.getCustomThemeKeyShape());

			int id = context.getResources().getIdentifier("image_full_rcube", "drawable", context.getPackageName());
			themeValues.put(THEME_IMAGE, drawableToByteArray(context.getResources().getDrawable(id)));

			themeValues.put(CUSTOM_THEME_BG,themeItem.getCustomThemeBGPath());
			themeValues.put(CUSTOM_TRANPARENCY, themeItem.getCustomTransparency());
			themeValues.put(CUSTOM_BG_PREFS, themeItem.getCustomBGPrefs());

			rowId = mDataBase.insert(THEME_TABLE, null, themeValues);

		}catch (SQLException e) {
			e.printStackTrace();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}

		return rowId;
	}

	/**
	 * Updates the themes changes
	 * @param themeId
	 * @param themeItem
	 * @return number of rows deleted
	 */
	public int  updateThemeValues(int themeId, KPTThemeItem themeItem){
		int rowsEffected = 0;
		//mDataBase = dBHelper.getWritableDatabase();


		String WHERE_CLAUSE = "theme_id = "+themeId; 

		try{
			ContentValues themeValues = new ContentValues();

			if(themeItem.getThemeName() != null){
				themeValues.put(THEME_NAME, themeItem.getThemeName());
			}


			if(themeItem.getThemeEnabled() >  -1){
				themeValues.put(THEME_ENABLED, themeItem.getThemeEnabled());
			}

			if(themeItem.getCustomThemeFontColors() != null){
				themeValues.put(CUSTOM_FONT_COLORS, themeItem.getCustomThemeFontColors());
			}
			if(themeItem.getThemeType() == KPTConstants.THEME_CUSTOM){
				themeValues.put(CUSTOM_KEY_SHAPES, themeItem.getCustomThemeKeyShape());
			}

			if(themeItem.getCustomThemeBGPath() != null){
				themeValues.put(CUSTOM_THEME_BG, themeItem.getCustomThemeBGPath());
			}

			if(themeItem.getThemeImage() != null){
				themeValues.put(THEME_IMAGE, themeItem.getThemeImage());
			}

			if(themeItem.getCustomTransparency() > -1){
				themeValues.put(CUSTOM_TRANPARENCY, themeItem.getCustomTransparency());
			}
			
			if(themeItem.getCustomBGPrefs() > -1){
				themeValues.put(CUSTOM_BG_PREFS, themeItem.getCustomBGPrefs());
			}

			rowsEffected = mDataBase.update(THEME_TABLE, themeValues, WHERE_CLAUSE, null);
			
			//mDataBase.close();
		}catch(SQLException sqlException){
			//mDataBase.close();
			sqlException.printStackTrace();
		}

		return rowsEffected;
	}
	
	public String getCurThemeName(int currentThemeId) {
		String name = null;
		try{
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+THEME_NAME+" FROM " + THEME_TABLE
					+ " WHERE "+THEME_ID+" = " + currentThemeId, null);
			
			if(cursor.moveToFirst()){
				name = cursor.getString(0);
			}
		}catch (SQLException e) {
			e.printStackTrace();
		}
		return name;
	}
	
	public int getThemeType(String themeName) {
		int themeType = 4;
		try{
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+THEME_TYPE+" FROM " + THEME_TABLE
					+ " WHERE "+THEME_NAME+" = '"+ themeName.trim()+"'", null);
			
			if(cursor.moveToFirst()){
				themeType = cursor.getInt(0);
			}else{
				themeType = 5;
			}
		}catch (SQLException e) {
			e.printStackTrace();
		}
		return themeType;
	}

	
	/**
	 * Deletes the specified theme using the package name.
	 * @param themeId
	 * @return number of rows deleted
	 */
	public int deleteTheme(int themeId){
		int rowsEffected = 0;
		//mDataBase = dBHelper.getWritableDatabase();
		String whereDelete = THEME_ID+" = "+themeId+" OR "+BASE_ID+" = "+themeId;

		try{
			rowsEffected = mDataBase.delete(THEME_TABLE, whereDelete, null);

			//mDataBase.close();
		}catch (SQLException e) {
			//mDataBase.close();
		}

		return rowsEffected;
	}
	
	public int deleteCustomThemes(String themeName){
		int rowsEffected = 0;
		int deleteRowId = -1;
		String whereDelete = THEME_NAME+" = '"+ themeName+"'";
		try{
			Cursor cursor = mDataBase.rawQuery("SELECT "+THEME_ID+" FROM "+ THEME_TABLE+" WHERE "+whereDelete, null);
			if(cursor.moveToFirst()){
				deleteRowId = cursor.getInt(0);
				rowsEffected = deleteTheme(deleteRowId);
			}

		}catch (SQLException e) {
			e.printStackTrace();
		}
		return rowsEffected;
	}
	
	public int getIDFromPackage(String themePackageName){
		int deleteRowId = -1;
		String wherePackage = PACKAGE_NAME+" = '"+ themePackageName+"'";


		try{
			Cursor cursor = mDataBase.rawQuery("SELECT "+THEME_ID+" FROM "+ THEME_TABLE+" WHERE "+wherePackage, null);
			if(cursor.moveToFirst()){
				deleteRowId = cursor.getInt(0);
			}
			
		}catch (SQLException e) {
			//e.printStackTrace();
		}

		return deleteRowId;
	}

	/**
	 * Deletes the specified theme using the package name.
	 * Used in Package Listener service 
	 * @param themePackageName
	 * @return number of rows deleted
	 */
	public int deleteTheme(String themePackageName){
		int rowsEffected = 0;
		int deleteRowId = -1;
		String whereDelete = PACKAGE_NAME+" = '"+ themePackageName+"'";


		try{
			Cursor cursor = mDataBase.rawQuery("SELECT "+THEME_ID+" FROM "+ THEME_TABLE+" WHERE "+whereDelete, null);
			if(cursor.moveToFirst()){
				deleteRowId = cursor.getInt(0);
				rowsEffected = deleteTheme(deleteRowId);
			}
			//rowsEffected = mDataBase.delete(THEME_TABLE, whereDelete, null);

			//mDataBase.close();
		}catch (SQLException e) {
			//mDataBase.close();
			e.printStackTrace();
		}

		return rowsEffected;
	}

	/**
	 * Gets the current saved number of themes 
	 * @return count of themes saved in DB
	 */
	public int getNumberOfThemes(){
		int themeCount = 0;
		//mDataBase = dBHelper.getReadableDatabase();
		try {
			Cursor cursor = mDataBase.rawQuery(
					"SELECT * FROM " + THEME_TABLE, null);
			if (cursor != null) {
				cursor.moveToFirst(); 
				themeCount = cursor.getCount(); 
				cursor.close(); 
			}
			//mDataBase.close();
		} catch (CursorIndexOutOfBoundsException e) {
			e.printStackTrace();
			//mDataBase.close();
		}
		return themeCount;
	}

	public int[] getAllThemeIDs(){ 
		int arr[] = null;
		//mDataBase = dBHelper.getReadableDatabase();
		try {
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+THEME_ID+" FROM " + THEME_TABLE, null);
			arr = new int[cursor.getCount()];
			if (cursor != null) {
				cursor.moveToFirst();
				for(int i = 0; i <cursor.getCount() ; i++){ 
					arr[i] = cursor.getInt(0);
					cursor.moveToNext();
				}
				cursor.close(); 
			}
			//mDataBase.close();

		} catch (CursorIndexOutOfBoundsException e) {
			//e.printStackTrace();
			//mDataBase.close();
		}
		return arr ;
	}

	public static Drawable byteToDrawable(byte[] data) {

		if (data == null)
			return null;
		else
			return new BitmapDrawable(BitmapFactory.decodeByteArray(data, 0, data.length));
	}
	
	public String getThemePackageName(String themeName) {
		String packName = "";
		//mDataBase = dBHelper.getReadableDatabase();
		try {
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+PACKAGE_NAME+" FROM " + THEME_TABLE + " WHERE "+THEME_NAME+" = " +"'"+themeName+"'", null);
			if (cursor != null) {
				cursor.moveToFirst();
				packName = cursor.getString(0);
				cursor.close(); 
			}
			//mDataBase.close();
			
		} catch (CursorIndexOutOfBoundsException e) {
			e.printStackTrace();
			//mDataBase.close();
		}
		return packName ;
	}
	
	
	public int getThemeId(String themeName) {
		int themeId = -1;
		

		/*byte[] iso8859sequence;
		try {
			iso8859sequence = themeName.getBytes("UTF-8");
			themeName = new String(iso8859sequence, "US-ASCII")	;
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		try {
			Cursor cursor = mDataBase.rawQuery(
					"SELECT "+THEME_ID+" FROM " + THEME_TABLE + " WHERE "+THEME_NAME+" = '"+ themeName+"'", null);
			if (cursor != null) {
				cursor.moveToFirst();
				themeId = cursor.getInt(0);
				cursor.close(); 
			}
			//mDataBase.close();
			
		} catch (CursorIndexOutOfBoundsException e) {
			e.printStackTrace();
			//mDataBase.close();
		}

		return themeId;
	}

	



	public class DBHelper extends SQLiteOpenHelper{
		
		public DBHelper(Context context) {
			super(context, THEME_ADDON_DB, null, DB_VERSION);

			mContext = context;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL(CREATE_THEME_TABLE);
			initDB(db);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
	
			Cursor previousInstalledThemesCursor = db.rawQuery(
					"SELECT * FROM " + THEME_TABLE
					+ " WHERE "+THEME_TYPE +"!= " + KPTConstants.THEME_INTERNAL, null);
			
			String deleteSQL = "DELETE FROM " + THEME_TABLE+";";
			String dropSQL = "DROP TABLE " + THEME_TABLE+";";
			
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
			int currentThemeId = Integer.parseInt(pref.getString(KPTConstants.PREF_THEME_MODE, 0+""));
			

			
			if(previousInstalledThemesCursor!= null && previousInstalledThemesCursor.getCount()== 0){
				db.execSQL(deleteSQL);
				initDB(db);
			}else{
				db.execSQL(dropSQL);
				db.execSQL(CREATE_THEME_TABLE);
				initDB(db);
				initDB(db,previousInstalledThemesCursor);
				
				//After downloading the theme making it as default theme. 
				if(currentThemeId > 2){
					SharedPreferences pref1 = PreferenceManager.getDefaultSharedPreferences(mContext);
					Editor prefEdit = pref1.edit(); 
					prefEdit.putString(KPTConstants.PREF_THEME_MODE, ""+(currentThemeId+2));
					prefEdit.commit();
				}
			}
			
			
			
		}

	}

}
