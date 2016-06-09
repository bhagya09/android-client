package com.bsb.hike.models;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrivacyPreferencesTest {
    private final PrivacyPreferences preferences = new PrivacyPreferences(PrivacyPreferences.DEFAULT_VALUE);

    @Test
    public void byDefaultAllPrivacyPreferencesAreOff() {
        assertFalse(preferences.shouldShowLastSeen());
        assertFalse(preferences.shouldShowStatusUpdate());
    }

    @Test
    public void userCanToggleLastSeenPreference() {
        preferences.setLastSeen(true);
        assertTrue(preferences.shouldShowLastSeen());
        preferences.setLastSeen(false);
        assertFalse(preferences.shouldShowLastSeen());
    }

    @Test
    public void userCanToggleStatusUpdatePreference() {
        preferences.setStatusUpdate(true);
        assertTrue(preferences.shouldShowStatusUpdate());
        preferences.setStatusUpdate(false);
        assertFalse(preferences.shouldShowLastSeen());
    }
}
