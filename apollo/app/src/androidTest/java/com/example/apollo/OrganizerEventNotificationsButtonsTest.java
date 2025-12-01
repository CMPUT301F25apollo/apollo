package com.example.apollo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.R;
import com.example.apollo.ui.organizer.events.OrganizerEventDetailsFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * OrganizerEventNotificationsButtonsTest
 *  US 02.07.02
 *  US 02.07.03
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerEventNotificationsButtonsTest {


    // Check that the "notify selected" and "notify cancelled"
    // buttons exist on the organizer event details screen
    // and can be clicked.
    @Test
    public void organizerScreen_hasNotificationButtonsForSelectedAndCancelled() {
        Bundle args = new Bundle();
        args.putString("eventId", "dummyEventId");

        FragmentScenario<OrganizerEventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(
                        OrganizerEventDetailsFragment.class,
                        args,
                        R.style.Theme_Apollo
                );

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            Button notifySelected  = root.findViewById(R.id.buttonNotifySelected);
            Button notifyCancelled = root.findViewById(R.id.buttonNotifyCancelled);

            assertNotNull("Notify-selected button should exist", notifySelected);
            assertNotNull("Notify-cancelled button should exist", notifyCancelled);

            assertTrue("Notify-selected button should be clickable", notifySelected.isClickable());
            assertTrue("Notify-cancelled button should be clickable", notifyCancelled.isClickable());
        });
    }
}
