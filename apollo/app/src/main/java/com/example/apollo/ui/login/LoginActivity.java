package com.example.apollo.ui.login;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.apollo.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * LoginActivity.java
 *
 * A lightweight entry activity responsible for routing the user
 * to the correct screen based on their authentication state.
 *
 * Behavior:
 * - If a Firebase user is already logged in → go directly to MainActivity.
 * - If not logged in → redirect to SignUpActivity.
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            // No session redirect to sign-up
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        }
    }
}
