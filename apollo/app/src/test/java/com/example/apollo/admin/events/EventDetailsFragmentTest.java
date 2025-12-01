package com.example.apollo.admin.events;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.example.apollo.R;
import com.example.apollo.ui.admin.events.EventDetailsFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Config(sdk = 34)
@RunWith(RobolectricTestRunner.class)
public class EventDetailsFragmentTest {

    private FragmentActivity activity;
    private EventDetailsFragment fragment;

    @Before
    public void setUp() {
        // Initialize Firebase for Robolectric so FirebaseFirestore.getInstance() works
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }

        activity = Robolectric.buildActivity(FragmentActivity.class)
                .setup()
                .get();

        fragment = new EventDetailsFragment();
        // No arguments -> onCreateView will NOT auto-call loadEventDetails()
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment, "eventDetails")
                .commitNow();
    }

    @Test
    public void loadEventDetails_populatesViews_correctly() throws Exception {
        // Arrange
        String eventId = "event123";

        // Mock Firestore pieces
        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        CollectionReference mockEventsCollection = mock(CollectionReference.class);
        DocumentReference mockDocRef = mock(DocumentReference.class);

        @SuppressWarnings("unchecked")
        Task<DocumentSnapshot> mockDocTask = (Task<DocumentSnapshot>) mock(Task.class);
        DocumentSnapshot mockDocSnapshot = mock(DocumentSnapshot.class);

        CollectionReference mockWaitlistCollection = mock(CollectionReference.class);
        @SuppressWarnings("unchecked")
        Task<QuerySnapshot> mockWaitlistTask = (Task<QuerySnapshot>) mock(Task.class);
        QuerySnapshot mockWaitlistSnapshot = mock(QuerySnapshot.class);

        // db.collection("events")
        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        // .document(eventId)
        when(mockEventsCollection.document(eventId)).thenReturn(mockDocRef);
        // .get()
        when(mockDocRef.get()).thenReturn(mockDocTask);

        // When addOnSuccessListener is attached to the doc Task, immediately call it with our mock DocumentSnapshot
        when(mockDocTask.addOnSuccessListener(any(OnSuccessListener.class)))
                .thenAnswer(invocation -> {
                    OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
                    listener.onSuccess(mockDocSnapshot);
                    return mockDocTask;
                });
        // Just return the task for failure (won't be used here)
        when(mockDocTask.addOnFailureListener(any(OnFailureListener.class)))
                .thenReturn(mockDocTask);

        // Stub document fields
        when(mockDocSnapshot.exists()).thenReturn(true);
        when(mockDocSnapshot.getString("title")).thenReturn("Sample Event");
        when(mockDocSnapshot.getString("description")).thenReturn("This is a sample event.");
        when(mockDocSnapshot.getString("location")).thenReturn("SUB");
        when(mockDocSnapshot.getString("date")).thenReturn("2024-10-10");
        when(mockDocSnapshot.getString("time")).thenReturn("6:00 PM");
        when(mockDocSnapshot.getString("registrationOpen")).thenReturn("2024-09-01");
        when(mockDocSnapshot.getString("registrationClose")).thenReturn("2024-10-01");
        when(mockDocSnapshot.getLong("eventCapacity")).thenReturn(100L);
        when(mockDocSnapshot.getLong("waitlistCapacity")).thenReturn(20L);
        when(mockDocSnapshot.getDouble("price")).thenReturn(15.5);
        // posterUrl null to skip Glide
        when(mockDocSnapshot.getString("eventPosterUrl")).thenReturn(null);

        // Now mock the nested waitlist query:
        when(mockDocRef.collection("waitlist")).thenReturn(mockWaitlistCollection);
        when(mockWaitlistCollection.get()).thenReturn(mockWaitlistTask);

        when(mockWaitlistTask.addOnSuccessListener(any(OnSuccessListener.class)))
                .thenAnswer(invocation -> {
                    OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
                    listener.onSuccess(mockWaitlistSnapshot);
                    return mockWaitlistTask;
                });
        when(mockWaitlistTask.addOnFailureListener(any(OnFailureListener.class)))
                .thenReturn(mockWaitlistTask);

        // 2 people on waitlist
        when(mockWaitlistSnapshot.size()).thenReturn(2);

        // Inject mocked db into fragment
        setDbOnFragment(mockDb);

        // Act: call private loadEventDetails(eventId)
        callLoadEventDetails(eventId);

        // Assert: check views
        TextView titleView = activity.findViewById(R.id.textEventTitle);
        TextView descriptionView = activity.findViewById(R.id.textEventDescription);
        TextView summaryView = activity.findViewById(R.id.textEventSummary);
        TextView waitlistView = activity.findViewById(R.id.textWaitlistCount);

        assertEquals("Sample Event", titleView.getText().toString());
        assertEquals("This is a sample event.", descriptionView.getText().toString());

        String expectedSummary =
                " Location: SUB" +
                        "\n Date: 2024-10-10" +
                        "\n Time: 6:00 PM" +
                        "\n Price: $15.5" +
                        "\n Registration Opens: 2024-09-01" +
                        "\n Registration Closes: 2024-10-01" +
                        "\n Event Capacity: 100" +
                        "\n Waitlist Capacity: 20";

        assertEquals(expectedSummary, summaryView.getText().toString());
        assertEquals("Waitlist: 2/20", waitlistView.getText().toString());
    }

    // ================== reflection helpers ==================

    private void setDbOnFragment(FirebaseFirestore firestore) throws Exception {
        Field field = EventDetailsFragment.class.getDeclaredField("db");
        field.setAccessible(true);
        field.set(fragment, firestore);
    }

    private void callLoadEventDetails(String eventId) throws Exception {
        Method method = EventDetailsFragment.class
                .getDeclaredMethod("loadEventDetails", String.class);
        method.setAccessible(true);
        method.invoke(fragment, eventId);
    }
}
