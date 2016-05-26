package com.bsb.hike.productpopup;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.platform.ContentModules.PlatformContentModel;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.productpopup.ProductPopupsConstants.PopupStateEnum;
import com.bsb.hike.productpopup.ProductPopupsConstants.PopupTriggerPoints;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import static com.bsb.hike.db.DBConstants.HIKE_CONTENT.END_TIME;
import static com.bsb.hike.db.DBConstants.HIKE_CONTENT.POPUPDATA;
import static com.bsb.hike.db.DBConstants.HIKE_CONTENT.START_TIME;
import static com.bsb.hike.db.DBConstants.HIKE_CONTENT.TRIGGER_POINT;

/**
 * 
 * @author himanshu
 * 
 *         The Manager to ProductPopus.This call handles when the popups need to be shown ,all interactions with the ProductPopup DB.
 */
public class ProductInfoManager
{
	// Handler that is running on the backend thread.

	private final Handler handler = new Handler(HikeHandlerUtil.getInstance().getLooper());

	private static final ProductInfoManager mmProductInfoMgr = new ProductInfoManager();

	// <TRIGGER_POINT,ARRAYLIST<POPUPS>
	private SparseArray<ArrayList<ProductContentModel>> mmSparseArray = new SparseArray<ArrayList<ProductContentModel>>();

	Context context = null;

	// Singleton class
	private ProductInfoManager()
	{
		context = HikeMessengerApp.getInstance().getApplicationContext();

	}

	// To be called once in on create of HikeMessangerApp.
	public void init()
	{
		handler.post(new Runnable()
		{

			@Override
			public void run()
			{
				mmSparseArray = HikeContentDatabase.getInstance().getAllPopup();
			}
		});

	}

	public static ProductInfoManager getInstance()
	{
		return mmProductInfoMgr;
	}

	/**
	 * 
	 * @param val
	 * @param iShowPopup
	 *            -Listener to return the result on the Calling Activity
	 * 
	 *            Logic :this method check taht if there is any popup on a particular trigger point ...
	 * 
	 *            We get the Arraylist containing of all the popups for the corresponding trigger point.We sort it according to the start time.Then we check the popup with the
	 *            highest start time whose endtime is also valid ..And we delete all the popups with the lower start time ..and we do nothing for the popups whose start time is
	 *            greater that the Present start time(Future Popus)
	 * 
	 * 
	 */
	public void isThereAnyPopup(Integer val, IActivityPopup iShowPopup)
	{
		ProductContentModel mmModel = null;
		long presentTime = System.currentTimeMillis();
		ArrayList<ProductContentModel> popUpToBeDeleted=null;
		if (mmSparseArray.get(val) != null && !mmSparseArray.get(val).isEmpty())
		{
			popUpToBeDeleted = new ArrayList<ProductContentModel>();
			ArrayList<ProductContentModel> mmArray = mmSparseArray.get(val);
			Collections.sort(mmArray, ProductContentModel.ProductContentStartTimeComp);
			for (ProductContentModel productContentModel : mmArray)
			{
				if (productContentModel.getStarttime() <= presentTime)
				{
					if (productContentModel.getEndtime() >= presentTime && mmModel == null)
					{
						mmModel = productContentModel;
						Logger.d("ProductPopup", productContentModel.getTriggerpoint() + ">>>>>" + productContentModel.getStarttime() + ">>>>>>" + productContentModel.getEndtime());
					}
					else
					{
						popUpToBeDeleted.add(productContentModel);
					}
				}
			}
		}

		deletePopups(popUpToBeDeleted);

		if (mmModel != null)
		{
			parseAndShowPopup(mmModel, iShowPopup);
		}
		else
		{
			iShowPopup.onFailure();
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);
	}

	/**
	 * 
	 * @param mmArrayList
	 * 
	 *            Deleting all the Popups from the Database and form the memory
	 */
	public void deletePopups(final ArrayList<ProductContentModel> mmArrayList)
	{

		// Deleting all the things on Backend thread;
		handler.post(new Runnable()
		{

			@Override
			public void run()
			{
				if (mmArrayList != null && !mmArrayList.isEmpty())
				{
					
					String[] hashCode = new String[mmArrayList.size()];
					int length = 0;
					for (ProductContentModel a : mmArrayList)
					{
						hashCode[length++] = a.hashCode() + "";
                    }
					Logger.d("ProductPopup",hashCode.toString());
					HikeContentDatabase.getInstance().deletePopup(hashCode);
					int triggerPoint = (mmArrayList.get(0).getTriggerpoint());
					Logger.d("ProductPopup", "start deleting" + mmArrayList.get(0).getTriggerpoint() + "<<<<<");

					if (mmSparseArray.get(triggerPoint) != null)
						mmSparseArray.get(triggerPoint).removeAll(mmArrayList);

					Logger.d("ProductPopup", "End deleting" + mmArrayList.toString());
					HikeMessengerApp.getPubSub().publish(HikePubSub.PRODUCT_POPUP_BADGE_COUNT_CHANGED, null);
				}
			}
		});
		
	}
	
