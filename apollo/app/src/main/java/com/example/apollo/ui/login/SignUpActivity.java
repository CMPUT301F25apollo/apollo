package com.example.apollo.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.apollo.MainActivity;
import com.example.apollo.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import android.widget.ImageButton;

/**
 * SignUpActivity.java
 *
 * Purpose:
 * This activity handles user registration using Firebase Authentication and Firestore.
 * It collects user information, validates inputs, and stores profile data in the database.
 * Users can also choose whether to register as an organizer or entrant.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern, connecting user input from the sign-up view
 * to the Firebase model that stores user data.
 *
 * Notes:
 * - Phone number and email validations could be expanded.
 * - Consider adding stronger password requirements.
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword, editTextPhoneNumber, editTextUsername;
    private CheckBox checkboxOrganizer;
    private Button buttonSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    /**
     * Called when the activity is created.
     * Initializes Firebase, sets up input fields, and handles the sign-up process.
     *
     * @param savedInstanceState The saved state of the activity, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        editTextName = findViewById(R.id.editTextName);
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextUsername = findViewById(R.id.editTextUsername);
        checkboxOrganizer = findViewById(R.id.checkbox_organizer);
        buttonSignUp = findViewById(R.id.buttonSignUp);

        // Handle sign-up button click
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            /**
             * Handles user registration.
             * Validates input fields and registers the user using Firebase Authentication.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                String name = editTextName.getText().toString().trim();
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                String phoneNumber = editTextPhoneNumber.getText().toString().trim();
                String username = editTextUsername.getText().toString().trim();
                boolean isOrganizer = checkboxOrganizer.isChecked();

                // Check for empty required fields
                if (TextUtils.isEmpty(name)) {
                    editTextName.setError("Name is required.");
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    editTextEmail.setError("Email is required.");
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    editTextPassword.setError("Password is required.");
                    return;
                }

                if (TextUtils.isEmpty(username)) {
                    editTextUsername.setError("Username is required.");
                    return;
                }

                // Validate username length
                if (username.length() < 5) {
                    editTextUsername.setError("Username must be at least 5 characters long");
                    return;
                }

                // Optional: Validate phone number (10 digits or empty)
                if (!TextUtils.isEmpty(phoneNumber) && !phoneNumber.matches("\\d{10}")) {
                    editTextPhoneNumber.setError("Phone number must be exactly 10 digits or left blank");
                    return;
                }

                // Validate email format
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    editTextEmail.setError("Please enter a valid email address");
                    return;
                }

                // Create user with Firebase Authentication
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            /**
                             * Called when Firebase account creation completes.
                             * If successful, saves the user's profile information in Firestore.
                             *
                             * @param task The result of the Firebase authentication attempt.
                             */
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    String userId = mAuth.getCurrentUser().getUid();

                                    // Create a user profile map
                                    Map<String, Object> user = new HashMap<>();
                                    user.put("name", name);
                                    user.put("email", email);
                                    user.put("phone", phoneNumber);
                                    user.put("username", username);
                                    String role = isOrganizer ? "organizer" : "entrant";
                                    user.put("role", role);

                                    // Save profile data to Firestore
                                    db.collection("users").document(userId)
                                            .set(user)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                /**
                                                 * Called when Firestore write completes.
                                                 * Shows success or error messages.
                                                 *
                                                 * @param task The Firestore write task result.
                                                 */
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(SignUpActivity.this,
                                                                "Sign up successful.", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                                        finish();
                                                    } else {
                                                        Toast.makeText(SignUpActivity.this,
                                                                "Error: " + task.getException().getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(SignUpActivity.this,
                                            "Authentication failed: " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}
