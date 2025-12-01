package com.example.apollo;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.entrant.home.EventDetailsFragment;
import com.example.apollo.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

// Espresso static imports (correct!)
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistButtonUiTest {

    private void forceState(EventDetailsFragment fragment, String stateName) throws Exception {
        Class<?> clazz = fragment.getClass();

        // ensure registration is open
        Field notStarted = clazz.getDeclaredField("registrationNotStartedYet");
        notStarted.setAccessible(true);
        notStarted.setBoolean(fragment, false);

        Field ended = clazz.getDeclaredField("registrationEnded");
        ended.setAccessible(true);
        ended.setBoolean(fragment, false);

        // update enum state
        Field stateField = clazz.getDeclaredField("state");
        stateField.setAccessible(true);
        Class<?> enumType = stateField.getType();
        Object newState = Enum.valueOf((Class<Enum>) enumType, stateName);
        stateField.set(fragment, newState);

        // call private renderButton()
        Method renderBtn = clazz.getDeclaredMethod("renderButton");
        renderBtn.setAccessible(true);
        renderBtn.invoke(fragment);
    }

    @Test
    public void ui_showsJoinWhenNotOnWaitlist() {
        FragmentScenario<EventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(
                        EventDetailsFragment.class,
                        (Bundle) null,
                        R.style.Theme_Apollo
                );

        scenario.onFragment(fragment -> {
            try {
                forceState(fragment, "NONE");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        onView(withId(R.id.buttonJoinWaitlist))
                .check(matches(withText("JOIN WAITLIST")))
                .check(matches(isEnabled()));
    }

    @Test
    public void ui_showsLeaveWhenOnWaitlist() {
        FragmentScenario<EventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(
                        EventDetailsFragment.class,
                        (Bundle) null,
                        R.style.Theme_Apollo
                );

        scenario.onFragment(fragment -> {
            try {
                forceState(fragment, "WAITING");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        onView(withId(R.id.buttonJoinWaitlist))
                .check(matches(withText("LEAVE WAITLIST")))
                .check(matches(isEnabled()));
    }
}
