package com.example.apollo.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.apollo.R;
import com.google.android.material.button.MaterialButton;
import android.widget.TextView;



public class EventDetailsFragment extends Fragment {

    private boolean joined = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_details, container, false);

        // Grab views
        TextView title = view.findViewById(R.id.text_event_name);
        TextView info = view.findViewById(R.id.text_event_info);
        MaterialButton joinButton = view.findViewById(R.id.button_join_waitlist);

        // Get bundle data
        Bundle args = getArguments();
        if (args != null) {
            String eventTitle = args.getString("eventTitle", "Unknown Event");
            String eventLocation = args.getString("eventLocation", "Unknown Location");
            String eventTime = args.getString("eventTime", "Unknown Time");
            String eventRegistration = args.getString("eventRegistration", "N/A");

            title.setText(eventTitle);
            info.setText(
                    eventTitle + "\n" +
                            "Location: " + eventLocation + "\n" +
                            "Time: " + eventTime + "\n" +
                            "Registration between " + eventRegistration
            );
        }

        //join button toggle logic
        joinButton.setOnClickListener(v -> {
            if (!joined) {
                joinButton.setText("JOINED!");
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.black));
                joinButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                joined = true;
            } else {
                joinButton.setText("Join Waiting List");
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.lightblue));
                joinButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
                joined = false;
            }
        });

        return view;
    }
}