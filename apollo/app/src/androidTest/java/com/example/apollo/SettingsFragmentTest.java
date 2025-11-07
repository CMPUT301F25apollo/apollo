package com.example.apollo;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.view.View;
import androidx.test.espresso.Root;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.apollo.MainActivity;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingsFragmentTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void navigateToSettings() {
        // This test assumes you are already logged in.
        // It navigates from the profile screen to the settings screen.
        onView(withId(R.id.navigation_profile)).perform(click());
        onView(withId(R.id.btnSettings)).perform(click());
    }

    @Test
    public void testSaveUserProfile_EmptyFields_ShowsToast() {
        onView(withId(R.id.editName)).perform(clearText());
        onView(withId(R.id.Savebtn)).perform(click());

        // Check for the toast message
        onView(withText("Please fill all required fields"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDeleteAccount_PromptsDialog() {
        onView(withId(R.id.Deleteaccountbtn)).perform(click());

        // Verify the dialog is displayed by checking for its title
        onView(withText("Re-authenticate to Delete Account")).check(matches(isDisplayed()));
    }
}

// Custom matcher to find toasts
class ToastMatcher extends TypeSafeMatcher<Root> {
    @Override
    public void describeTo(Description description) {
        description.appendText("is toast");
    }

    @Override
    public boolean matchesSafely(Root root) {
        int type = root.getWindowLayoutParams().get().type;
        if (type == android.view.WindowManager.LayoutParams.TYPE_TOAST) {
            View windowDecorView = root.getDecorView();
            return windowDecorView.getWindowToken() == windowDecorView.getApplicationWindowToken();
        }
        return false;
    }
}
