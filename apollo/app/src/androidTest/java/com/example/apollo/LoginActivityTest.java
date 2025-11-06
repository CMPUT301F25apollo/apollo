
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
import static org.hamcrest.Matchers.not;

import com.example.apollo.ui.login.LoginActivity;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @Test
    public void test_UIElementsDisplayed() {
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
        onView(withId(R.id.tvSignUp)).check(matches(isDisplayed()));
        onView(withId(R.id.tvGuest)).check(matches(isDisplayed()));
    }

    @Test
    public void test_loginWithEmptyEmailAndPassword() {
        onView(withId(R.id.btnLogin)).perform(click());
        onView(withText("Email and password cannot be empty")).check(matches(isDisplayed()));
    }

    @Test
    public void test_loginWithInvalidCredentials() {
        onView(withId(R.id.etEmail)).perform(typeText("invalid@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("wrongpassword"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
        onView(withText("Login failed")).check(matches(isDisplayed()));
    }

    @Test
    public void test_successfulLogin() {
        // Replace with a valid email and password from your Firebase Authentication
        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
        intended(hasComponent(MainActivity.class.getName()));
    }

    @After
    public void tearDown() {
        Intents.release();
    }
}
