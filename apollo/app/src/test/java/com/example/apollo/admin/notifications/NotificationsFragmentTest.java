package com.example.apollo.admin.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.example.apollo.R;
import com.example.apollo.ui.admin.notifications.NotificationsFragment;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;

@Config(sdk = 34)
@RunWith(RobolectricTestRunner.class)
public class NotificationsFragmentTest {

    private FragmentActivity activity;
    private NotificationsFragment fragment;

    @Before
    public void setUp() {
        // Ensure FirebaseApp exists for Firestore.getInstance()
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }

        activity = Robolectric.buildActivity(FragmentActivity.class)
                .setup()
                .get();

        fragment = new NotificationsFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment, "notifications")
                .commitNow();
    }

    // --- Helpers to access private stuff via reflection ---

    private LinearLayout getEventsContainer() throws Exception {
        Field field = NotificationsFragment.class.getDeclaredField("eventsContainer");
        field.setAccessible(true);
        return (LinearLayout) field.get(fragment);
    }

    private void callAddLogCard(QueryDocumentSnapshot doc) throws Exception {
        Method m = NotificationsFragment.class.getDeclaredMethod(
                "addLogCard",
                QueryDocumentSnapshot.class
        );
        m.setAccessible(true);
        m.invoke(fragment, doc);
    }

    private void callAddEmptyMessage() throws Exception {
        Method m = NotificationsFragment.class.getDeclaredMethod("addEmptyMessage");
        m.setAccessible(true);
        m.invoke(fragment);
    }

    private void callUpdateMeta(TextView meta, String key, String value) throws Exception {
        Method m = NotificationsFragment.class.getDeclaredMethod(
                "updateMeta",
                TextView.class,
                String.class,
                String.class
        );
        m.setAccessible(true);
        m.invoke(fragment, meta, key, value);
    }

    // --- Tests ---

    @Test
    public void addLogCard_populatesTitleMessageAndInitialMeta() throws Exception {
        // Arrange: mock firestore document
        QueryDocumentSnapshot doc = Mockito.mock(QueryDocumentSnapshot.class);

        Mockito.when(doc.getString("notificationTitle")).thenReturn("Test Title");
        Mockito.when(doc.getString("notificationMessage")).thenReturn("Body message");
        Mockito.when(doc.getString("eventId")).thenReturn("event123");
        Mockito.when(doc.getString("organizerId")).thenReturn("org456");
        Mockito.when(doc.getString("recipientId")).thenReturn("user789");
        Mockito.when(doc.getString("notificationType")).thenReturn("INVITE");
        Mockito.when(doc.getTimestamp("timestamp"))
                .thenReturn(new Timestamp(new Date()));

        LinearLayout container = getEventsContainer();
        container.removeAllViews();

        // Act
        callAddLogCard(doc);

        // Assert: exactly one card added
        assertEquals(1, container.getChildCount());

        TextView titleView = container.getChildAt(0).findViewById(R.id.logTitle);
        TextView messageView = container.getChildAt(0).findViewById(R.id.logMessage);
        TextView metaView = container.getChildAt(0).findViewById(R.id.logMeta);

        assertEquals("Test Title", titleView.getText().toString());
        assertEquals("Body message", messageView.getText().toString());

        String metaText = metaView.getText().toString();
        // We only guarantee that initial placeholders + type + sent line are present.
        assertTrue(metaText.contains("Event: loading..."));
        assertTrue(metaText.contains("Type: INVITE"));
        assertTrue(metaText.contains("From: loading..."));
        assertTrue(metaText.contains("To: loading..."));
        assertTrue(metaText.contains("Sent:"));
    }

    @Test
    public void updateMeta_replacesPlaceholderCorrectly() throws Exception {
        // Arrange
        TextView meta = new TextView(activity);
        meta.setText(
                "Event: loading..." +
                        "\nType: INVITE" +
                        "\nFrom: loading..." +
                        "\nTo: loading..." +
                        "\nSent: some time"
        );

        // Act
        callUpdateMeta(meta, "Event", "Sip & Paint");

        // Assert
        String updated = meta.getText().toString();
        assertTrue(updated.contains("Event: Sip & Paint"));
        // Other placeholders should still be there
        assertTrue(updated.contains("From: loading..."));
        assertTrue(updated.contains("To: loading..."));
    }

    @Test
    public void addEmptyMessage_addsNoLogsTextView() throws Exception {
        LinearLayout container = getEventsContainer();
        container.removeAllViews();

        callAddEmptyMessage();

        assertEquals(1, container.getChildCount());
        TextView tv = (TextView) container.getChildAt(0);
        assertEquals("No logs found.", tv.getText().toString());
    }
}
