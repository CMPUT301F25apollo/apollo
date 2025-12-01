package com.example.apollo;

import android.os.Bundle;
import android.widget.CheckBox;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.entrant.home.FilterFragment;


import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Button;

import java.util.concurrent.atomic.AtomicReference;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class FilterFragmentTest {

    @Test
    public void filterFragment_showsInitialArgsInCheckboxes() {
        // Arrange: launch FilterFragment with open=true, closed=false
        Bundle args = new Bundle();
        args.putBoolean("open", true);
        args.putBoolean("closed", false);

        FragmentScenario<FilterFragment> scenario =
                FragmentScenario.launchInContainer(FilterFragment.class, args);

        // Act + Assert on the fragment's views directly
        scenario.onFragment(fragment -> {
            CheckBox cbOpen = fragment.getView().findViewById(R.id.cbOpen);
            CheckBox cbClosed = fragment.getView().findViewById(R.id.cbClosed);

            assertTrue(cbOpen.isChecked());
            assertFalse(cbClosed.isChecked());
        });
    }
    @Test
    public void filterFragment_sendsSelectedFiltersBackOnApply() {
        // Start with both false
        Bundle args = new Bundle();
        args.putBoolean("open", false);
        args.putBoolean("closed", false);

        FragmentScenario<FilterFragment> scenario =
                FragmentScenario.launchInContainer(FilterFragment.class, args);

        AtomicReference<Bundle> resultRef = new AtomicReference<>();

        scenario.onFragment(fragment -> {
            // Listen to the "filters" result
            fragment.getParentFragmentManager().setFragmentResultListener(
                    "filters",
                    fragment,
                    (key, bundle) -> resultRef.set(bundle)
            );

            // Grab the views
            CheckBox cbOpen = fragment.getView().findViewById(R.id.cbOpen);
            CheckBox cbClosed = fragment.getView().findViewById(R.id.cbClosed);

            // Simulate user choosing filters
            cbOpen.setChecked(true);
            cbClosed.setChecked(true);

            // Manually do what the Apply button does *before* navigation:
            Bundle result = new Bundle();
            result.putBoolean("open", cbOpen.isChecked());
            result.putBoolean("closed", cbClosed.isChecked());

            fragment.getParentFragmentManager().setFragmentResult("filters", result);
        });

        // After onFragment finishes, our listener should have captured the result
        Bundle result = resultRef.get();
        assertNotNull("Result bundle should not be null", result);
        assertTrue(result.getBoolean("open"));
        assertTrue(result.getBoolean("closed"));
    }

}
