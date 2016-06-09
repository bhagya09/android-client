package com.bsb.hike.modules.contactmgr;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.PrivacyPreferences;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PrivacyPreferencePersistenceTest {
    private final HikeUserDatabase db = mock(HikeUserDatabase.class);
    private final PrivacyPreferencePersistence persistence = new PrivacyPreferencePersistence(db) {
        @Override
        protected void execute(Runnable runnable) {
            runnable.run();
        }
    };
    private final PrivacyPreferences preSetPref = mock(PrivacyPreferences.class);
    private final String msisdn = "1234567890";
    private final ContactInfo contactInfo = new ContactInfo();

    public PrivacyPreferencePersistenceTest() {
        contactInfo.setPrivacyPrefs(preSetPref);
        contactInfo.setMsisdn(msisdn);
    }

    @Test
    public void whenPrivacyPrefIsNotSetForGivenMSISDNThenReturnsNull() {
        when(db.getPrivacyPreferencesForAGivenMsisdn(msisdn)).thenReturn(null);
        PrivacyPreferences preferences = persistence.getPrivacyPrefsForAGivenMsisdn(msisdn);
        assertNull(preferences);
    }

    @Test
    public void returnsSetPrivacyPrefForAGivenMSISDNFromDB() {
        when(db.getPrivacyPreferencesForAGivenMsisdn(msisdn)).thenReturn(preSetPref);
        PrivacyPreferences preferences = persistence.getPrivacyPrefsForAGivenMsisdn(msisdn);
        assertSame(preSetPref, preferences);
    }

    @Test
    public void shouldUpdatesBothDBAndInMemoryPrivacyPrefForLastSeen() throws Exception {
        persistence.toggleLastSeenSetting(contactInfo, true);
        verify(preSetPref).setLastSeen(true);
        verify(db).setLastSeenForMsisdns(msisdn, 1);
        persistence.toggleLastSeenSetting(contactInfo, false);
        verify(preSetPref).setLastSeen(false);
        verify(db).setLastSeenForMsisdns(msisdn, 0);
    }

    @Test
    public void shouldUpdatesBothDBAndInMemoryPrivacyPrefForSU() {
        persistence.toggleStatusUpdateSetting(contactInfo, true);
        verify(preSetPref).setStatusUpdate(true);
        verify(db).setSUSettingForMsisdns(msisdn, 1);
        persistence.toggleStatusUpdateSetting(contactInfo, false);
        verify(preSetPref).setStatusUpdate(false);
        verify(db).setSUSettingForMsisdns(msisdn, 0);
    }

    @Test
    public void shouldTurnOffLastSeenForAllContactsInDBWhenUserSwitchesToNoBody() {
        persistence.setAllLastSeenValues(false);
        verify(db).setAllLastSeenPrivacyValues(false);
    }

    @Test
    public void shouldTurnOnLastSeenForAllContactsInDBWhenUserSwitchesToFriends() {
        persistence.setAllLastSeenValues(true);
        verify(db).setAllLastSeenPrivacyValues(true);
    }

    @Test
    public void shouldDefaultToNotShowSUForContactsNotPresentInDB() {
        when(db.getPrivacyPreferencesForAGivenMsisdn(msisdn)).thenReturn(null);
        assertFalse(persistence.shouldShowStatusUpdateForGivenMsisdn(msisdn));
    }

    @Test
    public void shouldFetchShowSUPrivacyPrefFromDB() {
        when(db.getPrivacyPreferencesForAGivenMsisdn(msisdn)).thenReturn(preSetPref);
        when(preSetPref.shouldShowStatusUpdate()).thenReturn(true);
        assertTrue(persistence.shouldShowStatusUpdateForGivenMsisdn(msisdn));
    }

    @Test
    public void shouldSwitchToDefaultPrivacyPrefForAllContactsWhenFriendsExperimentIsTurnedOff() {
        persistence.flushOldPrivacyValues(false, true);
        verify(db).flushOldPrivacyValues(false, true);
    }
}
