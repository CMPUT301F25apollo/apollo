package com.example.apollo;

import static org.junit.Assert.assertNotNull;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.entrant.home.EventDetailsFragment;
import com.example.apollo.R;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * EventDetailsWaitlistInfoTest
 *  US 01.05.04
 *  US 01.05.05
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailsWaitlistInfoTest {

    @Test
    public void eventDetailsScreen_hasWaitlistCountTextView() {
        FragmentScenario<EventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(
                        EventDetailsFragment.class,
                        /* args = */ null,
                        R.style.Theme_Apollo
                );

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull("Root view should not be null", root);

            TextView waitlistCount = root.findViewById(R.id.textWaitlistCount);
            assertNotNull("Waitlist count TextView should exist", waitlistCount);
        });
    }

    @Test
    public void eventDetailsScreen_hasSummaryAreaForLotteryGuidelines() {
        FragmentScenario<EventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(
                        EventDetailsFragment.class,
                        /* args = */ null,
                        R.style.Theme_Apollo
                );

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull("Root view should not be null", root);

            TextView summary = root.findViewById(R.id.textEventSummary);
            assertNotNull("Event summary TextView should exist for showing details and lottery info", summary);
        });
    }
}
