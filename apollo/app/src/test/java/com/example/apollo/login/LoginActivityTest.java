package com.example.apollo.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.example.apollo.MainActivity;
import com.example.apollo.R;
import com.example.apollo.ui.login.LoginActivity;
import com.example.apollo.ui.login.SignUpActivity;
import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@Config(sdk = 34)
@RunWith(RobolectricTestRunner.class)
public class LoginActivityTest {

    private LoginActivity activity;
    private EditText emailField;
    private EditText passwordField;
    private Button loginButton;
    private TextView signUpText;
    private TextView guestText;

    @Before
    public void setUp() {
        // Ensure FirebaseApp exists so FirebaseAuth doesn't crash
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }

        activity = Robolectric.buildActivity(LoginActivity.class)
                .setup()
                .get();

        emailField    = activity.findViewById(R.id.editTextEmail);
        passwordField = activity.findViewById(R.id.editTextPassword);
        loginButton   = activity.findViewById(R.id.buttonLogin);
        signUpText    = activity.findViewById(R.id.textViewSignUp);
        guestText     = activity.findViewById(R.id.textViewGuest);
    }

    @Test
    public void emptyEmail_showsEmailError() {
        emailField.setText("");                     // empty
        passwordField.setText("password123");       // filled

        loginButton.performClick();

        assertEquals("Email is required.", emailField.getError().toString());
    }

    @Test
    public void emptyPassword_showsPasswordError() {
        emailField.setText("test@example.com");     // filled
        passwordField.setText("");                  // empty

        loginButton.performClick();

        assertEquals("Password is required.", passwordField.getError().toString());
    }

    @Test
    public void validInputs_noImmediateFieldErrors() {
        emailField.setText("test@example.com");
        passwordField.setText("password123");

        loginButton.performClick();

        // We don't assert Firebase success, just that validation passed
        assertNull(emailField.getError());
        assertNull(passwordField.getError());
    }

    @Test
    public void clickingSignUp_startsSignUpActivity() {
        signUpText.performClick();

        ShadowActivity shadow = Shadows.shadowOf(activity);
        Intent nextIntent = shadow.getNextStartedActivity();
        assertNotNull("SignUp intent should be started", nextIntent);
        assertEquals(SignUpActivity.class.getName(),
                nextIntent.getComponent().getClassName());
    }

    @Test
    public void clickingGuest_startsMainActivityWithGuestExtra() {
        guestText.performClick();

        ShadowActivity shadow = Shadows.shadowOf(activity);
        Intent nextIntent = shadow.getNextStartedActivity();
        assertNotNull("Guest intent should be started", nextIntent);
        assertEquals(MainActivity.class.getName(),
                nextIntent.getComponent().getClassName());

        boolean isGuest = nextIntent.getBooleanExtra("isGuest", false);
        assertEquals(true, isGuest);
    }
}
