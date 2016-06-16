package com.bsb.hike.modules.signupmgr;

import android.support.test.runner.AndroidJUnit4;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.HikeUserDatabase;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.response.ResponseBody;
import com.bsb.hike.modules.httpmgr.response.ResponseFacade;
import com.bsb.hike.modules.httpmgr.retry.BasicRetryPolicy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class PostAddressBookTaskTest {
    private final String msisdn = "+911234512345";
    private final String name = "Test User";
    private final String uid = "uid";
    private final String userId = "user_id";

    @Test
    public void duringNewInstallUsersAddressBookContactsAreUpdatedFromServerWithUID() throws JSONException {
        ContactInfo singleContact = buildContactInfo(msisdn, name, userId);
        Map<String, List<ContactInfo>> contactMap = buildContactInfoMap(singleContact);

        JSONArray blockList = new JSONArray();
        JSONArray favorites = new JSONArray();
        JSONObject addressBook = wrapContactInJSONArray(userId, buildContact(msisdn, uid, true));

        final JSONObject responseBody = buildServerResponse(blockList, favorites, addressBook);

        HttpRequests httpRequest = buildHttpRequestToInterceptPostAddressBookCallAndReturn(responseBody);
        PostAddressBookTask task = new PostAddressBookTask(contactMap, httpRequest);
        assertTrue(task.execute());

        ContactInfo contactInfo = fetchContactInfoFromDBBasedOn(msisdn);
        assertEquals(uid, contactInfo.getUid());
    }

    @Test
    public void duringNewInstallBlockedPhoneNumbersInUsersAddressBookAreUpdatedFromServerWithUID() throws JSONException {
        ContactInfo singleContact = buildContactInfo(msisdn, name, userId);
        singleContact.setBlockStatus(true);
        Map<String, List<ContactInfo>> contactMap = buildContactInfoMap(singleContact);

        JSONObject contact = buildContact(msisdn, uid, true);
        JSONArray blockList = wrapContactInJSONArray(contact);
        JSONArray favorites = new JSONArray();
        JSONObject addressBook = wrapContactInJSONArray(userId, contact);

        final JSONObject responseBody = buildServerResponse(blockList, favorites, addressBook);

        HttpRequests httpRequest = buildHttpRequestToInterceptPostAddressBookCallAndReturn(responseBody);
        PostAddressBookTask task = new PostAddressBookTask(contactMap, httpRequest);
        assertTrue(task.execute());

        ContactInfo contactInfo = fetchContactInfoFromDBBasedOn(msisdn);
        assertEquals(uid, contactInfo.getUid());
        assertTrue(contactInfo.getBlockedStatus());
    }


    private Map<String, List<ContactInfo>> buildContactInfoMap(ContactInfo... singleContact) {
        final List<ContactInfo> contactInfos = asList(singleContact);
        return new HashMap<String, List<ContactInfo>>() {{
            put(userId, contactInfos);
        }};
    }

    private ContactInfo buildContactInfo(String msisdn, String name, String userId) {
        return new ContactInfo(userId, msisdn, name, msisdn);
    }

    private ContactInfo fetchContactInfoFromDBBasedOn(String msisdn) {
        Map<String, ContactInfo> allContacts = HikeUserDatabase.getInstance().getContactInfoFromMsisdns(singletonList(msisdn), false);
        return allContacts.get(msisdn);
    }

    private static HttpRequests buildHttpRequestToInterceptPostAddressBookCallAndReturn(final JSONObject responseBody) {
        final IResponseInterceptor.Chain chain = new IResponseInterceptor.Chain() {
            private final ResponseBody<JSONObject> body = ResponseBody.create("application/json", 0, responseBody);

            @Override
            public ResponseFacade getResponseFacade() {
                return new ResponseFacade(null) {
                    @Override
                    public Response getResponse() {
                        Response.Builder builder = new Response.Builder();
                        builder.setBody(body);
                        builder.setUrl("testURL");
                        builder.setStatusCode(200);
                        return builder.build();
                    }

                    @Override
                    public ResponseBody<JSONObject> getBody() {
                        return body;
                    }
                };
            }

            @Override
            public void proceed() {

            }
        };
        return new HttpRequests() {
            @Override
            public RequestToken postAddressBookRequest(JSONObject json, final IRequestListener requestListener, final IResponseInterceptor responseInterceptor, BasicRetryPolicy retryPolicy) {
                return new RequestToken(null) {
                    @Override
                    public void execute() {
                        requestListener.onRequestSuccess(chain.getResponseFacade().getResponse());
                        responseInterceptor.intercept(chain);
                    }
                };
            }
        };
    }

    private JSONObject buildServerResponse(JSONArray blockList, JSONArray favorites, JSONObject addressBook) {
        JSONObject responseBody = new JSONObject();
        try {
            responseBody.put("blocklist", blockList);
            responseBody.put("favorites", favorites);
            responseBody.put("addressbook", addressBook);
        } catch (JSONException e) {
            fail("Could not build JSON - " + e.getMessage());
        }
        return responseBody;
    }

    private JSONObject wrapContactInJSONArray(String userId, JSONObject contact) throws JSONException {
        JSONObject obj = new JSONObject();
        JSONArray value = new JSONArray();
        value.put(contact);
        obj.put(userId, value);
        return obj;
    }

    private JSONArray wrapContactInJSONArray(JSONObject contact) {
        JSONArray value = new JSONArray();
        value.put(contact);
        return value;
    }

    private JSONObject buildContact(String msisdn, String uid, boolean onHike) throws JSONException {
        JSONObject contact = new JSONObject();
        contact.put("m", msisdn);
        contact.put("oh", onHike);
        contact.put("u", uid);
        return contact;
    }
}
