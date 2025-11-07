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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Fragment that displays a list of events created by the currently logged-in organizer.
 * <p>
 * Each event card includes a title, optional poster image, and click navigation
 * to the corresponding event details screen. The fragment also provides a button
 * to add new events.
 */
public class EventsFragment extends Fragment {

    /** View binding instance for accessing layout elements safely. */
    private FragmentOrganizerEventsBinding binding;

    /** ViewModel instance for managing event-related data (reserved for future use). */
    private EventsViewModel eventsViewModel;

    /** Firestore instance used to load events from the database. */
    private FirebaseFirestore db;

    /**
     * Called to have the fragment instantiate its user interface view.
     * <p>
     * This method inflates the fragment layout, initializes Firebase and ViewModel instances,
     * loads the organizer’s events from Firestore, and sets up navigation for creating new events.
     *
     * @param inflater  LayoutInflater used to inflate the layout XML file
     * @param container Optional parent view that the fragment’s UI should attach to
     * @param savedInstanceState previously saved instance state, if available
     * @return the root view for this fragment’s layout
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentOrganizerEventsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        eventsViewModel = new ViewModelProvider(this).get(EventsViewModel.class);

        // Load all events created by the logged-in organizer
        loadEvents();

        // Navigate to the Add Event screen when the button is clicked
        binding.addNewEventButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_organizer_events_to_navigation_organizer_add_event);
        });

        return root;
    }

    /**
     * Fetches and displays all events created by the current organizer.
     * <p>
     * The method queries Firestore for documents in the "events" collection
     * where {@code creatorId} matches the current user’s UID. For each event document,
     * a card view is inflated dynamically and populated with title, image, and click behavior.
     * If an event poster URL is available, Glide loads and displays it.
     */
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

                        // Inflate a new event card layout
                        View card = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_event_card, container, false);

                        // Set event title
                        TextView titleView = card.findViewById(R.id.eventTitle);
                        titleView.setText(title);

                        // Load poster image if available
                        ImageView posterView = card.findViewById(R.id.eventPosterImage);
                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(posterUrl)
                                    .into(posterView);
                        }

                        // Navigate to the event details screen when the card is tapped
                        card.setOnClickListener(v -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("eventId", eventId);

                            NavController navController = NavHostFragment.findNavController(this);
                            navController.navigate(R.id.action_navigation_organizer_events_to_organizer_event_details, bundle);
                        });

                        // Add card to container layout
                        container.addView(card);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading events", e));
    }

    /**
     * Cleans up the view binding reference when the view hierarchy is destroyed
     * to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
