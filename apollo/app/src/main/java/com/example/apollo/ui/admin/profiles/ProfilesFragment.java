/**
 * ProfilesFragment.java
 *
 * This fragment allows admin users to view and manage all user profiles.
 * It loads user documents from Firestore, displays them as cards, supports
 * live search by name or username, and lets admins delete profiles.
 */
package com.example.apollo.ui.admin.profiles;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.apollo.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays a list of user profiles for admins.
 * Each profile card shows basic account information and includes
 * a delete button with confirmation.
 */
public class ProfilesFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout profilesContainer;
    private EditText searchInput;

    /** In-memory list of all profiles, used for filtering. */
    private final List<DocumentSnapshot> allProfiles = new ArrayList<>();

    /**
     * Inflates the profiles layout, initializes Firestore and UI views,
     * loads the list of user profiles, and sets up the search bar.
     *
     * @param inflater  LayoutInflater used to inflate the fragment layout.
     * @param container Optional parent view group.
     * @param savedInstanceState Previously saved state (not used here).
     * @return The root view for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profiles_admin, container, false);

        db = FirebaseFirestore.getInstance();
        profilesContainer = view.findViewById(R.id.profilesContainer);
        searchInput = view.findViewById(R.id.search_input);

        loadProfiles();
        setupSearchBar();

        return view;
    }

    /**
     * Loads all user documents from the "users" collection in Firestore.
     * The results are stored in {@code allProfiles} and rendered as cards.
     */
    private void loadProfiles() {
        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    profilesContainer.removeAllViews();
                    allProfiles.clear();

                    allProfiles.addAll(query.getDocuments());

                    for (DocumentSnapshot doc : allProfiles) {
                        addProfileCard(doc);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load profiles", Toast.LENGTH_SHORT).show()
                );
    }


    /**
     * Creates and adds a single profile card view to the container using
     * information from the given Firestore document.
     *
     * @param doc Firestore document representing one user profile.
     */
    private void addProfileCard(DocumentSnapshot doc) {
        View card = getLayoutInflater().inflate(R.layout.item_profile_card, profilesContainer, false);

        String name = doc.getString("name");
        String email = doc.getString("email");
        String phone = doc.getString("phone");
        String role = doc.getString("role");
        String username = doc.getString("username");

        ((TextView) card.findViewById(R.id.profile_name)).setText(name);
        ((TextView) card.findViewById(R.id.profile_email)).setText(email);
        ((TextView) card.findViewById(R.id.profile_role)).setText("Role: " + role);
        ((TextView) card.findViewById(R.id.profile_phone)).setText("Phone: " + phone);
        ((TextView) card.findViewById(R.id.profile_username)).setText("Username: " + username);

        // Delete button with confirmation dialog
        card.findViewById(R.id.delete_button).setOnClickListener(v -> showDeleteConfirmation(doc, card));

        profilesContainer.addView(card);
    }


    /**
     * Shows a confirmation dialog before deleting the given profile.
     *
     * @param doc  Firestore document for the user to delete.
     * @param card The card view to remove on successful deletion.
     */
    private void showDeleteConfirmation(DocumentSnapshot doc, View card) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Profile?")
                .setMessage("Are you sure you want to permanently delete this user?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile(doc, card))
                .setNegativeButton("Cancel", null)
                .show();
    }


    /**
     * Deletes the given user document from Firestore and removes its card
     * from the layout if the delete succeeds.
     *
     * @param doc  Firestore document to delete.
     * @param card Card view associated with this profile.
     */
    private void deleteProfile(DocumentSnapshot doc, View card) {
        db.collection("users")
                .document(doc.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile deleted", Toast.LENGTH_SHORT).show();
                    profilesContainer.removeView(card);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to delete profile", Toast.LENGTH_SHORT).show());
    }


    /**
     * Sets up the search bar so that profiles are filtered live as
     * the admin types by name or username.
     */
    private void setupSearchBar() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProfiles(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }



    /**
     * Filters the full list of profiles against the given query and
     * redraws the visible cards. Matches are done on name or username.
     *
     * @param query The search query entered by the admin.
     */
    private void filterProfiles(String query) {

        profilesContainer.removeAllViews();

        if (query.isEmpty()) {
            // Show all profiles again
            for (DocumentSnapshot doc : allProfiles) {
                addProfileCard(doc);
            }
            return;
        }

        String lowerQuery = query.toLowerCase();

        for (DocumentSnapshot doc : allProfiles) {
            String name = doc.getString("name");
            String username = doc.getString("username");

            if ((name != null && name.toLowerCase().contains(lowerQuery))
                    || (username != null && username.toLowerCase().contains(lowerQuery))) {

                addProfileCard(doc);
            }
        }
    }
}
