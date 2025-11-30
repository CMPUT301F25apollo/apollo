package com.example.apollo.ui.entrant.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.apollo.R;
import com.example.apollo.ui.login.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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
 * Outstanding Issues:
 * - Add input validation for email and phone formats.
 * - Improve error handling for Firestore operations.
 * - Consider adding a loading indicator while profile data is being fetched or saved.
 */
public class SettingsFragment extends Fragment {

    private EditText editName, editUsername, editEmail, editPhone;
    private Button saveButton, deleteAccButton;
    private Switch notificationsSwitch;

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
        deleteAccButton = view.findViewById(R.id.Deleteaccountbtn);


        if (currentUser != null) {
            loadUserProfile();
        }

        saveButton.setOnClickListener(v -> saveUserProfile());
        deleteAccButton.setOnClickListener(v -> deleteAccount());
        notificationsSwitch = view.findViewById(R.id.switchNotifications);

        if (currentUser != null) {
            loadUserProfile();
            loadNotificationSetting();
        }

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateNotificationSetting(isChecked);
        });
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

        // Username validation
        if (username.length() < 5) {
            editUsername.setError("Username must be at least 5 characters long");
            editUsername.requestFocus();
            return;
        }

        // Phone validation (if entered it must be 10 digits)
        if (!phone.isEmpty() && !phone.matches("\\d{10}")) {
            editPhone.setError("Phone number must be exactly 10 digits");
            editPhone.requestFocus();
            return;
        }

        // email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Please enter a valid email address");
            editEmail.requestFocus();
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

    private void deleteAccount() {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return; // Should not happen if the user is on this screen
        }

        final EditText passwordEditText = new EditText(getContext());
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(requireContext())
                .setTitle("Re-authenticate to Delete Account")
                .setMessage("Please enter your password to confirm.")
                .setView(passwordEditText)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String password = passwordEditText.getText().toString();
                    if (password.isEmpty()) {
                        Toast.makeText(getContext(), "Password is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reauthenticateAndDelete(user, password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadNotificationSetting() {
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());

        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Boolean enabled = snapshot.getBoolean("notificationsEnabled");

                // default = true if not set
                if (enabled == null) enabled = true;

                notificationsSwitch.setChecked(enabled);
            }
        });
    }

    private void updateNotificationSetting(boolean enabled) {
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());

        userRef.update("notificationsEnabled", enabled)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Notification preference updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to update setting", Toast.LENGTH_SHORT).show());
    }

    private void reauthenticateAndDelete(FirebaseUser user, String password) {
        String email = user.getEmail();
        if (email == null) {
            Toast.makeText(getContext(), "Could not get user email.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        db.collection("users").document(user.getUid())
                                .delete()
                                .addOnCompleteListener(firestoreTask -> {
                                    if (firestoreTask.isSuccessful()) {
                                        user.delete()
                                                .addOnCompleteListener(authTask -> {
                                                    if (authTask.isSuccessful()) {
                                                        Log.d("SettingsFragment", "User account deleted.");
                                                        Toast.makeText(getContext(), "Account deleted successfully.", Toast.LENGTH_LONG).show();
                                                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                        getActivity().finish();
                                                    } else {
                                                        Log.w("SettingsFragment", "Error deleting user account.", authTask.getException());
                                                        Toast.makeText(getContext(), "Failed to delete account.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        Log.w("SettingsFragment", "Error deleting user data from Firestore.", firestoreTask.getException());
                                        Toast.makeText(getContext(), "Failed to delete user data.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Log.w("SettingsFragment", "Re-authentication failed.", reauthTask.getException());
                        Toast.makeText(getContext(), "Re-authentication failed. Please check your password.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
