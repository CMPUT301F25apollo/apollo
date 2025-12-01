package com.example.apollo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.entrant.home.HomeFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * HomeFragmentEventListTest
 *
 * Covers US 01.01.03:
 *  As an entrant, I want to be able to see a list of events that I can join
 *  the waiting list for.
 *
 * This test verifies that:
 *  - HomeFragment launches without crashing.
 *  - The events list container (where event cards are added) exists
 *    and is VISIBLE, meaning the UI is ready to show joinable events.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HomeFragmentEventListTest {

    @Test
    public void home_showsEventsContainerVisible() {
        // Launch HomeFragment in isolation with the app theme
        FragmentScenario<HomeFragment> scenario =
                FragmentScenario.launchInContainer(
                        HomeFragment.class,
                        new Bundle(),
                        R.style.Theme_Apollo
                );

        // Inspect the fragment's view hierarchy directly
        scenario.onFragment(fragment -> {
            LinearLayout eventsContainer =
                    fragment.requireView().findViewById(R.id.eventsContainer);

            // 1) Container exists
            assertNotNull("eventsContainer should not be null", eventsContainer);

            // 2) Container is visible so entrant can see joinable events list
            assertEquals(
                    "eventsContainer should be VISIBLE so entrant can see joinable events",
                    View.VISIBLE,
                    eventsContainer.getVisibility()
            );
        });
    }
}
