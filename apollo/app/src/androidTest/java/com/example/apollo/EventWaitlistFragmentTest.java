package com.example.apollo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.organizer.events.EventWaitlistFragment;
import com.example.apollo.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * EventWaitlistFragmentTest
 * US 02.02.01
 * US 02.06.01
 * US 02.06.02
 * US 02.06.03
 * US 02.06.05
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventWaitlistFragmentTest {

    // Check the waitlist screen has the main UI pieces (list, filter, empty text, button)
    @Test
    public void eventWaitlistScreen_hasCoreUiElements() {
        FragmentScenario<EventWaitlistFragment> scenario =
                FragmentScenario.launchInContainer(EventWaitlistFragment.class);

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            ListView listView = root.findViewById(R.id.listView);
            Spinner spinner = root.findViewById(R.id.filterSpinner);
            TextView emptyText = root.findViewById(R.id.emptyText);
            Button exportButton = root.findViewById(R.id.exportCsvButton);

            assertNotNull(listView);
            assertNotNull(spinner);
            assertNotNull(emptyText);
            assertNotNull(exportButton);
        });
    }

    // Spinner should list all statuses the organizer can filter by.
    @Test
    public void eventWaitlist_filterSpinner_hasAllStatusOptions() {
        FragmentScenario<EventWaitlistFragment> scenario =
                FragmentScenario.launchInContainer(EventWaitlistFragment.class);

        scenario.onFragment(fragment -> {
            Spinner spinner = fragment.requireView().findViewById(R.id.filterSpinner);
            assertNotNull(spinner);
            assertNotNull(spinner.getAdapter());

            List<String> labels = new ArrayList<>();
            for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
                Object item = spinner.getAdapter().getItem(i);
                if (item != null) {
                    labels.add(item.toString());
                }
            }

            // expected status filters
            assertTrue(labels.contains("All"));
            assertTrue(labels.contains("Accepted"));
            assertTrue(labels.contains("Declined"));
            assertTrue(labels.contains("invited"));
            assertTrue(labels.contains("Loser"));
            assertTrue(labels.contains("Waiting"));
        });
    }

    // Make sure we can switch through each filter option without crashing.
    @Test
    public void eventWaitlist_canSelectEachStatusFilter() {
        FragmentScenario<EventWaitlistFragment> scenario =
                FragmentScenario.launchInContainer(EventWaitlistFragment.class);

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            Spinner spinner = root.findViewById(R.id.filterSpinner);
            ListView listView = root.findViewById(R.id.listView);

            assertNotNull(spinner);
            assertNotNull(listView);
            assertNotNull(spinner.getAdapter());

            int count = spinner.getAdapter().getCount();
            assertTrue("Spinner should have at least one filter option", count > 0);

            // loop through filters and select each one once
            for (int i = 0; i < count; i++) {
                spinner.setSelection(i);

                // selected item should match the spinner’s position
                assertEquals(i, spinner.getSelectedItemPosition());

                // listView should still be wired
                assertNotNull(listView.getAdapter());
            }
        });
    }

    // Export CSV button should be visible and clickable
    @Test
    public void eventWaitlist_exportCsvButton_isVisibleAndClickable() {
        FragmentScenario<EventWaitlistFragment> scenario =
                FragmentScenario.launchInContainer(EventWaitlistFragment.class);

        scenario.onFragment(fragment -> {
            Button exportButton = fragment.requireView().findViewById(R.id.exportCsvButton);
            assertNotNull(exportButton);
            assertEquals(View.VISIBLE, exportButton.getVisibility());
            assertTrue(exportButton.isClickable());
        });
    }

    @Test
    public void eventWaitlist_displaysEntrantsFromFirestore() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fake event ID
        String eventId = "testEvent_waitlistDisplay";

        // Fake waitlist
        Map<String, Object> user1 = new HashMap<>();
        user1.put("name", "Alice Test");
        Tasks.await(db.collection("users").document("userA").set(user1));

        Map<String, Object> user2 = new HashMap<>();
        user2.put("name", "Bob Test");
        Tasks.await(db.collection("users").document("userB").set(user2));

        Map<String, Object> wlState = new HashMap<>();
        wlState.put("state", "waiting");

        Tasks.await(db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document("userA")
                .set(wlState));

        Tasks.await(db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document("userB")
                .set(wlState));

        Bundle args = new Bundle();
        args.putString("eventId", eventId);

        FragmentScenario<EventWaitlistFragment> scenario =
                FragmentScenario.launchInContainer(
                        EventWaitlistFragment.class,
                        args,
                        R.style.Theme_Apollo
                );

        scenario.onFragment(fragment -> {
            View root = fragment.requireView();
            ListView listView = root.findViewById(R.id.listView);
            TextView emptyText = root.findViewById(R.id.emptyText);

            assertNotNull(listView);
            assertNotNull(listView.getAdapter());
            assertNotNull(emptyText);
        });
    }

}
