package com.example.apollo.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.apollo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * SettingsFragment.java
 *
 * Purpose:
 * Provides a user interface for viewing and updating the current user's profile information,
 * including name, username, email, and phone number. Retrieves and saves data from Firebase Firestore.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern, mediating between the view (EditText fields and Button)
 * and the model (Firestore user data).
 *
 * Outstanding Issues / TODOs:
 * - Add input validation for email and phone formats.
 * - Improve error handling for Firestore operations.
 * - Consider adding a loading indicator while profile data is being fetched or saved.
 */
public class SettingsFragment extends Fragment {

    private EditText editName, editUsername, editEmail, editPhone;
    private Button saveButton;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    /**
     * Inflates the layout for this fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views.
     * @param container The parent view that the fragment's UI should attach to.
     * @param savedInstanceState Bundle containing saved state, if any.
     * @return The root View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    /**
     * Called after the view has been created. Initializes UI components, Firestore instance,
     * loads user profile if a user is signed in, and sets up the save button click listener.
     *
     * @param view The View returned by onCreateView().
     * @param savedInstanceState Bundle containing saved state, if any.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        editName = view.findViewById(R.id.editName);
        editUsername = view.findViewById(R.id.editUsername);
        editEmail = view.findViewById(R.id.editEmail);
        editPhone = view.findViewById(R.id.editPhone);
        saveButton = view.findViewById(R.id.Savebtn);

        if (currentUser != null) {
            loadUserProfile();
        }

        saveButton.setOnClickListener(v -> saveUserProfile());
    }

    /**
     * Loads the current user's profile data from Firestore and populates the EditText fields.
     */
    private void loadUserProfile() {
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String username = documentSnapshot.getString("username");
                String email = documentSnapshot.getString("email");
                String phone = documentSnapshot.getString("phone");

                editName.setText(name);
                editUsername.setText(username);
                editEmail.setText(email);
                editPhone.setText(phone);
            }
        });
    }

    /**
     * Saves the updated profile information to Firestore. Displays Toast messages on success or failure.
     */
    private void saveUserProfile() {
        String name = editName.getText().toString().trim();
        String username = editUsername.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (name.isEmpty() || username.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("username", username);
        user.put("email", email);
        user.put("phone", phone);

        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.set(user)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating profile", Toast.LENGTH_SHORT).show());
    }
}
