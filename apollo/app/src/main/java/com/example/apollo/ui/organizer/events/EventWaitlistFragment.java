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

public class EventWaitlistFragment extends Fragment {

    private ListView listView;
    private TextView emptyTextView;
    private ArrayAdapter<String> adapter;
    private List<String> entrantsList = new ArrayList<>();

    private FirebaseFirestore db;
    private String eventId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_waitlist, container, false);

        listView = view.findViewById(R.id.listView);
        emptyTextView = view.findViewById(R.id.textView);

        // Setup adapter
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, entrantsList);
        listView.setAdapter(adapter);
        listView.setEmptyView(emptyTextView);

        db = FirebaseFirestore.getInstance();

        // Get eventId from arguments
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadWaitlistEntrants();
        }

        return view;
    }

    private void loadWaitlistEntrants() {
        if (eventId == null) return;

        emptyTextView.setText(""); // clear previous text
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
                        db.collection("users")
                                .document(entrantId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    uniqueEntrants.add(name != null ? name : entrantId);

                                    // Update list and adapter only once
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
                .addOnFailureListener(e -> {
                    emptyTextView.setText("Failed to load waitlist");
                });
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Allow back arrow handling
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavHostFragment.findNavController(this).popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onResume() {
        super.onResume();
        loadWaitlistEntrants();  // reload the list when fragment becomes visible
    }

}


