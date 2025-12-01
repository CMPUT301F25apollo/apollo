package com.example.apollo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.entrant.notifications.InviteResponseFragment;
import com.example.apollo.R;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * InviteResponseFeatureTest
 *   US 01.05.02
 *   US 01.05.03
 *   Launches InviteResponseFragment with a dummy eventId
 *   Verifies the invite message + Accept / Decline buttons exist and are clickable
 *   We avoid calling Firestore by not checking backend side-effects here
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class InviteResponseFeatureTest {

    @Test
    public void inviteResponseScreen_hasMessageAndAcceptDeclineButtons() {
        Bundle args = new Bundle();
        args.putString("eventId", "dummyEventId");

        FragmentScenario<InviteResponseFragment> scenario =
                FragmentScenario.launchInContainer(
                        InviteResponseFragment.class,
                        args,
                        R.style.Theme_Apollo
                );

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull("Root view should not be null", root);

            TextView inviteMessage = root.findViewById(R.id.textInviteMessage);
            Button buttonAccept    = root.findViewById(R.id.buttonAcceptInvite);
            Button buttonDecline   = root.findViewById(R.id.buttonDeclineInvite);

            assertNotNull("Invite message text should exist", inviteMessage);
            assertNotNull("Accept button should exist", buttonAccept);
            assertNotNull("Decline button should exist", buttonDecline);

            assertTrue("Accept button should be clickable", buttonAccept.isClickable());
            assertTrue("Decline button should be clickable", buttonDecline.isClickable());
        });
    }
}
