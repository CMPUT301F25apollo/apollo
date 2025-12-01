package com.example.apollo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.view.View;
import android.widget.Switch;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.apollo.ui.organizer.events.AddEventFragment;
import com.google.android.material.textfield.TextInputEditText;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * US 02.01.04
 * US 02.02.03
 * US 02.03.01
 */
@RunWith(AndroidJUnit4.class)
public class AddEventFragmentTest {

    private FragmentScenario<AddEventFragment> launchAddEventFragment() {
        return FragmentScenario.launchInContainer(
                AddEventFragment.class,
                null,
                R.style.Theme_Apollo
        );
    }


    @Test
    public void registrationPeriodFields_areVisibleAndEditable() {
        FragmentScenario<AddEventFragment> scenario = launchAddEventFragment();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            TextInputEditText regOpen =
                    root.findViewById(R.id.registrationOpen);
            TextInputEditText regClose =
                    root.findViewById(R.id.registrationClose);

            assertNotNull(regOpen);
            assertNotNull(regClose);

            regOpen.setText("12/10/2025");
            regClose.setText("12/15/2025");

            assertEquals("12/10/2025",
                    regOpen.getText().toString());
            assertEquals("12/15/2025",
                    regClose.getText().toString());
        });
    }

    @Test
    public void geolocationSwitch_canBeToggled() {
        FragmentScenario<AddEventFragment> scenario = launchAddEventFragment();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            Switch geoSwitch = root.findViewById(R.id.switchButton);
            assertNotNull(geoSwitch);

            boolean initial = geoSwitch.isChecked();
            geoSwitch.setChecked(!initial);

            assertEquals(!initial, geoSwitch.isChecked());
        });
    }


    @Test
    public void waitlistCapacityField_isVisibleAndEditable() {
        FragmentScenario<AddEventFragment> scenario = launchAddEventFragment();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            TextInputEditText waitlistCap =
                    root.findViewById(R.id.waitlistCapacity);
            assertNotNull(waitlistCap);

            waitlistCap.setText("25");
            assertEquals("25", waitlistCap.getText().toString());
        });
    }
}
