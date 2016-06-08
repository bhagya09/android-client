package com.bsb.hike.modules.contactmgr;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.PrivacyPreferences;

import org.junit.Test;

import static com.bsb.hike.models.PrivacyPreferences.DEFAULT_VALUE;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrivacyPreferencePersistenceTest {
    private final HikeUserDatabase db = mock(HikeUserDatabase.class);
    private final PrivacyPreferencePersistence persistence = new PrivacyPreferencePersistence(db);
    private final PrivacyPreferences preSetPref = new PrivacyPreferences(DEFAULT_VALUE);
    private final String msisdn = "1234567890";
    private final ContactInfo contactInfo = new ContactInfo();

    public PrivacyPreferencePersistenceTest() {
        contactInfo.setPrivacyPrefs(preSetPref);
        contactInfo.setMsisdn(msisdn);
    }

    @Test
    public void getPrivacyPrefFromDbCheck() {
        when(db.getPrivacyPreferencesForAGivenMsisdn(msisdn)).thenReturn(preSetPref);
        PrivacyPreferences preferences = persistence.getPrivacyPrefsForAGivenMsisdn(msisdn);
        assertSame(preSetPref, preferences);
    }

    @Test
    public void toggleLastSeenSetting() {
        persistence.toggleLastSeenSetting(contactInfo, true);
        assertTrue(contactInfo.getPrivacyPrefs().shouldShowLastSeen());
        persistence.toggleLastSeenSetting(contactInfo, false);
        assertFalse(contactInfo.getPrivacyPrefs().shouldShowLastSeen());
    }

    @Test
    public void toggleStatusUpdateSetting() {
        persistence.toggleStatusUpdateSetting(contactInfo, true);
        assertTrue(contactInfo.getPrivacyPrefs().shouldShowStatusUpdate());
        persistence.toggleStatusUpdateSetting(contactInfo, false);
        assertFalse(contactInfo.getPrivacyPrefs().shouldShowStatusUpdate());
    }
}
