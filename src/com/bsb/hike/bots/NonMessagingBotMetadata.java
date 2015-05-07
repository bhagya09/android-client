package com.bsb.hike.bots;

<<<<<<< HEAD
<<<<<<< HEAD
import com.bsb.hike.models.OverFlowMenuItem;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
=======
=======
>>>>>>> parent of dc90570... Revert "Merge pull request #2716 from gaurav-hike/nonMessagingShobhit"
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.models.OverFlowMenuItem;
<<<<<<< HEAD
>>>>>>> parent of dc90570... Revert "Merge pull request #2716 from gaurav-hike/nonMessagingShobhit"
=======
>>>>>>> parent of dc90570... Revert "Merge pull request #2716 from gaurav-hike/nonMessagingShobhit"

/**
 * Created by shobhit on 22/04/15.
 */
public class NonMessagingBotMetadata
{
	JSONObject json;

	public NonMessagingBotMetadata(String jsonString)
	{
		try
		{
<<<<<<< HEAD
<<<<<<< HEAD
			this.json = new JSONObject(jsonString);
=======
			json = new JSONObject(jsonString);
>>>>>>> parent of dc90570... Revert "Merge pull request #2716 from gaurav-hike/nonMessagingShobhit"
=======
			json = new JSONObject(jsonString);
>>>>>>> parent of dc90570... Revert "Merge pull request #2716 from gaurav-hike/nonMessagingShobhit"
		}
		catch (JSONException e)
		{
			e.printStackTrace();
<<<<<<< HEAD
<<<<<<< HEAD
			this.json = new JSONObject();
		}
	}

	public NonMessagingBotMetadata(JSONObject metadata)
	{
		this.json = (null == metadata) ? new JSONObject() : metadata;
	}

	@Override
	public String toString()
	{
		return json.toString();
	}

	public List<OverFlowMenuItem> getOverflowItems()
	{
		return null;
	}
=======
			json = new JSONObject();
		}
	}
>>>>>>> parent of dc90570... Revert "Merge pull request #2716 from gaurav-hike/nonMessagingShobhit"
=======
			json = new JSONObject();
		}
	}
>>>>>>> parent of dc90570... Revert "Merge pull request #2716 from gaurav-hike/nonMessagingShobhit"

	
	
}
