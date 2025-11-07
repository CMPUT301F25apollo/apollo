package com.example.apollo.ui.organizer.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.apollo.databinding.FragmentEditProfileOrganizerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * EditProfileFragment.java
 *
 * Purpose:
 * Allows organizers to view and update their profile information.
 * Loads existing data from Firestore and saves any changes made by the user.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern.
 * Communicates with Firestore (Model) and updates the user interface (View).
 *
 * Notes:
 * - Validates that required fields are filled before saving.
 * - Displays success or error messages using Toasts.
 */
public class EditProfileFragment extends Fragment {

    private FragmentEditProfileOrganizerBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    /**
     * Called when the fragment’s view is created.
     * Sets up Firestore, loads user data, and initializes button actions.
     *
     * @param inflater Used to inflate the layout.
     * @param container The parent view the fragment attaches to.
     * @param savedInstanceState The saved instance state, if any.
     * @return The root view of the fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentEditProfileOrganizerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Load profile information if user is signed in
        if (currentUser != null) {
            loadUserProfile();
        }

        // Save button: saves updates to Firestore
        binding.saveButton.setOnClickListener(v -> saveUserProfile());

        // Cancel button: returns to previous screen
        binding.cancelButton.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        return root;
    }

    /**
     * Loads the current organizer’s profile information from Firestore
     * and displays it in the corresponding input fields.
     */
    private void loadUserProfile() {
        DocumentReference docRef = db.collection("Organizers").document(currentUser.getUid());
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                binding.editName.setText(documentSnapshot.getString("name"));
                binding.editUsername.setText(documentSnapshot.getString("username"));
                binding.editEmail.setText(documentSnapshot.getString("email"));
                binding.editPhone.setText(documentSnapshot.getString("phone"));
            }
        });
    }

    /**
     * Validates user input and saves updated profile information to Firestore.
     * Displays a confirmation or error message.
     */
    private void saveUserProfile() {
        String name = binding.editName.getText().toString().trim();
        String username = binding.editUsername.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String phone = binding.editPhone.getText().toString().trim();

        if (name.isEmpty() || username.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("username", username);
        user.put("email", email);
        user.put("phone", phone);

        db.collection("Organizers").document(currentUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(binding.getRoot()).navigateUp();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error updating profile", Toast.LENGTH_SHORT).show());
    }

    /**
     * Cleans up view binding when the fragment is destroyed to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
