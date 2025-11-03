package com.example.apollo.ui.organizer.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.example.apollo.databinding.OrganiserFragmentEventsBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class EventsFragment extends Fragment {

    private OrganiserFragmentEventsBinding binding;
    private EventsViewModel eventsViewModel;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = OrganiserFragmentEventsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        eventsViewModel = new ViewModelProvider(this).get(EventsViewModel.class);

        // Load events
        loadEvents();

        // Add new event button
        binding.addNewEventButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_events_to_navigation_add_event);
        });

        return root;
    }

    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    LinearLayout container = binding.eventsContainer;
                    container.removeAllViews();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String title = document.getString("title");
                        String date = document.getString("date");
                        String time = document.getString("time");

                        View card = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_event_card, container, false);

                        TextView titleView = card.findViewById(R.id.eventTitle);
                        TextView imageLabelView = card.findViewById(R.id.eventImageLabel);

                        titleView.setText(title != null ? title : "Untitled Event");
                        imageLabelView.setText(date + " at " + time);


                        container.addView(card);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading events", e));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
