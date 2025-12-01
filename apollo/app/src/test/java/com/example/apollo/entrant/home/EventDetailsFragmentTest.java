package com.example.apollo.entrant.home;

import static org.junit.Assert.assertEquals;

import android.widget.Button;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.example.apollo.ui.entrant.home.EventDetailsFragment;
import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Config(sdk = 34)
@RunWith(RobolectricTestRunner.class)
public class EventDetailsFragmentTest {

    private FragmentActivity activity;
    private EventDetailsFragment fragment;

    @Before
    public void setUp() {
        // Ensure a FirebaseApp exists (FirebaseAuth/FirebaseFirestore need this)
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }

        activity = Robolectric.buildActivity(FragmentActivity.class)
                .setup()
                .get();

        fragment = new EventDetailsFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment, "details")
                .commitNow();
    }

    // ---------- reflection helpers ----------

    private Button getJoinButton() throws Exception {
        Field f = EventDetailsFragment.class.getDeclaredField("buttonJoinWaitlist");
        f.setAccessible(true);
        return (Button) f.get(fragment);
    }

    private void setBooleanField(String name, boolean value) throws Exception {
        Field f = EventDetailsFragment.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setBoolean(fragment, value);
    }

    private void setStateEnum(String stateName) throws Exception {
        Field field = EventDetailsFragment.class.getDeclaredField("state");
        field.setAccessible(true);

        Class<?> enumClass = Class.forName(
                "com.example.apollo.ui.entrant.home.EventDetailsFragment$State"
        );
        @SuppressWarnings("unchecked")
        Enum<?> enumValue = Enum.valueOf((Class<Enum>) enumClass, stateName);
        field.set(fragment, enumValue);
    }

    private void setRegistrationOpenText(String value) throws Exception {
        Field f = EventDetailsFragment.class.getDeclaredField("registrationOpenText");
        f.setAccessible(true);
        f.set(fragment, value);
    }

    private void callRecalcState(Boolean registered, Boolean invited, Boolean waiting) throws Exception {
        Method m = EventDetailsFragment.class.getDeclaredMethod(
                "recalcState", Boolean.class, Boolean.class, Boolean.class
        );
        m.setAccessible(true);
        m.invoke(fragment, registered, invited, waiting);
    }

    private void callRenderButton() throws Exception {
        Method m = EventDetailsFragment.class.getDeclaredMethod("renderButton");
        m.setAccessible(true);
        m.invoke(fragment);
    }

    // ---------- tests ----------

    @Test
    public void recalcState_registered_setsRegisteredUI() throws Exception {
        Button btn = getJoinButton();

        // all registration-period flags false to allow normal state behaviour
        setBooleanField("registrationNotStartedYet", false);
        setBooleanField("registrationEnded", false);
        setBooleanField("registrationOpenNow", true);

        callRecalcState(true, null, null);

        assertEquals("REGISTERED", btn.getText().toString());
        assertEquals(false, btn.isEnabled());
    }

    @Test
    public void recalcState_invited_setsSignUpUI() throws Exception {
        Button btn = getJoinButton();

        setBooleanField("registrationNotStartedYet", false);
        setBooleanField("registrationEnded", false);
        setBooleanField("registrationOpenNow", true);

        callRecalcState(false, true, false);

        assertEquals("SIGN UP", btn.getText().toString());
        assertEquals(true, btn.isEnabled());
    }

    @Test
    public void recalcState_waiting_setsLeaveWaitlistUI() throws Exception {
        Button btn = getJoinButton();

        setBooleanField("registrationNotStartedYet", false);
        setBooleanField("registrationEnded", false);
        setBooleanField("registrationOpenNow", true);

        callRecalcState(false, false, true);

        assertEquals("LEAVE WAITLIST", btn.getText().toString());
        assertEquals(true, btn.isEnabled());
    }

    @Test
    public void renderButton_none_beforeRegistration_setsNotOpenTextAndDisabled() throws Exception {
        Button btn = getJoinButton();

        setStateEnum("NONE");
        setBooleanField("registrationNotStartedYet", true);
        setBooleanField("registrationEnded", false);
        setRegistrationOpenText("01/15/2026");

        callRenderButton();

        assertEquals("REGISTRATION OPENS ON 01/15/2026", btn.getText().toString());
        assertEquals(false, btn.isEnabled());
    }

    @Test
    public void renderButton_none_afterRegistration_setsEndedTextAndDisabled() throws Exception {
        Button btn = getJoinButton();

        setStateEnum("NONE");
        setBooleanField("registrationNotStartedYet", false);
        setBooleanField("registrationEnded", true);

        callRenderButton();

        assertEquals("REGISTRATION ENDED", btn.getText().toString());
        assertEquals(false, btn.isEnabled());
    }

    @Test
    public void renderButton_none_normal_setsJoinWaitlistEnabled() throws Exception {
        Button btn = getJoinButton();

        setStateEnum("NONE");
        setBooleanField("registrationNotStartedYet", false);
        setBooleanField("registrationEnded", false);

        callRenderButton();

        assertEquals("JOIN WAITLIST", btn.getText().toString());
        assertEquals(true, btn.isEnabled());
    }
}
