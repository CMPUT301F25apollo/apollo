package com.example.apollo.ui.organizer.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.example.apollo.databinding.FragmentOrganizerEventsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class EventsFragment extends Fragment {

    private FragmentOrganizerEventsBinding binding;
    private EventsViewModel eventsViewModel;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentOrganizerEventsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        eventsViewModel = new ViewModelProvider(this).get(EventsViewModel.class);

        loadEvents();

        // Add new event button
        binding.addNewEventButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_organizer_events_to_navigation_organizer_add_event);
        });

        return root;
    }

    private void loadEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "No logged-in user");
            return;
        }

        String uid = currentUser.getUid();

        db.collection("events")
                .whereEqualTo("creatorId", uid)  // Only fetch events created by this user
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    LinearLayout container = binding.eventsContainer;
                    container.removeAllViews();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String eventId = document.getId();
                        String title = document.getString("title");
                        String description = document.getString("description");
                        String location = document.getString("location");
                        String time = document.getString("time");
                        String date = document.getString("date");
                        String posterUrl = document.getString("eventPosterUrl");

                        View card = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_event_card, container, false);

                        TextView titleView = card.findViewById(R.id.eventTitle);
                        titleView.setText(title);

                        ImageView posterView = card.findViewById(R.id.eventPosterImage);

                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(posterUrl)
                                    .into(posterView);
                        }

                        card.setOnClickListener(v -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("eventId", eventId);

                            NavController navController = NavHostFragment.findNavController(this);
                            navController.navigate(R.id.action_navigation_organizer_events_to_organizer_event_details, bundle);
                        });

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