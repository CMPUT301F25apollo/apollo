package com.example.apollo.ui.organizer.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * EventWaitlistFragment.java
 *
 * Purpose:
 * Displays a list of entrants currently on the waitlist for a specific event.
 * Fetches data from Firestore and shows entrant names or IDs in a simple ListView.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern, retrieving waitlist data from Firestore (model)
 * and displaying it in the UI (view).
 *
 * Notes:
 * - Shows an empty message if there are no entrants.
 * - Automatically refreshes the list when the fragment becomes visible.
 */
public class EventWaitlistFragment extends Fragment {

    private ListView listView;
    private TextView emptyTextView;
    private ArrayAdapter<String> adapter;
    private final List<String> entrantsList = new ArrayList<>();

    private FirebaseFirestore db;
    private String eventId;

    /**
     * Called when the fragment’s view is created.
     * Initializes Firestore, sets up the ListView adapter, and loads waitlist data.
     *
     * @param inflater Used to inflate the fragment layout.
     * @param container The parent view that the fragment attaches to.
     * @param savedInstanceState The saved instance state, if available.
     * @return The root view for the fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_waitlist, container, false);

        listView = view.findViewById(R.id.listView);
        emptyTextView = view.findViewById(R.id.textView);

        // Set up the adapter for the ListView
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, entrantsList);
        listView.setAdapter(adapter);
        listView.setEmptyView(emptyTextView);

        db = FirebaseFirestore.getInstance();

        // Get eventId from fragment arguments
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadWaitlistEntrants();
        }

        return view;
    }

    /**
     * Loads and displays the entrants currently in the waitlist for this event.
     * Queries Firestore for entrants and fetches their names from the users collection.
     */
    private void loadWaitlistEntrants() {
        if (eventId == null) return;

        emptyTextView.setText("");
        entrantsList.clear();
        adapter.notifyDataSetChanged();

        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .whereEqualTo("state", "waiting")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        emptyTextView.setText("No entrants in waitlist");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    Set<String> uniqueEntrants = new LinkedHashSet<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String entrantId = doc.getId();

                        // Fetch entrant details from users collection
                        db.collection("users")
                                .document(entrantId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    uniqueEntrants.add(name != null ? name : entrantId);

                                    entrantsList.clear();
                                    entrantsList.addAll(uniqueEntrants);
                                    adapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    uniqueEntrants.add(entrantId);
                                    entrantsList.clear();
                                    entrantsList.addAll(uniqueEntrants);
                                    adapter.notifyDataSetChanged();
                                });
                    }
                })
                .addOnFailureListener(e -> emptyTextView.setText("Failed to load waitlist"));
    }

    /**
     * Enables the back arrow in the toolbar for navigation.
     *
     * @param savedInstanceState The saved state of the fragment, if available.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Handles toolbar item selections such as the back arrow.
     *
     * @param item The selected menu item.
     * @return true if handled, otherwise passes to the superclass.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavHostFragment.findNavController(this).popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Reloads the waitlist whenever the fragment becomes visible again.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadWaitlistEntrants();
    }
}