	public static void recordPopupEvent(String appName, String pid, boolean isFullScreen, String type)
	{
		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(ProductPopupsConstants.APP_NAME, appName);
			metadata.put(ProductPopupsConstants.PID, pid);
			metadata.put(ProductPopupsConstants.IS_FULL_SCREEN, isFullScreen);
			metadata.put(HikeConstants.STATUS, type);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, ProductPopupsConstants.PRODUCT_POP_UP, metadata);

		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param productContentModel
	 * @param iShowPopup
	 *            -Listener to return the result to the calling activity
	 * 
	 *            This method is responsible for downloading the zip if not present and then mustaching the template,validating the data
	 */
	public void parseAndShowPopup(final ProductContentModel productContentModel, final IActivityPopup iShowPopup)
	{

		PlatformContent.getContent(HikePlatformConstants.PlatformBotType.ONE_TIME_POPUPS,productContentModel.toJSONString(), new PopupContentListener(productContentModel, iShowPopup)
		{
			ProductContentModel productContentModel = null;

			@Override
			public void onEventOccured(int uniqueId,EventCode event)
			{
				productContentModel = getProductContentModel();
			    switch (event)
				{
				case LOW_CONNECTIVITY:
					HikeContentDatabase.getInstance().updatePopupStatus(productContentModel.hashCode(), PopupStateEnum.NOT_DOWNLOADED.ordinal());
					if (getListener() != null)
					{
						getListener().onFailure();
					}
					break;
				case INVALID_DATA:
                case INCOMPLETE_ZIP_DOWNLOAD:
				case STORAGE_FULL:

					ArrayList<ProductContentModel> mmArrayList = new ArrayList<ProductContentModel>();
					mmArrayList.add(productContentModel);
					deletePopups(mmArrayList);
					if (getListener() != null)
					{
						getListener().onFailure();
					}
					break;
				default:

					break;
				}
			}

			@Override
			public void onComplete(PlatformContentModel content)
			{
				productContentModel = getProductContentModel();

				if (getListener() != null)
				{
					productContentModel.setFormedData(content.getFormedData());
					getListener().onSuccess(productContentModel);
					if (productContentModel.isCancellable())
					{
						ArrayList<ProductContentModel> list = new ArrayList<>(1);
						list.add(productContentModel);
						deletePopups(list);
					}

				}
				else
				{
					handlePushScenarios(productContentModel);
					if (mmSparseArray.get(productContentModel.getTriggerpoint()) != null)
					{
						ArrayList<ProductContentModel> mmArrayList = mmSparseArray.get(productContentModel.getTriggerpoint());
						mmArrayList.add(productContentModel);
					}
					else
					{
						ArrayList<ProductContentModel> mmArrayList = new ArrayList<ProductContentModel>();
						mmArrayList.add(productContentModel);
						mmSparseArray.put(productContentModel.getTriggerpoint(), mmArrayList);
					}
					HikeContentDatabase.getInstance().updatePopupStatus(productContentModel.hashCode(), PopupStateEnum.DOWNLOADED.ordinal());
					HikeMessengerApp.getPubSub().publish(HikePubSub.PRODUCT_POPUP_RECEIVE_COMPLETE, null);
                    recordPopupEvent(productContentModel.getAppName(), productContentModel.getPid(), productContentModel.isFullScreen(), HikeConstants.DOWNLOAD);
				}
					HikeMessengerApp.getPubSub().publish(HikePubSub.PRODUCT_POPUP_BADGE_COUNT_CHANGED, null);
			}
				
		});

	}

	private void handlePushScenarios(ProductContentModel productContentModel)
	{
		if (productContentModel.isPushReceived())
		{

			Intent intent = new Intent();
			intent.putExtra(ProductPopupsConstants.USER, productContentModel.getUser());
			intent.putExtra(ProductPopupsConstants.NOTIFICATION_TITLE, productContentModel.getNotifTitle());
			intent.putExtra(ProductPopupsConstants.PUSH_SOUND, productContentModel.shouldPlaySound());
			intent.putExtra(ProductPopupsConstants.TRIGGER_POINT, productContentModel.getTriggerpoint());
			if (productContentModel.isPushFuture())
			{
				
				Logger.d("ProductPopup","Future Push Received"+productContentModel.getPushTime()+"......."+ System.currentTimeMillis());
				HikeAlarmManager.setAlarmWithIntent(HikeMessengerApp.getInstance().getApplicationContext(),productContentModel.getPushTime(),
						HikeAlarmManager.REQUESTCODE_PRODUCT_POPUP, true, intent);
			}
			else
			{	
				NotificationContentModel mmNotificationContentModel=new NotificationContentModel(productContentModel.getNotifTitle(), productContentModel.getUser(), productContentModel.shouldPlaySound(), productContentModel.getTriggerpoint());
				ProductInfoManager.getInstance().notifyUser(mmNotificationContentModel);
			}
		}
	}

