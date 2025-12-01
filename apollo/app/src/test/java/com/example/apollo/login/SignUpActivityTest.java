package com.example.apollo.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.widget.Button;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;

import com.example.apollo.R;
import com.example.apollo.ui.login.SignUpActivity;
import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(sdk = 34)
@RunWith(RobolectricTestRunner.class)
public class SignUpActivityTest {

    private SignUpActivity activity;
    private EditText nameField;
    private EditText emailField;
    private EditText passwordField;
    private EditText phoneField;
    private EditText usernameField;
    private Button signUpButton;

    @Before
    public void setUp() {
        // Make sure a FirebaseApp exists so FirebaseAuth/FirebaseFirestore don't crash
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }

        activity = Robolectric.buildActivity(SignUpActivity.class)
                .setup()
                .get();

        nameField     = activity.findViewById(R.id.editTextName);
        emailField    = activity.findViewById(R.id.editTextEmail);
        passwordField = activity.findViewById(R.id.editTextPassword);
        phoneField    = activity.findViewById(R.id.editTextPhoneNumber);
        usernameField = activity.findViewById(R.id.editTextUsername);
        signUpButton  = activity.findViewById(R.id.buttonSignUp);
    }

    @Test
    public void emptyName_showsNameError() {
        // Leave name empty
        nameField.setText("");

        // Fill other required fields so we reach the name check
        emailField.setText("test@example.com");
        passwordField.setText("password123");
        usernameField.setText("validUser");
        phoneField.setText(""); // optional

        signUpButton.performClick();

        assertEquals("Name is required.", nameField.getError().toString());
    }

    @Test
    public void emptyEmail_showsEmailError() {
        nameField.setText("Test User");
        emailField.setText(""); // empty
        passwordField.setText("password123");
        usernameField.setText("validUser");
        phoneField.setText("");

        signUpButton.performClick();

        assertEquals("Email is required.", emailField.getError().toString());
    }

    @Test
    public void emptyPassword_showsPasswordError() {
        nameField.setText("Test User");
        emailField.setText("test@example.com");
        passwordField.setText(""); // empty
        usernameField.setText("validUser");
        phoneField.setText("");

        signUpButton.performClick();

        assertEquals("Password is required.", passwordField.getError().toString());
    }

    @Test
    public void shortUsername_showsUsernameLengthError() {
        nameField.setText("Test User");
        emailField.setText("test@example.com");
        passwordField.setText("password123");
        usernameField.setText("abc"); // < 5 chars
        phoneField.setText("");

        signUpButton.performClick();

        assertEquals("Username must be at least 5 characters long",
                usernameField.getError().toString());
    }

    @Test
    public void invalidPhone_showsPhoneError() {
        nameField.setText("Test User");
        emailField.setText("test@example.com");
        passwordField.setText("password123");
        usernameField.setText("validUser");
        phoneField.setText("12345"); // not 10 digits

        signUpButton.performClick();

        assertEquals("Phone number must be exactly 10 digits or left blank",
                phoneField.getError().toString());
    }

    @Test
    public void invalidEmailFormat_showsEmailFormatError() {
        nameField.setText("Test User");
        emailField.setText("not-an-email"); // invalid format
        passwordField.setText("password123");
        usernameField.setText("validUser");
        phoneField.setText("1234567890"); // valid phone

        signUpButton.performClick();

        assertEquals("Please enter a valid email address",
                emailField.getError().toString());
    }

    @Test
    public void validInputs_noImmediateFieldErrors() {
        nameField.setText("Test User");
        emailField.setText("test@example.com");
        passwordField.setText("password123");
        usernameField.setText("validUser");
        phoneField.setText("1234567890");

        signUpButton.performClick();

        // We don't assert Firebase success here, just that validation passed
        assertNull(nameField.getError());
        assertNull(emailField.getError());
        assertNull(passwordField.getError());
        assertNull(usernameField.getError());
        assertNull(phoneField.getError());
    }
}
