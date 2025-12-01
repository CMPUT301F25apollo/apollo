package com.example.apollo.admin.images;

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
import com.example.apollo.ui.admin.images.ImagesFragment;
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
public class ImagesFragmentTest {

    private FragmentActivity activity;
    private ImagesFragment fragment;

    @Before
    public void setUp() {
        // Initialize Firebase for Robolectric process
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }

        // Host activity
        activity = Robolectric.buildActivity(FragmentActivity.class)
                .setup()
                .get();

        // Attach fragment so onCreateView() runs
        fragment = new ImagesFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment, "images")
                .commitNow();
    }

    // ================== FILTER TESTS ==================

    @Test
    public void filterImages_showsAllImages_whenQueryEmpty() throws Exception {
        List<Object> allImages = getAllImagesList();
        allImages.clear();
        allImages.add(newImageItem("id1", "Welcome Party", "https://example.com/1.jpg"));
        allImages.add(newImageItem("id2", "Science Fair", "https://example.com/2.jpg"));
        allImages.add(newImageItem("id3", "Apollo Basketball Night", "https://example.com/3.jpg"));

        callFilterImages("");

        LinearLayout container = getImagesContainer();
        assertEquals(3, container.getChildCount());
    }

    @Test
    public void filterImages_filtersByTitleSubstring_caseInsensitive() throws Exception {
        List<Object> allImages = getAllImagesList();
        allImages.clear();
        allImages.add(newImageItem("id1", "Welcome Party", "https://example.com/1.jpg"));
        allImages.add(newImageItem("id2", "Science Fair", "https://example.com/2.jpg"));
        allImages.add(newImageItem("id3", "Apollo Basketball Night", "https://example.com/3.jpg"));

        callFilterImages("party");

        LinearLayout container = getImagesContainer();
        assertEquals(1, container.getChildCount());

        TextView titleView = container.getChildAt(0).findViewById(R.id.eventTitle);
        String titleText = titleView.getText().toString();
        assertTrue(titleText.toLowerCase().contains("party"));
    }

    // ================== DELETE TEST ==================

    @Test
    public void deleteImage_removesCardFromContainer_onSuccess() throws Exception {
        // Arrange: start with one dummy card in the container
        LinearLayout container = getImagesContainer();
        View card = new View(activity);
        container.addView(card);
        assertEquals(1, container.getChildCount());

        String eventId = "event123";

        // Mock Firestore and update() chain
        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        CollectionReference mockEvents = mock(CollectionReference.class);
        DocumentReference mockDocRef = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> mockTask = (Task<Void>) mock(Task.class);

        when(mockDb.collection("events")).thenReturn(mockEvents);
        when(mockEvents.document(eventId)).thenReturn(mockDocRef);
        when(mockDocRef.update(eq("eventPosterUrl"), eq(""))).thenReturn(mockTask);

        // When addOnSuccessListener is called, immediately trigger success
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class)))
                .thenAnswer(invocation -> {
                    OnSuccessListener<Void> listener = invocation.getArgument(0);
                    listener.onSuccess(null);
                    return mockTask;
                });

        // Just return the same task for failure listener (won't be used here)
        when(mockTask.addOnFailureListener(any(OnFailureListener.class)))
                .thenReturn(mockTask);

        // Inject mocked db into fragment
        setDbOnFragment(mockDb);

        // Act: call private deleteImage(String eventId, View card)
        callDeleteImage(eventId, card);

        // Assert: card removed from container
        assertEquals(0, container.getChildCount());
    }

    // ============= reflection helpers =============

    @SuppressWarnings("unchecked")
    private List<Object> getAllImagesList() throws Exception {
        Field field = ImagesFragment.class.getDeclaredField("allImages");
        field.setAccessible(true);
        return (List<Object>) field.get(fragment);
    }

    private LinearLayout getImagesContainer() throws Exception {
        Field field = ImagesFragment.class.getDeclaredField("imagesContainer");
        field.setAccessible(true);
        return (LinearLayout) field.get(fragment);
    }

    private void callFilterImages(String query) throws Exception {
        Method method = ImagesFragment.class.getDeclaredMethod("filterImages", String.class);
        method.setAccessible(true);
        method.invoke(fragment, query);
    }

    private Object newImageItem(String id, String title, String posterUrl) throws Exception {
        Class<?> imageItemClass =
                Class.forName("com.example.apollo.ui.admin.images.ImagesFragment$ImageItem");
        Constructor<?> ctor = imageItemClass.getDeclaredConstructor(
                String.class, String.class, String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(id, title, posterUrl);
    }

    private void setDbOnFragment(FirebaseFirestore firestore) throws Exception {
        Field field = ImagesFragment.class.getDeclaredField("db");
        field.setAccessible(true);
        field.set(fragment, firestore);
    }

    private void callDeleteImage(String eventId, View card) throws Exception {
        Method method = ImagesFragment.class.getDeclaredMethod("deleteImage", String.class, View.class);
        method.setAccessible(true);
        method.invoke(fragment, eventId, card);
    }
}