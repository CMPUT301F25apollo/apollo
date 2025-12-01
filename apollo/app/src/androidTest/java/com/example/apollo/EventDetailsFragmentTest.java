package com.example.apollo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.widget.Button;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.entrant.home.EventDetailsFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailsFragmentTest {

    /**
     * US 01.01.01
     * US 01.01.02
     * Helper method to manually set the fragment’s internal state
     * and then force the button to re-render.
     * simulate different user situations
     *
     */
    private void setStateAndRender(EventDetailsFragment fragment, String stateName) throws Exception {
        Class<?> clazz = fragment.getClass();

        // make sure registration isn’t blocked
        Field notStartedField = clazz.getDeclaredField("registrationNotStartedYet");
        notStartedField.setAccessible(true);
        notStartedField.setBoolean(fragment, false);

        Field endedField = clazz.getDeclaredField("registrationEnded");
        endedField.setAccessible(true);
        endedField.setBoolean(fragment, false);

        // update the private field "state"
        Field stateField = clazz.getDeclaredField("state");
        stateField.setAccessible(true);
        Class<?> stateEnumType = stateField.getType();
        Object newState = Enum.valueOf((Class<Enum>) stateEnumType, stateName);
        stateField.set(fragment, newState);

        // refresh  UI
        Method renderButton = clazz.getDeclaredMethod("renderButton");
        renderButton.setAccessible(true);
        renderButton.invoke(fragment);
    }

    /**
     * If the user is NOT on the waitlist, they should see:
     * "JOIN WAITLIST"
     *
     * US 01.01.01
     */
    @Test
    public void whenNotOnWaitlist_buttonShowsJoinWaitlist() {
        FragmentScenario<EventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(EventDetailsFragment.class, (Bundle) null);

        scenario.onFragment(fragment -> {
            try {
                setStateAndRender(fragment, "NONE");

                Button joinButton = fragment.getView().findViewById(R.id.buttonJoinWaitlist);
                assertEquals("JOIN WAITLIST", joinButton.getText().toString());
                assertTrue(joinButton.isEnabled());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * If the user IS already on the waitlist, they should see:
     * "LEAVE WAITLIST"
     *
     * US 01.01.02
     */
    @Test
    public void whenOnWaitlist_buttonShowsLeaveWaitlist() {
        FragmentScenario<EventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(EventDetailsFragment.class, (Bundle) null);

        scenario.onFragment(fragment -> {
            try {
                setStateAndRender(fragment, "WAITING");

                Button joinButton = fragment.getView().findViewById(R.id.buttonJoinWaitlist);
                assertEquals("LEAVE WAITLIST", joinButton.getText().toString());
                assertTrue(joinButton.isEnabled());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * If registration hasn’t opened yet,
     * the button should be disabled and show the open-date message.
     */
    @Test
    public void whenRegistrationNotStarted_buttonShowsRegistrationOpens() {
        FragmentScenario<EventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(EventDetailsFragment.class, (Bundle) null);

        scenario.onFragment(fragment -> {
            try {
                Class<?> clazz = fragment.getClass();

                // registration hasn’t opened yet
                Field notStartedField = clazz.getDeclaredField("registrationNotStartedYet");
                notStartedField.setAccessible(true);
                notStartedField.setBoolean(fragment, true);

                // registration hasn’t ended
                Field endedField = clazz.getDeclaredField("registrationEnded");
                endedField.setAccessible(true);
                endedField.setBoolean(fragment, false);

                // fake open date
                Field registrationOpenTextField = clazz.getDeclaredField("registrationOpenText");
                registrationOpenTextField.setAccessible(true);
                registrationOpenTextField.set(fragment, "12/25/2025");


                Field stateField = clazz.getDeclaredField("state");
                stateField.setAccessible(true);
                Class<?> stateEnumType = stateField.getType();
                Object newState = Enum.valueOf((Class<Enum>) stateEnumType, "NONE");
                stateField.set(fragment, newState);

                // update UI
                Method renderButton = clazz.getDeclaredMethod("renderButton");
                renderButton.setAccessible(true);
                renderButton.invoke(fragment);

                Button joinButton = fragment.getView().findViewById(R.id.buttonJoinWaitlist);
                String text = joinButton.getText().toString();

                assertTrue(text.startsWith("REGISTRATION OPENS ON"));
                assertTrue(!joinButton.isEnabled());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * If registration has already ended,
     * the button should be disabled and show "REGISTRATION ENDED".
     */
    @Test
    public void whenRegistrationEnded_buttonShowsRegistrationEnded() {
        FragmentScenario<EventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(EventDetailsFragment.class, (Bundle) null);

        scenario.onFragment(fragment -> {
            try {
                Class<?> clazz = fragment.getClass();

                // registration ended = true
                Field notStartedField = clazz.getDeclaredField("registrationNotStartedYet");
                notStartedField.setAccessible(true);
                notStartedField.setBoolean(fragment, false);

                Field endedField = clazz.getDeclaredField("registrationEnded");
                endedField.setAccessible(true);
                endedField.setBoolean(fragment, true);


                Field stateField = clazz.getDeclaredField("state");
                stateField.setAccessible(true);
                Class<?> stateEnumType = stateField.getType();
                Object newState = Enum.valueOf((Class<Enum>) stateEnumType, "NONE");
                stateField.set(fragment, newState);

                // update UI
                Method renderButton = clazz.getDeclaredMethod("renderButton");
                renderButton.setAccessible(true);
                renderButton.invoke(fragment);

                Button joinButton = fragment.getView().findViewById(R.id.buttonJoinWaitlist);
                assertEquals("REGISTRATION ENDED", joinButton.getText().toString());
                assertTrue(!joinButton.isEnabled());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