	/**
	 * 
	 * @param metaData
	 *            of the Json
	 * 
	 *            Saving the popup.
	 */
	public void parsePopupPacket(JSONObject metaData)
	{

		Logger.d("ProductPopup", "Popup received Going to inserti in DB");
		
		// saving the Popup in DataBase:
		ProductContentModel productContentModel = ProductContentModel.makeProductContentModel(metaData);

		recordPopupEvent(productContentModel.getAppName(), productContentModel.getPid(), productContentModel.isFullScreen(), ProductPopupsConstants.RECEIVED);

		if(!HikeContentDatabase.getInstance().isDuplicatePopup(productContentModel.getPid()))
		{
			HikeContentDatabase.getInstance().savePopup(productContentModel, PopupStateEnum.NOT_DOWNLOADED.ordinal());

			parseAndShowPopup(productContentModel, null);
		}
		else
		{
			Logger.d("ProductPopup", "Popup received is duplicate with pid :" + productContentModel.getPid());
		}
	}

	/**
	 * 
	 * @param intent
	 * 
	 *            Send a push to the user
	 */
	public void notifyUser(NotificationContentModel notificationContentModel)
	{
		String title = notificationContentModel.getTitle();
		String user = notificationContentModel.getUser();
		boolean shouldPlaySound = notificationContentModel.isShouldPlaySound();
		int triggerpoint = notificationContentModel.getTriggerpoint();

		if (triggerpoint == PopupTriggerPoints.CHAT_SCR.ordinal())
		{
			HikeNotification.getInstance().sendNotificationToChatThread(user, title, !shouldPlaySound);

		}
		else
		{
			HikeNotification.getInstance().notifyUserAndOpenHomeActivity(user, title, !shouldPlaySound);
		}
	}

	/**
	 * 
	 * @param mmModel
	 * @return
	 * 
	 *         Creating a Dialog Pojo ...Utility method
	 */
	public DialogPojo getDialogPojo(ProductContentModel mmModel)
	{
		DialogPojo mmPojo = new DialogPojo(mmModel.isFullScreen(), mmModel.getHeight(), mmModel.getFormedData(), mmModel);
		return mmPojo;
	}

	/**
	 * function to get the host from the Url.
	 * @param metaData
	 * @return
	 */
	public String getFormedUrl(String metaData)
	{
		try
		{
			JSONObject mmObject = new JSONObject(metaData);
			String url = mmObject.optString(ProductPopupsConstants.URL);
			return Utils.appendTokenInURL(url);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * The function calls the Url.Calling from background  Thread .
	 * @param metaData
	 */
	public void callToServer(final String metaData)
	{
		RequestToken requestToken = HttpRequests.productPopupRequest(getFormedUrl(metaData), new IRequestListener()
		{	
			@Override
			public void onRequestSuccess(Response result)
			{
				Logger.d("ProductPopup", " response code " + result.getStatusCode());
			}
			
			@Override
			public void onRequestProgressUpdate(float progress)
			{	
			}
			
			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				Logger.d("ProductPopup", " error code " + httpException.getErrorCode());
			}
		},getRequestType(metaData));
		requestToken.execute();
	}

	private String getRequestType(String metaData) 
	{
		String request=HikeConstants.GET;
		try
		{
			JSONObject object=new JSONObject(metaData);
			request=object.optString(ProductPopupsConstants.REQUEST_TYPE,HikeConstants.GET);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return request;
	}

	public void deleteAllPopups()
	{
		HikeContentDatabase.getInstance().deleteAllPopupsFromDatabase();
		clearPopupStack();
		HikeMessengerApp.getPubSub().publish(HikePubSub.PRODUCT_POPUP_BADGE_COUNT_CHANGED, null);
	}
	
	private void clearPopupStack()
	{
		mmSparseArray.clear();
	}
	
	public int getAllValidPopUp()
	{
		long presentTime = System.currentTimeMillis();
		int countValidPopUps = 0;

		ProductPopupsConstants.PopupTriggerPoints[] triggerPoints = ProductPopupsConstants.PopupTriggerPoints.values();
		for (int i = 0; i < triggerPoints.length; i++)
		{
			ArrayList<ProductContentModel> mmArray = mmSparseArray.get(triggerPoints[i].ordinal());
			if (mmArray != null&&!mmArray.isEmpty())
			{
				
				for (ProductContentModel productContentModel : mmArray)
				{
					if (productContentModel!=null&&productContentModel.getStarttime() <= presentTime && productContentModel.getEndtime() >= presentTime)
					{

						countValidPopUps++;

						break;

					}
				}
			}
		}
		return countValidPopUps;
	}
    
}
