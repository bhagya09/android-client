package com.bsb.hike.timeline;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.ActionsDataModel.DataTypes;
import com.bsb.hike.timeline.model.TimelineActions;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class ActionsDeserializer implements JsonDeserializer<TimelineActions>
{

	@SuppressWarnings("rawtypes")
	@Override
	public TimelineActions deserialize(JsonElement json, Type type, JsonDeserializationContext arg2) throws JsonParseException
	{
		if (type instanceof Class)
		{
			if (((Class) type).getName().equals(TimelineActions.class.getName()))
			{

				final TimelineActions timelineActions = new TimelineActions();

				// The below is based on client server agreement
				final JsonObject jsonObject = json.getAsJsonObject();
				String stat = jsonObject.get("stat").getAsString();
				if (stat.equals("ok"))
				{
					JsonArray msgArray = jsonObject.get("m").getAsJsonArray();

					int size = msgArray.size();

					for (int i = 0; i < size; i++)
					{
						JsonObject msgObj = msgArray.get(i).getAsJsonObject();
						String uuid = null;
						if (msgObj.has("uuid"))
						{
							uuid = msgObj.get("uuid").getAsString();
						}
						else if (msgObj.has("su_id"))
						{
							uuid = msgObj.get("su_id").getAsString();
						}

						if (uuid == null)
						{
							continue;
						}

						ArrayList<ContactInfo> contactInfoList = new ArrayList<ContactInfo>();

						int count = 0;

						DataTypes actionType = null;

						if (msgObj.has("l"))
						{
							// Has likes
							actionType = ActionsDataModel.DataTypes.LIKE;

							JsonArray likesArray = msgObj.get("l").getAsJsonArray();

							int likesArraySize = likesArray.size();

							for (int j = 0; j < likesArraySize; j++)
							{
								String msisdn = likesArray.get(j).getAsJsonObject().get("mdn").getAsString();
								ContactInfo contact = ContactManager.getInstance().getContact(msisdn);
								if (contact != null)
								{
									contactInfoList.add(contact);
								}
							}

							count = msgObj.get("lc").getAsInt();
						}

						timelineActions.addActionDetails(uuid, contactInfoList, actionType, count);
					}
				}

				return timelineActions;
			}
		}
		return null;
	}
}
