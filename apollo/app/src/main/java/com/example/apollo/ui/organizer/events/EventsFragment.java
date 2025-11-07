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
 * EventsFragment.java
 *
 * Purpose:
 * Displays all events created by the currently logged-in organizer.
 * Fetches data from Firestore and displays event cards with title, image, and navigation options.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern, managing the display of Firestore data (model)
 * inside a scrollable layout (view).
 *
 * Notes:
 * - Only events created by the logged-in user are displayed.
 * - Each card navigates to an event detail page when clicked.
 * - Glide is used for image loading.
 */
public class EventsFragment extends Fragment {

    /** View binding instance for accessing layout elements safely. */
    private FragmentOrganizerEventsBinding binding;

    /** ViewModel instance for managing event-related data (reserved for future use). */
    private EventsViewModel eventsViewModel;

    /** Firestore instance used to load events from the database. */
    private FirebaseFirestore db;

    /**
     * Called when the fragment’s view is created.
     * Initializes Firebase, loads events for the current user, and sets up button listeners.
     *
     * @param inflater Used to inflate the fragment layout.
     * @param container The parent view group for the fragment.
     * @param savedInstanceState The saved instance state, if available.
     * @return The root view of the fragment.
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

        // Handle "Add New Event" button click
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
                .whereEqualTo("creatorId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    LinearLayout container = binding.eventsContainer;
                    container.removeAllViews();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String eventId = document.getId();
                        String title = document.getString("title");
                        String posterUrl = document.getString("eventPosterUrl");

                        // Inflate card layout
                        View card = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_event_card, container, false);

                        // Set title
                        TextView titleView = card.findViewById(R.id.eventTitle);
                        titleView.setText(title);

                        // Set image if available
                        ImageView posterView = card.findViewById(R.id.eventPosterImage);
                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(posterUrl)
                                    .into(posterView);
                        }

                        // Navigate to event details on click
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
     * Called when the fragment’s view is destroyed.
     * Clears the binding reference to avoid memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
