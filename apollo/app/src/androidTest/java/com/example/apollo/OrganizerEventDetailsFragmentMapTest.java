package com.example.apollo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.apollo.ui.organizer.events.OrganizerEventDetailsFragment;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * US 02.02.02
 *
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerEventDetailsFragmentMapTest {


    @Test
    public void mapVisible_whenGeolocationEnabledAndCoordinate() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String eventId = "testEvent_withMap";

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title", "Map Test Event");
        eventData.put("description", "Event for map UI test");
        eventData.put("location", "Test Location");
        eventData.put("date", "12/20/2025");
        eventData.put("time", "10:00 AM");
        eventData.put("geolocation", true);

        Map<String, Object> coord = new HashMap<>();
        coord.put("lat", 53.5461);
        coord.put("lon", -113.4938);
        eventData.put("coordinate", Arrays.asList(coord));

        Tasks.await(db.collection("events").document(eventId).set(eventData));

        Bundle args = new Bundle();
        args.putString("eventId", eventId);

        FragmentScenario<OrganizerEventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(
                        OrganizerEventDetailsFragment.class,
                        args,
                        R.style.Theme_Apollo
                );

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            View mapView = root.findViewById(R.id.map);
            assertNotNull("MapView should exist in layout", mapView);

        });
    }

}
