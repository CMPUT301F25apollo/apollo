package com.example.apollo.ui.organizer.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.apollo.R;
import com.example.apollo.databinding.FragmentEventsBinding;

public class EventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventsViewModel eventsViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Use view binding
        binding = FragmentEventsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Connect ViewModel
        eventsViewModel = new ViewModelProvider(this).get(EventsViewModel.class);

        // Find the button from XML (it’s already in fragment_events.xml)
        Button joinButton = root.findViewById(R.id.button_event_waitlist);

        // When button is clicked → join waiting list
        joinButton.setOnClickListener(v -> {
            // Simulate joining event with id "1" as user "hana123"
            eventsViewModel.joinWaitingList("1", "hana123");
            Toast.makeText(getContext(), "Joined waiting list for event 1!", Toast.LENGTH_SHORT).show();
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}