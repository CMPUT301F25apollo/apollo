package com.example.apollo.entrant.home;

import static org.junit.Assert.assertEquals;

import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.example.apollo.ui.entrant.home.HomeFragment;
import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Config(sdk = 34)
@RunWith(RobolectricTestRunner.class)
public class HomeFragmentTest {

    private FragmentActivity activity;
    private HomeFragment fragment;

    @Before
    public void setUp() {
        // Make sure FirebaseApp exists so FirebaseFirestore.getInstance() doesn't crash.
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }

        activity = Robolectric.buildActivity(FragmentActivity.class)
                .setup()
                .get();

        fragment = new HomeFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment, "home")
                .commitNow();
    }

    // ---------- Reflection helpers ----------

    @SuppressWarnings("unchecked")
    private List<Object> getAllEventsList() throws Exception {
        Field f = HomeFragment.class.getDeclaredField("allEvents");
        f.setAccessible(true);
        return (List<Object>) f.get(fragment);
    }

    private void setBooleanField(String name, boolean value) throws Exception {
        Field f = HomeFragment.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setBoolean(fragment, value);
    }

    private void setStringField(String name, String value) throws Exception {
        Field f = HomeFragment.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(fragment, value);
    }

    private void callFilterEvents() throws Exception {
        Method m = HomeFragment.class.getDeclaredMethod("filterEvents");
        m.setAccessible(true);
        m.invoke(fragment);
    }

    private Object newEvent(
            String title,
            String location,
            boolean isOpen,
            boolean isClosed,
            View view,
            Date regOpen,
            Date regClose
    ) throws Exception {
        Class<?> eventClass = Class.forName(
                "com.example.apollo.ui.entrant.home.HomeFragment$Event"
        );
        Constructor<?> ctor = eventClass.getDeclaredConstructor(
                String.class,
                String.class,
                boolean.class,
                boolean.class,
                View.class,
                Date.class,
                Date.class
        );
        ctor.setAccessible(true);
        return ctor.newInstance(title, location, isOpen, isClosed, view, regOpen, regClose);
    }

    // ---------- Tests ----------

    @Test
    public void filterEvents_respectsOpenClosedFlags() throws Exception {
        List<Object> allEvents = getAllEventsList();
        allEvents.clear();

        View openView = new View(activity);
        View closedView = new View(activity);

        allEvents.add(newEvent("Open Event", "Hall", true, false, openView, null, null));
        allEvents.add(newEvent("Closed Event", "Hall", false, true, closedView, null, null));

        // show only open
        setBooleanField("showOpen", true);
        setBooleanField("showClosed", false);
        setStringField("titleKeyword", "");
        setStringField("locationKeyword", "");
        setStringField("dateFilter", "");

        callFilterEvents();

        assertEquals(View.VISIBLE, openView.getVisibility());
        assertEquals(View.GONE, closedView.getVisibility());
    }

    @Test
    public void filterEvents_filtersByTitleKeyword_caseInsensitive() throws Exception {
        List<Object> allEvents = getAllEventsList();
        allEvents.clear();

        View paintView = new View(activity);
        View techView = new View(activity);

        allEvents.add(newEvent("Sip and Paint Night", "Studio", true, false, paintView, null, null));
        allEvents.add(newEvent("Tech Talk", "Hall", true, false, techView, null, null));

        setBooleanField("showOpen", true);
        setBooleanField("showClosed", true);
        // HomeFragment stores these internally as lowercase, so give lowercase.
        setStringField("titleKeyword", "paint");
        setStringField("locationKeyword", "");
        setStringField("dateFilter", "");

        callFilterEvents();

        assertEquals(View.VISIBLE, paintView.getVisibility());
        assertEquals(View.GONE, techView.getVisibility());
    }

    @Test
    public void filterEvents_filtersByDateAvailability() throws Exception {
        List<Object> allEvents = getAllEventsList();
        allEvents.clear();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        Date open1 = sdf.parse("12/10/2025");
        Date close1 = sdf.parse("12/20/2025");
        Date open2 = sdf.parse("12/01/2025");
        Date close2 = sdf.parse("12/10/2025");

        View withinRangeView = new View(activity);
        View outsideRangeView = new View(activity);

        // Both are "open" logically, but date filter will distinguish them
        allEvents.add(newEvent("Inside Window", "Room A", true, false,
                withinRangeView, open1, close1));
        allEvents.add(newEvent("Outside Window", "Room B", true, false,
                outsideRangeView, open2, close2));

        setBooleanField("showOpen", true);
        setBooleanField("showClosed", true);
        setStringField("titleKeyword", "");
        setStringField("locationKeyword", "");
        // Filter date is 12/15, inside [10,20] but after close2 (10)
        setStringField("dateFilter", "12/15/2025");

        callFilterEvents();

        assertEquals(View.VISIBLE, withinRangeView.getVisibility());
        assertEquals(View.GONE, outsideRangeView.getVisibility());
    }
}
