package com.example.apollo.admin.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.example.apollo.R;
import static org.mockito.ArgumentMatchers.any;
import com.example.apollo.ui.admin.profiles.ProfilesFragment;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;


@Config(sdk = 34)
@RunWith(RobolectricTestRunner.class)
public class ProfilesFragmentTest {

    private FragmentActivity activity;
    private ProfilesFragment fragment;

    @Before
    public void setUp() {
        // Initialize Firebase so FirebaseFirestore.getInstance() works in Robolectric
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }

        activity = Robolectric.buildActivity(FragmentActivity.class)
                .setup()
                .get();

        fragment = new ProfilesFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment, "profiles")
                .commitNow();
    }

    @Test
    public void filterProfiles_showsAllProfiles_whenQueryEmpty() throws Exception {
        List<DocumentSnapshot> allProfiles = getAllProfilesList();
        allProfiles.clear();

        allProfiles.add(mockProfile(
                "id1",
                "Amarachi Okafor",
                "amarachi",
                "ama@example.com",
                "organizer",
                "111-111-1111"
        ));
        allProfiles.add(mockProfile(
                "id2",
                "Science Club",
                "scienceclub",
                "sci@example.com",
                "entrant",
                "222-222-2222"
        ));
        allProfiles.add(mockProfile(
                "id3",
                "Basketball Admin",
                "balleradmin",
                "baller@example.com",
                "admin",
                "333-333-3333"
        ));

        // Empty query -> show all
        callFilterProfiles("");

        LinearLayout container = getProfilesContainer();
        assertEquals(3, container.getChildCount());
    }

    @Test
    public void filterProfiles_filtersByNameOrUsername_caseInsensitive() throws Exception {
        List<DocumentSnapshot> allProfiles = getAllProfilesList();
        allProfiles.clear();

        allProfiles.add(mockProfile(
                "id1",
                "Amarachi Okafor",
                "amarachi",
                "ama@example.com",
                "organizer",
                "111-111-1111"
        ));
        allProfiles.add(mockProfile(
                "id2",
                "Science Club",
                "scienceclub",
                "sci@example.com",
                "entrant",
                "222-222-2222"
        ));
        allProfiles.add(mockProfile(
                "id3",
                "Basketball Admin",
                "balleradmin",
                "baller@example.com",
                "admin",
                "333-333-3333"
        ));

        // Search by partial name/username
        callFilterProfiles("ama");

        LinearLayout container = getProfilesContainer();
        assertEquals(1, container.getChildCount());

        TextView nameView = container.getChildAt(0).findViewById(R.id.profile_name);
        String shownName = nameView.getText().toString();
        assertTrue(shownName.toLowerCase().contains("ama"));
    }

    @Test
    public void filterProfiles_showsNone_whenNoMatches() throws Exception {
        List<DocumentSnapshot> allProfiles = getAllProfilesList();
        allProfiles.clear();

        allProfiles.add(mockProfile(
                "id1",
                "Amarachi Okafor",
                "amarachi",
                "ama@example.com",
                "organizer",
                "111-111-1111"
        ));
        allProfiles.add(mockProfile(
                "id2",
                "Science Club",
                "scienceclub",
                "sci@example.com",
                "entrant",
                "222-222-2222"
        ));

        // Query that matches nobody
        callFilterProfiles("zzz-not-here");

        LinearLayout container = getProfilesContainer();
        assertEquals(0, container.getChildCount());
    }


    @Test
    public void deleteProfile_removesCardFromContainer_onSuccess() throws Exception {
        // Arrange: one fake profile
        List<DocumentSnapshot> allProfiles = getAllProfilesList();
        allProfiles.clear();

        DocumentSnapshot doc = mockProfile(
                "id1",
                "Admin User",
                "adminuser",
                "admin@example.com",
                "Admin",
                "999-999-9999"
        );
        allProfiles.add(doc);

        // Add its card to the container via the real method
        callAddProfileCard(doc);
        LinearLayout container = getProfilesContainer();
        assertEquals(1, container.getChildCount());
        View card = container.getChildAt(0);

        // Mock Firestore + delete() chain
        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        CollectionReference mockUsers = mock(CollectionReference.class);
        DocumentReference mockDocRef = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> mockTask = (Task<Void>) mock(Task.class);

        when(mockDb.collection("users")).thenReturn(mockUsers);
        when(mockUsers.document("id1")).thenReturn(mockDocRef);
        when(mockDocRef.delete()).thenReturn(mockTask);

        // When addOnSuccessListener is called, immediately invoke the listener
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class)))
                .thenAnswer(invocation -> {
                    OnSuccessListener<Void> listener = invocation.getArgument(0);
                    listener.onSuccess(null);
                    return mockTask;
                });

        // Inject mocked Firestore into the fragment
        setDbOnFragment(mockDb);

        // Act: call deleteProfile(doc, card) via reflection
        callDeleteProfile(doc, card);

        // Assert: card is removed from container
        assertEquals(0, container.getChildCount());
    }


    private void setDbOnFragment(FirebaseFirestore firestore) throws Exception {
        Field field = ProfilesFragment.class.getDeclaredField("db");
        field.setAccessible(true);
        field.set(fragment, firestore);
    }

    private void callAddProfileCard(DocumentSnapshot doc) throws Exception {
        Method method = ProfilesFragment.class.getDeclaredMethod("addProfileCard", DocumentSnapshot.class);
        method.setAccessible(true);
        method.invoke(fragment, doc);
    }

    private void callDeleteProfile(DocumentSnapshot doc, View card) throws Exception {
        Method method = ProfilesFragment.class.getDeclaredMethod("deleteProfile", DocumentSnapshot.class, View.class);
        method.setAccessible(true);
        method.invoke(fragment, doc, card);
    }



    // ================== reflection helpers ==================

    @SuppressWarnings("unchecked")
    private List<DocumentSnapshot> getAllProfilesList() throws Exception {
        Field field = ProfilesFragment.class.getDeclaredField("allProfiles");
        field.setAccessible(true);
        return (List<DocumentSnapshot>) field.get(fragment);
    }

    private LinearLayout getProfilesContainer() throws Exception {
        Field field = ProfilesFragment.class.getDeclaredField("profilesContainer");
        field.setAccessible(true);
        return (LinearLayout) field.get(fragment);
    }

    private void callFilterProfiles(String query) throws Exception {
        Method method = ProfilesFragment.class.getDeclaredMethod("filterProfiles", String.class);
        method.setAccessible(true);
        method.invoke(fragment, query);
    }

    private DocumentSnapshot mockProfile(
            String id,
            String name,
            String username,
            String email,
            String role,
            String phone
    ) {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.getId()).thenReturn(id);
        when(doc.getString("name")).thenReturn(name);
        when(doc.getString("username")).thenReturn(username);
        when(doc.getString("email")).thenReturn(email);
        when(doc.getString("role")).thenReturn(role);
        when(doc.getString("phone")).thenReturn(phone);
        return doc;
    }
}