package com.example.apollo.ui;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.example.apollo.R;
import com.example.apollo.models.Event;

public class EventDetailFragment extends Fragment {

    private TextView title, description, location, time, registration;
    private Button signUpButton;

    private Event event;
    private boolean isOrganizerView = false;

    public EventDetailFragment(Event event, boolean isOrganizerView) {
        this.event = event;
        this.isOrganizerView = isOrganizerView;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_event_detail, container, false);

        // Get references to views that still exist in the new layout
        title = view.findViewById(R.id.textEventTitle);
        description = view.findViewById(R.id.textEventDescription);
        signUpButton = view.findViewById(R.id.buttonSignUp);

        TextView summaryText = view.findViewById(R.id.textEventSummary);

        populateEventDetails(summaryText);

        if (isOrganizerView) {
            signUpButton.setText("EDIT EVENT");
            signUpButton.setOnClickListener(v -> openEditEvent());
        } else {
            signUpButton.setOnClickListener(v -> joinWaitlist());
        }

        return view;
    }

    private void populateEventDetails(TextView summaryText) {
        title.setText(event.getTitle());
        description.setText(event.getDescription());

        String summary = event.getTitle() + "\n" +
                "Location: " + event.getLocation() + "\n" +
                "Time: " + event.getTime() + "\n" +
                "Registration between " + event.getRegistrationStart() + " - " + event.getRegistrationEnd();

        summaryText.setText(summary);
    }

    private void joinWaitlist() {
        // TODO: Implement waitlist logic
    }

    private void openEditEvent() {
        // TODO: Implement edit logic
    }
}