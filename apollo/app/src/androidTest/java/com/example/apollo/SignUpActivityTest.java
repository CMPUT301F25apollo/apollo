
package com.example.apollo;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import com.example.apollo.ui.login.SignUpActivity;

@RunWith(AndroidJUnit4.class)
public class SignUpActivityTest {

    @Rule
    public ActivityScenarioRule<SignUpActivity> activityRule =
            new ActivityScenarioRule<>(SignUpActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @Test
    public void test_UIElementsDisplayed() {
        onView(withId(R.id.etName)).check(matches(isDisplayed()));
        onView(withId(R.id.etUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.etPhoneNumber)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.etConfirmPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.btnSignUp)).check(matches(isDisplayed()));
        onView(withId(R.id.tvLogin)).check(matches(isDisplayed()));
    }

    @Test
    public void test_signUpWithEmptyFields() {
        onView(withId(R.id.btnSignUp)).perform(click());
        onView(withText("All fields are required")).check(matches(isDisplayed()));
    }

    @Test
    public void test_signUpWithPasswordMismatch() {
        onView(withId(R.id.etName)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.etUsername)).perform(typeText("testuser"), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPhoneNumber)).perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.etConfirmPassword)).perform(typeText("password456"), closeSoftKeyboard());
        onView(withId(R.id.btnSignUp)).perform(click());
        onView(withText("Passwords do not match")).check(matches(isDisplayed()));
    }

    @Test
    public void test_successfulSignUp() {
        String email = "test" + System.currentTimeMillis() + "@example.com";
        onView(withId(R.id.etName)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.etUsername)).perform(typeText("testuser"), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(typeText(email), closeSoftKeyboard());
        onView(withId(R.id.etPhoneNumber)).perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.etConfirmPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnSignUp)).perform(click());
        intended(hasComponent(MainActivity.class.getName()));
    }

    @After
    public void tearDown() {
        Intents.release();
    }
}
