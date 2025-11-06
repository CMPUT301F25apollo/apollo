package com.example.apollo.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

public class SignUpActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword, editTextPhoneNumber, editTextUsername;
    private CheckBox checkboxOrganizer;
    private Button buttonSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextUsername = findViewById(R.id.editTextUsername);
        checkboxOrganizer = findViewById(R.id.checkbox_organizer);
        buttonSignUp = findViewById(R.id.buttonSignUp);

        // Handle sign up
        buttonSignUp.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        boolean isOrganizer = checkboxOrganizer.isChecked();

        // -------- Validation checks --------
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
        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters.");
            return;
        }
        if (TextUtils.isEmpty(phoneNumber)) {
            editTextPhoneNumber.setError("Phone number is required.");
            return;
        }
        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError("Username is required.");
            return;
        }

        // -------- Firebase Authentication --------
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // ✅ Authentication successful
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(userId, name, email, phoneNumber, username, isOrganizer);
                    } else {
                        // ❌ Authentication failed
                        String errorMsg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Unknown error";
                        Toast.makeText(SignUpActivity.this,
                                "Authentication failed: " + errorMsg,
                                Toast.LENGTH_LONG).show();
                        Log.e("SignUp", "Auth failed", task.getException());
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email,
                                     String phoneNumber, String username, boolean isOrganizer) {

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phoneNumber", phoneNumber);
        userData.put("username", username);
        userData.put("isOrganizer", isOrganizer);
        userData.put("createdAt", System.currentTimeMillis());

        // Save under "users" collection
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // Also mirror under "organizers" if applicable
                    if (isOrganizer) {
                        db.collection("organizers").document(userId)
                                .set(userData)
                                .addOnSuccessListener(unused -> Log.d("SignUp", "Organizer added"))
                                .addOnFailureListener(e -> Log.e("SignUp", "Failed to add organizer", e));
                    }

                    Toast.makeText(SignUpActivity.this,
                            "Sign-up successful!",
                            Toast.LENGTH_SHORT).show();

                    // Go to main screen
                    startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this,
                            "Failed to save user: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e("SignUp", "Firestore save failed", e);
                });
    }
}
