package com.bsb.hike.chatthread;

import android.content.Context;
import android.test.suitebuilder.annotation.SmallTest;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SmallTest
public class NudgeManagerTest {
    private static final boolean TURNED_ON = true;
    private static final boolean TURNED_OFF = false;
    private final Context context = mock(Context.class);
    private final HAManager haManager = mock(HAManager.class);
    private final HikeSharedPreferenceUtil hikePref = mock(HikeSharedPreferenceUtil.class);
    private final List<Integer> displayedToastMsgs = new ArrayList<>();
    private final NudgeManager nudgeManager = new NudgeManager(context, hikePref, haManager){
        @Override
        protected void showToast(int msg) {
            displayedToastMsgs.add(msg);
        }
    };

    @Before
    public void setUpCoolOffTime() {
        when(hikePref.getData(HikeConstants.NUDGE_SEND_COOLOFF_TIME, 300)).thenReturn(100);
    }

    @Test
    public void nudgeIsAllowedWhenDoubleTapPreferenceIsSet() {
        givenDoubleTabPrefIs(TURNED_ON);
        verifySendNudgeShouldReturn(true);
    }

    @Test
    public void recordAnAnalyticsUIEventOnDoubleTapWhenNudgeIsTurnedOff() {
        givenDoubleTabPrefIs(TURNED_OFF);
        verifySendNudgeShouldReturn(false);
        verify(haManager).record(eq(AnalyticsConstants.UI_EVENT), eq(AnalyticsConstants.CLICK_EVENT), any(JSONObject.class));
    }

    @Test
    public void nudgeIsNotAllowedIfUserDoubleTapsTwiceVeryQuickly() {
        givenDoubleTabPrefIs(TURNED_ON);
        verifySendNudgeShouldReturn(true);
        verifySendNudgeShouldReturn(false);
    }

    @Test
    public void informUserThatDoubleTapPrefIsTurnedOffOnEveryThirdConsecutiveAttempt() {
        givenDoubleTabPrefIs(TURNED_OFF);
        int count = 9;
        for (int i = 0; i < count; i++) {
            verifySendNudgeShouldReturn(false);
        }
        assertEquals(count/3, displayedToastMsgs.size());
        assertEquals(R.string.nudge_toast, displayedToastMsgs.get(0).intValue());
    }

    private void verifySendNudgeShouldReturn(boolean expectedSendNudgeVal) {
        assertEquals("Expected SendNudge to be " + expectedSendNudgeVal, expectedSendNudgeVal, nudgeManager.shouldSendNudge());
    }

    private void givenDoubleTabPrefIs(boolean value) {
        when(hikePref.getSharedPreferenceAsBoolean(HikeConstants.DOUBLE_TAP_PREF, true)).thenReturn(value);
        nudgeManager.updateLatestNudgeSetting();
    }
}
