package com.example.apollo.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.apollo.MainActivity;
import com.example.apollo.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * LoginActivity.java
 *
 * Purpose:
 * Handles the login screen of the application. Allows users to sign in
 * using email and password through Firebase Authentication. Also provides
 * options to sign up or continue as a guest.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern, connecting user input from
 * the login view with Firebase Authentication (the model).
 *
 * Notes:
 * - Could include input validation improvements.
 * - Consider adding a "Forgot Password" option in the future.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewSignUp, textViewGuest;

    private FirebaseAuth mAuth;

    /**
     * Called when the activity is created.
     * Checks if a user is already signed in, initializes UI components,
     * and sets up listeners for login, sign-up, and guest mode actions.
     *
     * @param savedInstanceState The saved state of the activity, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Check if a user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Show the login screen
        setContentView(R.layout.activity_login);

        // Connect UI components
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewSignUp = findViewById(R.id.textViewSignUp);
        textViewGuest = findViewById(R.id.textViewGuest);

        // Handle login button click
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            /**
             * Handles the login button click event.
             * Validates input and performs Firebase Authentication.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                // Check if email or password is empty
                if (TextUtils.isEmpty(email)) {
                    editTextEmail.setError("Email is required.");
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    editTextPassword.setError("Password is required.");
                    return;
                }

                // Try logging in using Firebase Authentication
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            /**
                             * Called when Firebase sign-in completes.
                             * If successful, navigates to MainActivity.
                             * Otherwise, shows an error message.
                             *
                             * @param task The authentication task.
                             */
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this,
                                            "Login successful.", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "Authentication failed: " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        // Handle sign-up text click
        textViewSignUp.setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the SignUpActivity when the sign-up text is clicked.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });

        // Handle guest mode text click
        textViewGuest.setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the MainActivity in guest mode when clicked.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("isGuest", true);
                startActivity(intent);
            }
        });
    }
}
