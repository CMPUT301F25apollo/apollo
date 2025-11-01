package com.example.apollo.ui.home;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.apollo.R;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Button event1 = view.findViewById(R.id.button_event1);
        Button event2 = view.findViewById(R.id.button_event2);
        Button event3 = view.findViewById(R.id.button_event3);
        Button event4 = view.findViewById(R.id.button_event4);

        View.OnClickListener listener = v -> {
            Bundle bundle = new Bundle();

            if (v.getId() == R.id.button_event1) {
                bundle.putString("eventTitle", "Beginner Swimming Lessons");
                bundle.putString("eventDescription", "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
                bundle.putString("eventRegistration", "Registration: Oct 05, 2025 - Oct 12, 2025");
            } else if (v.getId() == R.id.button_event2) {
                bundle.putString("eventTitle", "Interpretive Dance Safety Class");
                bundle.putString("eventDescription", "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
                bundle.putString("eventRegistration", "Registration: Dec 01, 2024 - Dec 15, 2024");
            } else if (v.getId() == R.id.button_event3) {
                bundle.putString("eventTitle", "Beginner Piano Lessons");
                bundle.putString("eventDescription", "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
                bundle.putString("eventRegistration", "Registration: Nov 01, 2025 - Nov 10, 2025");
            } else {
                bundle.putString("eventTitle", "Community Gardening Workshop");
                bundle.putString("eventDescription", "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
                bundle.putString("eventRegistration", "Registration: Sep 01, 2025 - Sep 08, 2025");
            }

            Navigation.findNavController(v)
                    .navigate(R.id.navigation_event_details, bundle);
        };

        event1.setOnClickListener(listener);
        event2.setOnClickListener(listener);
        event3.setOnClickListener(listener);
        event4.setOnClickListener(listener);

        return view;
    }
}