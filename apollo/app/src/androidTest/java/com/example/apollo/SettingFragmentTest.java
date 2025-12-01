package com.example.apollo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.entrant.profile.SettingsFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SettingFragmentTest {

    //  US 01.02.01: entering profile info
    //  US 01.02.02: updating profile info

    @Test
    public void saveUserProfile_setsErrorOnShortUsername() {
        FragmentScenario<SettingsFragment> scenario =
                FragmentScenario.launchInContainer(SettingsFragment.class);

        scenario.onFragment(fragment -> {
            EditText editName = fragment.getView().findViewById(R.id.editName);
            EditText editUsername = fragment.getView().findViewById(R.id.editUsername);
            EditText editEmail = fragment.getView().findViewById(R.id.editEmail);
            EditText editPhone = fragment.getView().findViewById(R.id.editPhone);
            Button saveBtn = fragment.getView().findViewById(R.id.Savebtn);

            //username too short
            editName.setText("Test User");
            editUsername.setText("abc");
            editEmail.setText("test@example.com");
            editPhone.setText("");

            saveBtn.performClick();

            // should complain only about username length
            assertNotNull(editUsername.getError());
            assertEquals(
                    "Username must be at least 5 characters long",
                    editUsername.getError().toString()
            );
        });
    }

    @Test
    public void saveUserProfile_setsErrorOnInvalidPhoneNumber() {
        FragmentScenario<SettingsFragment> scenario =
                FragmentScenario.launchInContainer(SettingsFragment.class);

        scenario.onFragment(fragment -> {
            EditText editName = fragment.getView().findViewById(R.id.editName);
            EditText editUsername = fragment.getView().findViewById(R.id.editUsername);
            EditText editEmail = fragment.getView().findViewById(R.id.editEmail);
            EditText editPhone = fragment.getView().findViewById(R.id.editPhone);
            Button saveBtn = fragment.getView().findViewById(R.id.Savebtn);

            // bad phone number
            editName.setText("Test User");
            editUsername.setText("validUser");
            editEmail.setText("test@example.com");
            editPhone.setText("12345");

            saveBtn.performClick();

            // phone field should show the error
            assertNotNull(editPhone.getError());
            assertEquals(
                    "Phone number must be exactly 10 digits",
                    editPhone.getError().toString()
            );
        });
    }

    @Test
    public void saveUserProfile_setsErrorOnInvalidEmail() {
        FragmentScenario<SettingsFragment> scenario =
                FragmentScenario.launchInContainer(SettingsFragment.class);

        scenario.onFragment(fragment -> {
            EditText editName = fragment.getView().findViewById(R.id.editName);
            EditText editUsername = fragment.getView().findViewById(R.id.editUsername);
            EditText editEmail = fragment.getView().findViewById(R.id.editEmail);
            EditText editPhone = fragment.getView().findViewById(R.id.editPhone);
            Button saveBtn = fragment.getView().findViewById(R.id.Savebtn);

            // bad email
            editName.setText("Test User");
            editUsername.setText("validUser");
            editEmail.setText("not-an-email");
            editPhone.setText("");

            saveBtn.performClick();

            // email field should show the error
            assertNotNull(editEmail.getError());
            assertEquals(
                    "Please enter a valid email address",
                    editEmail.getError().toString()
            );
        });
    }

    @Test
    public void validProfileData_hasNoErrors() {
        FragmentScenario<SettingsFragment> scenario =
                FragmentScenario.launchInContainer(SettingsFragment.class);

        scenario.onFragment(fragment -> {
            EditText editName = fragment.getView().findViewById(R.id.editName);
            EditText editUsername = fragment.getView().findViewById(R.id.editUsername);
            EditText editEmail = fragment.getView().findViewById(R.id.editEmail);
            EditText editPhone = fragment.getView().findViewById(R.id.editPhone);
            Button saveBtn = fragment.getView().findViewById(R.id.Savebtn);

            // everything correct & no number
            editName.setText("Test User");
            editUsername.setText("validUser");
            editEmail.setText("test@example.com");
            editPhone.setText("");

            assertTrue(editName.getError() == null);
            assertTrue(editUsername.getError() == null);
            assertTrue(editEmail.getError() == null);
            assertTrue(editPhone.getError() == null);
        });
    }

    @Test
    public void updatingProfile_withValidData_hasNoErrors() {
        FragmentScenario<SettingsFragment> scenario =
                FragmentScenario.launchInContainer(SettingsFragment.class);

        scenario.onFragment(fragment -> {
            EditText editName = fragment.getView().findViewById(R.id.editName);
            EditText editUsername = fragment.getView().findViewById(R.id.editUsername);
            EditText editEmail = fragment.getView().findViewById(R.id.editEmail);
            EditText editPhone = fragment.getView().findViewById(R.id.editPhone);
            Button saveBtn = fragment.getView().findViewById(R.id.Savebtn);

            // old values
            editName.setText("Old Name");
            editUsername.setText("oldUser");
            editEmail.setText("old@example.com");
            editPhone.setText("1234567890");

            // user edits them to new valid values
            editName.setText("New Name");
            editUsername.setText("newUser");
            editEmail.setText("new@example.com");
            editPhone.setText("0987654321");

            assertTrue(editName.getError() == null);
            assertTrue(editUsername.getError() == null);
            assertTrue(editEmail.getError() == null);
            assertTrue(editPhone.getError() == null);
        });
    }


    // US 01.02.04: delete profile

    @Test
    public void deleteAccountButton_isVisible() {
        FragmentScenario<SettingsFragment> scenario =
                FragmentScenario.launchInContainer(SettingsFragment.class);

        scenario.onFragment(fragment -> {
            Button deleteBtn = fragment.getView().findViewById(R.id.Deleteaccountbtn);

            assertNotNull("Delete Account button should exist", deleteBtn);
            assertTrue("Delete Account button should be clickable", deleteBtn.isClickable());
        });
    }

    @Test
    public void deleteAccount_withNoCurrentUser_doesNotCrash() {
        FragmentScenario<SettingsFragment> scenario =
                FragmentScenario.launchInContainer(SettingsFragment.class);

        scenario.onFragment(fragment -> {
            Button deleteBtn = fragment.getView().findViewById(R.id.Deleteaccountbtn);

            // Clicking delete in this test setup should not throw error
            deleteBtn.performClick();
            assertTrue(fragment.isAdded());
        });
    }
}
