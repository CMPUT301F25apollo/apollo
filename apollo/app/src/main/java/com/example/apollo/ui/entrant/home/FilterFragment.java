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

public class FilterFragment extends Fragment {

    private boolean prevOpen = false;
    private boolean prevClosed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter, container, false);

        // UI elements
        CheckBox cbOpen = view.findViewById(R.id.cbOpen);
        CheckBox cbClosed = view.findViewById(R.id.cbClosed);
        EditText etTitle = view.findViewById(R.id.etTitleKeyword);
        EditText etLocation = view.findViewById(R.id.etLocationKeyword);
        EditText etDate = view.findViewById(R.id.etDate);
        Button btnApply = view.findViewById(R.id.btnApply);

        // Restore previous filter state if passed from HomeFragment
        if (getArguments() != null) {
            prevOpen = getArguments().getBoolean("open", true);
            prevClosed = getArguments().getBoolean("closed", false);
            etTitle.setText(getArguments().getString("titleKeyword", ""));
            etLocation.setText(getArguments().getString("locationKeyword", ""));
            etDate.setText(getArguments().getString("date", ""));
        }

        cbOpen.setChecked(prevOpen);
        cbClosed.setChecked(prevClosed);

        // Apply button click
        btnApply.setOnClickListener(v -> {
            prevOpen = cbOpen.isChecked();
            prevClosed = cbClosed.isChecked();

            Bundle result = new Bundle();
            result.putBoolean("open", prevOpen);
            result.putBoolean("closed", prevClosed);
            result.putString("titleKeyword", etTitle.getText().toString().trim());
            result.putString("locationKeyword", etLocation.getText().toString().trim());
            result.putString("date", etDate.getText().toString().trim());

            // Send results back to HomeFragment
            getParentFragmentManager().setFragmentResult("filters", result);

            // Navigate back
            NavHostFragment.findNavController(this).popBackStack();
        });

        return view;
    }
}
