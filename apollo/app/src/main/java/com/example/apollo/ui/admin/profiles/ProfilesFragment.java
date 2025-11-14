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

public class ProfilesFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout profilesContainer;
    private EditText searchInput;

    private final List<DocumentSnapshot> allProfiles = new ArrayList<>();

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

    // ---------------------------
    // LOAD ALL PROFILES
    // ---------------------------
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

    // ---------------------------
    // ADD PROFILE CARD
    // ---------------------------
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

        // DELETE BUTTON WITH CONFIRMATION
        card.findViewById(R.id.delete_button).setOnClickListener(v -> showDeleteConfirmation(doc, card));

        profilesContainer.addView(card);
    }

    // ---------------------------
    // SHOW "ARE YOU SURE?" DIALOG
    // ---------------------------
    private void showDeleteConfirmation(DocumentSnapshot doc, View card) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Profile?")
                .setMessage("Are you sure you want to permanently delete this user?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile(doc, card))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------------------
    // DELETE PROFILE FROM FIRESTORE
    // ---------------------------
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

    // ---------------------------
    // SEARCH FUNCTIONALITY
    // ---------------------------
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

    // ---------------------------
    // FILTER PROFILES
    // ---------------------------
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
