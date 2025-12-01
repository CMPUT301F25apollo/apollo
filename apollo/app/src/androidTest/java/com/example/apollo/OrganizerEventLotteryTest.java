package com.example.apollo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.Button;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.organizer.events.OrganizerEventDetailsFragment;
import com.example.apollo.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * OrganizerEventLotteryTest
 *  US 02.05.01
 *  US 02.05.02
 *  US 02.05.03
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerEventLotteryTest {

    // helper to set private boolean fields
    private static void setPrivateBoolean(OrganizerEventDetailsFragment fragment,
                                          String fieldName,
                                          boolean value) {
        try {
            Field f = OrganizerEventDetailsFragment.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(fragment, value);
        } catch (Exception e) {
            throw new AssertionError("Failed to set field '" + fieldName + "'", e);
        }
    }

    // helper to call updateLotteryButtonUi()
    private static void callUpdateLotteryButtonUi(OrganizerEventDetailsFragment fragment) {
        try {
            Method m = OrganizerEventDetailsFragment.class
                    .getDeclaredMethod("updateLotteryButtonUi");
            m.setAccessible(true);
            m.invoke(fragment);
        } catch (Exception e) {
            throw new AssertionError("Failed to invoke updateLotteryButtonUi()", e);
        }
    }

    // Organizer screen should have the main lottery + notification controls
    @Test
    public void organizerScreen_hasLotteryAndNotificationButtons() {
        FragmentScenario<OrganizerEventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(OrganizerEventDetailsFragment.class);

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            Button sendLottery      = root.findViewById(R.id.buttonSendLottery);
            Button notifySelected   = root.findViewById(R.id.buttonNotifySelected);
            Button notifyCancelled  = root.findViewById(R.id.buttonNotifyCancelled);
            Button viewParticipants = root.findViewById(R.id.buttonViewParticipants);

            assertNotNull(sendLottery);
            assertTrue(sendLottery.isClickable());

            assertNotNull(notifySelected);
            assertTrue(notifySelected.isClickable());

            assertNotNull(notifyCancelled);
            assertTrue(notifyCancelled.isClickable());

            assertNotNull(viewParticipants);
            assertTrue(viewParticipants.isClickable());
        });
    }

    // Before registration closes: lottery button stays grey
    @Test
    public void lotteryButton_beforeRegistrationClose_isGreyButClickable() {
        FragmentScenario<OrganizerEventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(OrganizerEventDetailsFragment.class);

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            Button sendLottery = root.findViewById(R.id.buttonSendLottery);
            assertNotNull(sendLottery);

            setPrivateBoolean(fragment, "registrationClosed", false);
            setPrivateBoolean(fragment, "lotteryDone", false);
            callUpdateLotteryButtonUi(fragment);

            assertEquals("SEND LOTTERY", sendLottery.getText().toString());
            assertTrue(sendLottery.isEnabled());

            Context ctx = fragment.requireContext();
            ColorStateList tint = sendLottery.getBackgroundTintList();
            assertNotNull(tint);
            int actualColor = tint.getDefaultColor();
            int expectedGrey = ContextCompat.getColor(ctx, android.R.color.darker_gray);
            assertEquals(expectedGrey, actualColor);
        });
    }

    // After registration closes but before lottery button turns active
    @Test
    public void lotteryButton_afterRegistrationClose_isActiveBlack() {
        FragmentScenario<OrganizerEventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(OrganizerEventDetailsFragment.class);

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            Button sendLottery = root.findViewById(R.id.buttonSendLottery);
            assertNotNull(sendLottery);

            setPrivateBoolean(fragment, "registrationClosed", true);
            setPrivateBoolean(fragment, "lotteryDone", false);
            callUpdateLotteryButtonUi(fragment);

            assertEquals("SEND LOTTERY", sendLottery.getText().toString());
            assertTrue(sendLottery.isEnabled());

            Context ctx = fragment.requireContext();
            ColorStateList tint = sendLottery.getBackgroundTintList();
            assertNotNull(tint);
            int actualColor = tint.getDefaultColor();
            int expectedBlack = ContextCompat.getColor(ctx, android.R.color.black);
            assertEquals(expectedBlack, actualColor);
        });
    }

    // After lottery is run button shows "LOTTERY SENT" and is disabled
    @Test
    public void lotteryButton_afterLotteryRun_isDisabledAndShowsSent() {
        FragmentScenario<OrganizerEventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(OrganizerEventDetailsFragment.class);

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            Button sendLottery = root.findViewById(R.id.buttonSendLottery);
            assertNotNull(sendLottery);

            setPrivateBoolean(fragment, "registrationClosed", true);
            setPrivateBoolean(fragment, "lotteryDone", true);
            callUpdateLotteryButtonUi(fragment);

            assertEquals("LOTTERY SENT", sendLottery.getText().toString());
            assertTrue(!sendLottery.isEnabled());

            Context ctx = fragment.requireContext();
            ColorStateList tint = sendLottery.getBackgroundTintList();
            assertNotNull(tint);
            int actualColor = tint.getDefaultColor();
            int expectedGrey = ContextCompat.getColor(ctx, android.R.color.darker_gray);
            assertEquals(expectedGrey, actualColor);
        });
    }
}
