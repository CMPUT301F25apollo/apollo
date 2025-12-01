package com.example.apollo.admin.events;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.example.apollo.R;
import com.example.apollo.ui.admin.events.EventsFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Config(sdk = 34)
@RunWith(RobolectricTestRunner.class)
public class EventsFragmentTest {

    private FragmentActivity activity;
    private EventsFragment fragment;

    @Before
    public void setUp() {
        // Initialize Firebase for Robolectric so FirebaseFirestore.getInstance() works
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }

        // Host activity
        activity = Robolectric.buildActivity(FragmentActivity.class)
                .setup()
                .get();

        // Attach fragment so onCreateView() runs and views are initialized
        fragment = new EventsFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment, "events")
                .commitNow();
    }

    // ================== FILTER TESTS ==================

    @Test
    public void filterEvents_showsAllEvents_whenQueryEmpty() throws Exception {
        List<Object> allEvents = getAllEventsList();
        allEvents.clear();

        // posterUrl = null to avoid calling Glide in tests
        allEvents.add(newEvent("id1", "Welcome Party", null, "SUB", 100L, 50L, 10));
        allEvents.add(newEvent("id2", "Science Fair", null, "Hall A", 200L, 80L, 20));
        allEvents.add(newEvent("id3", "Apollo Basketball Night", null, "Gym", 150L, 60L, 5));

        callFilterEvents("");

        LinearLayout container = getEventsContainer();
        assertEquals(3, container.getChildCount());
    }

    @Test
    public void filterEvents_filtersByTitleSubstring_caseInsensitive() throws Exception {
        List<Object> allEvents = getAllEventsList();
        allEvents.clear();

        allEvents.add(newEvent("id1", "Welcome Party", null, "SUB", 100L, 50L, 10));
        allEvents.add(newEvent("id2", "Science Fair", null, "Hall A", 200L, 80L, 20));
        allEvents.add(newEvent("id3", "Apollo Basketball Night", null, "Gym", 150L, 60L, 5));

        callFilterEvents("party");

        LinearLayout container = getEventsContainer();
        assertEquals(1, container.getChildCount());

        TextView titleView = container.getChildAt(0).findViewById(R.id.eventTitle);
        String titleText = titleView.getText().toString();
        assertTrue(titleText.toLowerCase().contains("party"));
    }

    // ================== DELETE TEST ==================

    @Test
    public void deleteEvent_removesCardFromContainer_onSuccess() throws Exception {
        LinearLayout container = getEventsContainer();

        // Start with a fake card in the container
        View card = new View(activity);
        container.addView(card);
        assertEquals(1, container.getChildCount());

        String eventId = "event123";

        // Mock Firestore and delete() chain
        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        CollectionReference mockEvents = mock(CollectionReference.class);
        DocumentReference mockDocRef = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> mockTask = (Task<Void>) mock(Task.class);

        when(mockDb.collection("events")).thenReturn(mockEvents);
        when(mockEvents.document(eventId)).thenReturn(mockDocRef);
        when(mockDocRef.delete()).thenReturn(mockTask);

        // When addOnSuccessListener is attached, immediately invoke the listener
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class)))
                .thenAnswer(invocation -> {
                    OnSuccessListener<Void> listener = invocation.getArgument(0);
                    listener.onSuccess(null);
                    return mockTask;
                });

        // Just return the task when failure listener is added (won't be used here)
        when(mockTask.addOnFailureListener(any(OnFailureListener.class)))
                .thenReturn(mockTask);

        // Inject mocked db into the fragment
        setDbOnFragment(mockDb);

        // Act: call private deleteEvent(String eventId, View card)
        callDeleteEvent(eventId, card);

        // Assert: card was removed from container
        assertEquals(0, container.getChildCount());
    }

    // ============= reflection helpers =============

    @SuppressWarnings("unchecked")
    private List<Object> getAllEventsList() throws Exception {
        Field field = EventsFragment.class.getDeclaredField("allEvents");
        field.setAccessible(true);
        return (List<Object>) field.get(fragment);
    }

    private LinearLayout getEventsContainer() throws Exception {
        Field field = EventsFragment.class.getDeclaredField("eventsContainer");
        field.setAccessible(true);
        return (LinearLayout) field.get(fragment);
    }

    private void callFilterEvents(String query) throws Exception {
        Method method = EventsFragment.class.getDeclaredMethod("filterEvents", String.class);
        method.setAccessible(true);
        method.invoke(fragment, query);
    }

    private void setDbOnFragment(FirebaseFirestore firestore) throws Exception {
        Field field = EventsFragment.class.getDeclaredField("db");
        field.setAccessible(true);
        field.set(fragment, firestore);
    }

    private void callDeleteEvent(String eventId, View card) throws Exception {
        Method method = EventsFragment.class.getDeclaredMethod("deleteEvent", String.class, View.class);
        method.setAccessible(true);
        method.invoke(fragment, eventId, card);
    }

    /**
     * Create an instance of the private static inner class EventsFragment.Event
     * using reflection.
     */
    private Object newEvent(
            String id,
            String title,
            String posterUrl,
            String location,
            Long capacity,
            Long waitlist,
            int waitlistCount
    ) throws Exception {
        Class<?> eventClass =
                Class.forName("com.example.apollo.ui.admin.events.EventsFragment$Event");
        Constructor<?> ctor = eventClass.getDeclaredConstructor(
                String.class, String.class, String.class,
                String.class, Long.class, Long.class, int.class
        );
        ctor.setAccessible(true);
        return ctor.newInstance(id, title, posterUrl, location, capacity, waitlist, waitlistCount);
    }
}
