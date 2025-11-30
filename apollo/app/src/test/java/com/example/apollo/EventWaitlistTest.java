package com.example.apollo.ui.home;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.apollo.MainActivity;
import com.example.apollo.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Black-box UI tests for waitlist functionality.
 * Covers:
 *  - US 01.01.01: Entrant can join waitlist
 *  - US 01.01.02: Entrant can leave waitlist
 */
@RunWith(AndroidJUnit4.class)
public class EventWaitlistUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /** US 01.01.01 — Verify user can join waitlist from UI */
    @Test
    public void userCanJoinWaitlist() {
        // Simulate user tapping "Join Waitlist" button
        onView(withId(R.id.buttonJoinWaitlist)).perform(click());

        // Verify that confirmation message appears
        onView(withText("Joined waitlist")).check(matches(isDisplayed()));
    }

    /** US 01.01.02 — Verify user can leave waitlist */
    @Test
    public void userCanLeaveWaitlist() {
        // Tap again to leave (if already joined)
        onView(withId(R.id.buttonJoinWaitlist)).perform(click());

        // Expect the “Left waitlist” toast or button label to update
        onView(withText("Left waitlist")).check(matches(isDisplayed()));
    }
}