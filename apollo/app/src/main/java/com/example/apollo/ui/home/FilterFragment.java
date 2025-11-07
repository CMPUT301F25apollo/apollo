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

public class FilterFragment extends Fragment {

    private boolean prevOpen = false;
    private boolean prevClosed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter, container, false);

        CheckBox cbOpen = view.findViewById(R.id.cbOpen);
        CheckBox cbClosed = view.findViewById(R.id.cbClosed);
        Button btnApply = view.findViewById(R.id.btnApply);

        // restore last filter state if passed from HomeFragment
        if (getArguments() != null) {
            prevOpen = getArguments().getBoolean("open", true);
            prevClosed = getArguments().getBoolean("closed", false);
        }

        cbOpen.setChecked(prevOpen);
        cbClosed.setChecked(prevClosed);

        btnApply.setOnClickListener(v -> {
            prevOpen = cbOpen.isChecked();
            prevClosed = cbClosed.isChecked();

            Bundle result = new Bundle();
            result.putBoolean("open", prevOpen);
            result.putBoolean("closed", prevClosed);

            getParentFragmentManager().setFragmentResult("filters", result);
            NavHostFragment.findNavController(this).popBackStack();
        });

        return view;
    }
}