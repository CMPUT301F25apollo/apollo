package com.example.apollo.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;

/**
 * FilterFragment
 * This fragment lets users choose which types of events to display (Open or Closed)
 * on the Home screen. It temporarily stores the user’s selections and returns them
 * to HomeFragment using the Fragment Result API when “Apply” is pressed.
 *
 * Current issues:
 * - Support more filter options
 */

public class FilterFragment extends Fragment {

    // Stores the last selected state for each filter checkbox
    private boolean prevOpen = false;
    private boolean prevClosed = false;

    /**
     * Inflates the layout for the filter screen, restores any previously
     * selected filter states (Open/Closed), and sets up the Apply button
     * to send selected filters back to HomeFragment.
     *
     * @param inflater  Used to inflate the fragment's layout
     * @param container The parent view that the fragment's UI should be attached to
     * @param savedInstanceState Saved state data, if available (unused here)
     * @return The root View of the inflated layout for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_filter, container, false);

        // Initialize UI elements
        CheckBox cbOpen = view.findViewById(R.id.cbOpen);
        CheckBox cbClosed = view.findViewById(R.id.cbClosed);
        Button btnApply = view.findViewById(R.id.btnApply);

        // Restore last filter state if passed from HomeFragment
        if (getArguments() != null) {
            prevOpen = getArguments().getBoolean("open", true);
            prevClosed = getArguments().getBoolean("closed", false);
        }

        // Set initial checkbox states
        cbOpen.setChecked(prevOpen);
        cbClosed.setChecked(prevClosed);


        //when apply is clicked, capture the current checkbox selections
        //package them in bundle & send back to HomeFragment using FragmentResult API
        // navigate back to Home screen
        btnApply.setOnClickListener(v -> {
            prevOpen = cbOpen.isChecked();
            prevClosed = cbClosed.isChecked();

            // Create result bundle with current filter settings
            Bundle result = new Bundle();
            result.putBoolean("open", prevOpen);
            result.putBoolean("closed", prevClosed);

            // Send the result back to HomeFragment
            getParentFragmentManager().setFragmentResult("filters", result);

            // Navigate back to HomeFragment
            NavHostFragment.findNavController(this).popBackStack();
        });

        return view;
    }
}