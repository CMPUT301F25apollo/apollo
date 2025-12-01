package com.example.apollo.entrant.home;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ApplicationProvider;

import com.example.apollo.R;
import com.example.apollo.ui.entrant.home.FilterFragment;
import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

@Config(sdk = 34)
@RunWith(RobolectricTestRunner.class)
public class FilterFragmentTest {

    private FragmentActivity activity;
    private FilterFragment fragment;

    @Before
    public void setUp() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }

        activity = Robolectric.buildActivity(FragmentActivity.class)
                .setup()
                .get();

        fragment = new FilterFragment();

        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment, "filter")
                .commitNow();
    }

    // ---------- UI helpers ----------

    private CheckBox getOpenCheckbox() {
        return fragment.requireView().findViewById(R.id.cbOpen);
    }

    private CheckBox getClosedCheckbox() {
        return fragment.requireView().findViewById(R.id.cbClosed);
    }

    private EditText getTitleInput() {
        return fragment.requireView().findViewById(R.id.etTitleKeyword);
    }

    private EditText getLocationInput() {
        return fragment.requireView().findViewById(R.id.etLocationKeyword);
    }

    private EditText getDateInput() {
        return fragment.requireView().findViewById(R.id.etDate);
    }

    private Button getApplyButton() {
        return fragment.requireView().findViewById(R.id.btnApply);
    }

    // ---------- Reflection helpers ----------

    private boolean getPrivateBoolean(String fieldName) throws Exception {
        Field f = FilterFragment.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.getBoolean(fragment);
    }

    // ------------------------------------------------------------------
    //                               TESTS
    // ------------------------------------------------------------------

    @Test
    public void restoresArguments_correctlySetsUI() {
        Bundle args = new Bundle();
        args.putBoolean("open", false);
        args.putBoolean("closed", true);
        args.putString("titleKeyword", "party");
        args.putString("locationKeyword", "toronto");
        args.putString("date", "2025-01-01");

        FilterFragment frag2 = new FilterFragment();
        frag2.setArguments(args);

        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, frag2)
                .commitNow();

        assertFalse(((CheckBox) frag2.requireView().findViewById(R.id.cbOpen)).isChecked());
        assertTrue(((CheckBox) frag2.requireView().findViewById(R.id.cbClosed)).isChecked());
        assertEquals("party", ((EditText) frag2.requireView().findViewById(R.id.etTitleKeyword)).getText().toString());
        assertEquals("toronto", ((EditText) frag2.requireView().findViewById(R.id.etLocationKeyword)).getText().toString());
        assertEquals("2025-01-01", ((EditText) frag2.requireView().findViewById(R.id.etDate)).getText().toString());
    }

    @Test
    public void applyButton_click_updatesPrivateBooleans() throws Exception {

        NavController mockNav = mock(NavController.class);

        try (MockedStatic<NavHostFragment> mocked = mockStatic(NavHostFragment.class)) {

            mocked.when(() -> NavHostFragment.findNavController(fragment))
                    .thenReturn(mockNav);

            getOpenCheckbox().setChecked(true);
            getClosedCheckbox().setChecked(false);

            getApplyButton().performClick();

            assertTrue(getPrivateBoolean("prevOpen"));
            assertFalse(getPrivateBoolean("prevClosed"));
        }
    }

    @Test
    public void applyButton_click_sendsFragmentResult() throws Exception {
        final Bundle[] captured = new Bundle[1];

        activity.getSupportFragmentManager()
                .setFragmentResultListener("filters", fragment, (key, result) -> {
                    captured[0] = result;
                });

        NavController mockNav = mock(NavController.class);

        try (MockedStatic<NavHostFragment> mocked = mockStatic(NavHostFragment.class)) {

            mocked.when(() -> NavHostFragment.findNavController(fragment))
                    .thenReturn(mockNav);

            getOpenCheckbox().setChecked(true);
            getClosedCheckbox().setChecked(true);
            getTitleInput().setText("music");
            getLocationInput().setText("edmonton");
            getDateInput().setText("2030-01-01");

            getApplyButton().performClick();
        }

        assertNotNull(captured[0]);
        assertTrue(captured[0].getBoolean("open"));
        assertTrue(captured[0].getBoolean("closed"));
        assertEquals("music", captured[0].getString("titleKeyword"));
        assertEquals("edmonton", captured[0].getString("locationKeyword"));
        assertEquals("2030-01-01", captured[0].getString("date"));
    }

    @Test
    public void applyButton_click_callsPopBackStack() {

        NavController mockNav = mock(NavController.class);

        try (MockedStatic<NavHostFragment> mocked = mockStatic(NavHostFragment.class)) {

            mocked.when(() -> NavHostFragment.findNavController(fragment))
                    .thenReturn(mockNav);

            getApplyButton().performClick();

            verify(mockNav, times(1)).popBackStack();
        }
    }
}
