/**
 * FilterFragment.java
 *
 * This fragment lets entrants apply filters to the event list.
 * Users can filter by open/closed registration, title keyword, location,
 * date, and a set of event categories. The chosen filters are returned to
 * the calling fragment via FragmentResult.
 */
package com.example.apollo.ui.entrant.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;

import java.util.ArrayList;

/**
 * Fragment that displays filter options for the home event list.
 * It restores any previously selected filters from arguments and
 * sends the updated filter values back when the user taps Apply.
 */
public class FilterFragment extends Fragment {

    private boolean prevOpen = false;
    private boolean prevClosed = false;

    /**
     * Inflates the filter layout, restores existing filter values from
     * arguments (if any), and sets up the Apply button to return the
     * selected filters back to the parent fragment.
     *
     * @param inflater  LayoutInflater used to inflate the UI.
     * @param container Parent view group for the fragment (may be null).
     * @param savedInstanceState Previously saved state (not used here).
     * @return The root view for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter, container, false);

        // Existing UI
        CheckBox cbOpen = view.findViewById(R.id.cbOpen);
        CheckBox cbClosed = view.findViewById(R.id.cbClosed);
        EditText etTitle = view.findViewById(R.id.etTitleKeyword);
        EditText etLocation = view.findViewById(R.id.etLocationKeyword);
        EditText etDate = view.findViewById(R.id.etDate);
        Button btnApply = view.findViewById(R.id.btnApply);

        // checkboxes
        CheckBox catYoga = view.findViewById(R.id.catYoga);
        CheckBox catFitness = view.findViewById(R.id.catFitness);
        CheckBox catKidsSports = view.findViewById(R.id.catKidsSports);
        CheckBox catMartialArts = view.findViewById(R.id.catMartialArts);
        CheckBox catTennis = view.findViewById(R.id.catTennis);
        CheckBox catAquatics = view.findViewById(R.id.catAquatics);
        CheckBox catAdultSports = view.findViewById(R.id.catAdultSports);
        CheckBox catWellness = view.findViewById(R.id.catWellness);
        CheckBox catCreative = view.findViewById(R.id.catCreative);
        CheckBox catCamps = view.findViewById(R.id.catCamps);

        // Restore previous filter state
        if (getArguments() != null) {
            prevOpen = getArguments().getBoolean("open", true);
            prevClosed = getArguments().getBoolean("closed", false);
            etTitle.setText(getArguments().getString("titleKeyword", ""));
            etLocation.setText(getArguments().getString("locationKeyword", ""));
            etDate.setText(getArguments().getString("date", ""));

            // Restore categories
            ArrayList<String> prevCategories =
                    getArguments().getStringArrayList("categories");

            if (prevCategories != null) {
                catYoga.setChecked(prevCategories.contains("Yoga and Mindfulness"));
                catFitness.setChecked(prevCategories.contains("Strength and Fitness Classes"));
                catKidsSports.setChecked(prevCategories.contains("Kids Sports Programs"));
                catMartialArts.setChecked(prevCategories.contains("Martial Arts"));
                catTennis.setChecked(prevCategories.contains("Tennis and Racquet Sports"));
                catAquatics.setChecked(prevCategories.contains("Aquatics and Swimming Lessons"));
                catAdultSports.setChecked(prevCategories.contains("Adult Drop-In Sports"));
                catWellness.setChecked(prevCategories.contains("Health and Wellness Workshops"));
                catCreative.setChecked(prevCategories.contains("Arts, Music and Creative Programs"));
                catCamps.setChecked(prevCategories.contains("Special Events and Camps"));
            }
        }

        cbOpen.setChecked(prevOpen);
        cbClosed.setChecked(prevClosed);

        // Apply button click
        btnApply.setOnClickListener(v -> {

            prevOpen = cbOpen.isChecked();
            prevClosed = cbClosed.isChecked();

            // Collect selected categories
            ArrayList<String> selectedCategories = new ArrayList<>();

            if (catYoga.isChecked()) selectedCategories.add("Yoga and Mindfulness");
            if (catFitness.isChecked()) selectedCategories.add("Strength and Fitness Classes");
            if (catKidsSports.isChecked()) selectedCategories.add("Kids Sports Programs");
            if (catMartialArts.isChecked()) selectedCategories.add("Martial Arts");
            if (catTennis.isChecked()) selectedCategories.add("Tennis & Racquet Sports");
            if (catAquatics.isChecked()) selectedCategories.add("Aquatics and Swimming Lessons");
            if (catAdultSports.isChecked()) selectedCategories.add("Adult Drop-In Sports");
            if (catWellness.isChecked()) selectedCategories.add("Health and Wellness Workshops");
            if (catCreative.isChecked()) selectedCategories.add("Arts, Music and Creative Programs");
            if (catCamps.isChecked()) selectedCategories.add("Special Events and Camps");

            Bundle result = new Bundle();
            result.putBoolean("open", prevOpen);
            result.putBoolean("closed", prevClosed);
            result.putString("titleKeyword", etTitle.getText().toString().trim());
            result.putString("locationKeyword", etLocation.getText().toString().trim());
            result.putString("date", etDate.getText().toString().trim());

            // Add categories to result
            result.putStringArrayList("categories", selectedCategories);

            // Send results back to HomeFragment
            getParentFragmentManager().setFragmentResult("filters", result);

            // Navigate back
            NavHostFragment.findNavController(this).popBackStack();
        });

        return view;
    }
}
