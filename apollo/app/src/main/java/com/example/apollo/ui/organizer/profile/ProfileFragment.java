package com.example.apollo.ui.organizer.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.apollo.R;
import com.example.apollo.ui.login.LoginActivity;
import com.example.apollo.databinding.FragmentProfileOrganizerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * ProfileFragment.java
 *
 * Purpose:
 * Displays the organizer’s profile information such as name, username, email, and phone.
 * Provides options to log out or navigate to the edit profile screen.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern.
 * Retrieves user data from Firestore (Model) and displays it in the layout (View).
 *
 * Notes:
 * - Uses a Firestore snapshot listener for real-time updates.
 * - Clears the activity stack after logout to prevent navigation back.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileOrganizerBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    /**
     * Called when the fragment’s view is created.
     * Initializes Firebase, loads user information, and sets up button listeners.
     *
     * @param inflater Used to inflate the layout.
     * @param container The parent view the fragment attaches to.
     * @param savedInstanceState Saved state, if available.
     * @return The root view for this fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileOrganizerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Load profile info if user is signed in
        if (currentUser != null) {
            loadUserProfile();
        }

        // Edit button: navigates to the edit profile fragment
        binding.editProfileButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_navigation_profile_to_editProfileFragment);
        });

        return root;
    }

    /**
     * Loads and listens for changes to the organizer’s profile information from Firestore.
     * Updates the UI fields when data changes.
     */
    private void loadUserProfile() {
        DocumentReference docRef = db.collection("Organizers").document(currentUser.getUid());
        docRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                return; // Stop if there's an error
            }

            if (snapshot != null && snapshot.exists()) {
                binding.profileName.setText(snapshot.getString("name"));
                binding.profileUsername.setText(snapshot.getString("username"));
                binding.profileEmail.setText(snapshot.getString("email"));
                binding.profilePhone.setText(snapshot.getString("phone"));
            }
        });
    }

    /**
     * Cleans up view binding to prevent memory leaks when the fragment is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
