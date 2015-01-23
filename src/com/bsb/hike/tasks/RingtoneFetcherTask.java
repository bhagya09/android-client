package com.bsb.hike.tasks;

import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;

import com.bsb.hike.R;

public class RingtoneFetcherTask extends AsyncTask<Void, Void, Void> implements ActivityCallableTask
{

	public static interface RingtoneFetchListener
	{
		public void onRingtoneFetched(boolean isSuccess, Map<String, Uri> ringtonesNameURIMap);
	}

	private RingtoneFetchListener listener;

	private boolean finished;

	private boolean delete;

	private Context ctx;

	private Map<String, Uri> ringtonesNameURIMap;

	private Cursor notifSoundCursor;

	public RingtoneFetcherTask(RingtoneFetchListener activity, boolean delete, Context context)
	{
		this.listener = activity;
		this.delete = delete;
		this.ctx = context;
		this.ringtonesNameURIMap = new LinkedHashMap<String, Uri>();
	}

	protected void onPreExecute()
	{
		initRingtoneLists();
	};

	@Override
	protected Void doInBackground(Void... params)
	{
			try
			{
				RingtoneManager ringtoneMgr = new RingtoneManager(ctx);
				ringtoneMgr.setType(RingtoneManager.TYPE_NOTIFICATION);
				notifSoundCursor = ringtoneMgr.getCursor();
				if (notifSoundCursor != null)
				{
					int notifSoundCount = notifSoundCursor.getCount();
					if (notifSoundCount != 0 && notifSoundCursor.moveToFirst())
					{
						String ringtoneName = "";
						while (!notifSoundCursor.isAfterLast() && notifSoundCursor.moveToNext())
						{
							int currentPosition = notifSoundCursor.getPosition();
							ringtoneName = ringtoneMgr.getRingtone(currentPosition).getTitle(ctx);
							ringtonesNameURIMap.put(ringtoneName, ringtoneMgr.getRingtoneUri(currentPosition));
						}
					}
					else
					{
						// No Notification Tone was present in device
					}

					// The returned cursor will be the same cursor returned each time this method is called,
					// so do not Cursor.close() the cursor. The cursor can be Cursor.deactivate() safely.
					notifSoundCursor.deactivate();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		return null;
	}

	protected void onPostExecute(Void result)
	{
		finished = true;
		if (listener != null)
		{
			listener.onRingtoneFetched(true, ringtonesNameURIMap);
		}
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.listener = (RingtoneFetchListener) activity;
	}

	@Override
	public boolean isFinished()
	{
		return finished;
	}

	private void initRingtoneLists()
	{
		ringtonesNameURIMap.put(ctx.getResources().getString(R.string.notif_sound_off), null);
		ringtonesNameURIMap.put(ctx.getResources().getString(R.string.notif_sound_default), null);
		ringtonesNameURIMap.put(ctx.getResources().getString(R.string.notif_sound_Hike), null);
	}

}