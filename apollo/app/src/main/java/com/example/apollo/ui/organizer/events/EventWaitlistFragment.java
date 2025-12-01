package com.example.apollo.ui.organizer.events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventWaitlistFragment extends Fragment {

    private ListView listView;
    private TextView emptyTextView;
    private ArrayAdapter<String> adapter;
    private final List<String> entrantsList = new ArrayList<>();

    private FirebaseFirestore db;
    private String eventId;
    private final List<String> allEntrants = new ArrayList<>();
    private Spinner filterSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_waitlist, container, false);

        listView = view.findViewById(R.id.listView);
        emptyTextView = view.findViewById(R.id.emptyText);
        filterSpinner = view.findViewById(R.id.filterSpinner);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, entrantsList);
        listView.setAdapter(adapter);
        listView.setEmptyView(emptyTextView);

        // Setup for Spinner
        String[] filterOptions = {"All", "Accepted", "Declined", "Loser", "Waiting"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, filterOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        db = FirebaseFirestore.getInstance();

        // Get eventId from arguments
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadWaitlistEntrants();
        }

        // Export CSV button
        Button exportButton = view.findViewById(R.id.exportCsvButton);
        exportButton.setOnClickListener(v -> exportWaitlistToCsv());

        return view;
    }

    private void loadWaitlistEntrants() {
        if (eventId == null) return;

        emptyTextView.setText("Loading entrants...");
        entrantsList.clear();
        adapter.notifyDataSetChanged();

        Task<QuerySnapshot> waitlistTask = db.collection("events").document(eventId).collection("waitlist").get();
        Task<QuerySnapshot> registrationsTask = db.collection("events").document(eventId).collection("registrations").get();
        Task<QuerySnapshot> declinedTask = db.collection("events").document(eventId).collection("declined").get();

        Tasks.whenAllSuccess(waitlistTask, registrationsTask, declinedTask).addOnSuccessListener(snapshots -> {
            Map<String, String> entrantStates = new HashMap<>();

            // Process declined first
            QuerySnapshot declinedSnapshot = (QuerySnapshot) snapshots.get(2);
            for (QueryDocumentSnapshot doc : declinedSnapshot) {
                entrantStates.put(doc.getId(), "Declined");
            }

            // Process registrations next
            QuerySnapshot registrationsSnapshot = (QuerySnapshot) snapshots.get(1);
            for (QueryDocumentSnapshot doc : registrationsSnapshot) {
                entrantStates.put(doc.getId(), "Accepted");
            }

            // Process waitlist for users not already processed
            QuerySnapshot waitlistSnapshot = (QuerySnapshot) snapshots.get(0);
            for (QueryDocumentSnapshot doc : waitlistSnapshot) {
                if (!entrantStates.containsKey(doc.getId())) {
                    String state = doc.getString("state");
                    entrantStates.put(doc.getId(), (state != null) ? state : "unknown");
                }
            }

            if (entrantStates.isEmpty()) {
                emptyTextView.setText("No one has joined the event");
                allEntrants.clear();
                applyFilter();
                return;
            }

            List<Task<DocumentSnapshot>> userDetailTasks = new ArrayList<>();
            for (String entrantId : entrantStates.keySet()) {
                userDetailTasks.add(db.collection("users").document(entrantId).get());
            }

            Tasks.whenAllSuccess(userDetailTasks).addOnSuccessListener(userSnapshots -> {
                List<String> newEntrantsList = new ArrayList<>();
                for (Object snapshot : userSnapshots) {
                    DocumentSnapshot userDoc = (DocumentSnapshot) snapshot;
                    if (!userDoc.exists()) continue;

                    String entrantId = userDoc.getId();
                    String name = userDoc.getString("name");
                    String displayName = (name != null) ? name : entrantId;
                    String status = entrantStates.get(entrantId);

                    if (status != null) {
                        String displayStatus = status.substring(0, 1).toUpperCase() + status.substring(1);
                        newEntrantsList.add(displayName + " – " + displayStatus);
                    }
                }

                Collections.sort(newEntrantsList);

                allEntrants.clear();
                allEntrants.addAll(newEntrantsList);
                applyFilter(); // This will handle updating entrantsList and the adapter

                if (allEntrants.isEmpty()) {
                    emptyTextView.setText("No entrants found");
                }

            }).addOnFailureListener(e -> {
                emptyTextView.setText("Failed to load entrant details");
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            emptyTextView.setText("Failed to load event data");
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void applyFilter() {
        if (filterSpinner == null || allEntrants == null) {
            return;
        }

        String selectedFilter = filterSpinner.getSelectedItem().toString();
        List<String> filteredList = new ArrayList<>();

        if (selectedFilter.equals("All")) {
            filteredList.addAll(allEntrants);
        } else {
            for (String entry : allEntrants) {
                // The status is after " – "
                if (entry.endsWith("– " + selectedFilter)) {
                    filteredList.add(entry);
                }
            }
        }

        entrantsList.clear();
        entrantsList.addAll(filteredList);
        adapter.notifyDataSetChanged();

        if (entrantsList.isEmpty() && !allEntrants.isEmpty()) {
            emptyTextView.setText("No entrants match the filter '" + selectedFilter + "'");
            emptyTextView.setVisibility(View.VISIBLE);
        } else if (allEntrants.isEmpty()) {
            emptyTextView.setText("No one has joined the event");
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            emptyTextView.setVisibility(View.GONE);
        }
    }
    private void exportWaitlistToCsv() {
        if (entrantsList.isEmpty()) {
            Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Name,Status");

        for (String entry : entrantsList) {
            String[] parts = entry.split(" – ");
            String name = parts[0];
            String status = (parts.length > 1) ? parts[1] : "unknown";

            csvBuilder.append(" ");
            csvBuilder.append(name).append(",").append(status);
        }

        try {
            String fileName = "waitlist_export_" + System.currentTimeMillis() + ".csv";
            File file = new File(getContext().getExternalFilesDir(null), fileName);

            FileWriter writer = new FileWriter(file);
            writer.write(csvBuilder.toString());
            writer.flush();
            writer.close();

            shareCsv(file);

        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to export CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCsv(File file) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Share CSV File"));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (eventId != null) {
            loadWaitlistEntrants();
        }
    }
}
