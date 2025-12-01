package com.example.apollo.ui.organizer.events;

import android.app.AlertDialog;
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

/**
 * EventWaitlistFragment.java
 *
 * Organizer-facing fragment that displays everyone associated with a single event:
 * - Entrants currently on the waitlist
 * - Registered (accepted) entrants
 * - Declined entrants
 *
 * Features:
 * - Filters entrants by status using a Spinner (All / Accepted / Declined / Invited / Loser / Waiting)
 * - Allows cancelling an invitation for "Invited" entrants
 * - Exports the currently visible list as a CSV file for sharing
 */
public class EventWaitlistFragment extends Fragment {

    /**
     * Simple data model for displaying entrant details in the ListView.
     */
    private static class Entrant {
        String id;
        String name;
        String status;

        Entrant(String id, String name, String status) {
            this.id = id;
            this.name = name;
            this.status = status;
        }

        String getId() { return id; }
        String getName() { return name; }
        String getStatus() { return status; }

        @NonNull
        @Override
        public String toString() {
            // Displayed in the ListView row
            return name + " – " + status;
        }
    }

    private ListView listView;
    private TextView emptyTextView;
    private ArrayAdapter<Entrant> adapter;
    private final List<Entrant> entrantsList = new ArrayList<>();
    private final List<Entrant> allEntrants = new ArrayList<>();

    private FirebaseFirestore db;
    private String eventId;
    private Spinner filterSpinner;

    /**
     * Inflates the layout, sets up the ListView, filter Spinner, export button,
     * and kicks off loading the waitlist for the provided eventId.
     */
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

        // Setup filter options
        String[] filterOptions = {"All", "Accepted", "Declined", "invited", "Loser", "Waiting"};
        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, filterOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        // Re-filter whenever selection changes
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Tap entrant to cancel "Invited" entries
        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            Entrant selectedEntrant = entrantsList.get(position);
            if ("Invited".equalsIgnoreCase(selectedEntrant.getStatus())) {
                showCancelInvitationDialog(selectedEntrant);
            } else {
                Toast.makeText(getContext(),
                        "Only invited entrants (status: Invited) can be cancelled.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadWaitlistEntrants();
        }

        Button exportButton = view.findViewById(R.id.exportCsvButton);
        exportButton.setOnClickListener(v -> exportWaitlistToCsv());

        return view;
    }

    /**
     * Shows a confirmation dialog before cancelling an invited entrant.
     *
     * @param entrant The entrant whose invitation may be cancelled.
     */
    private void showCancelInvitationDialog(Entrant entrant) {
        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Invitation")
                .setMessage("Are you sure you want to cancel the invitation for " + entrant.getName() + "? This action cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelInvitation(entrant))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Updates the entrant's waitlist state to "Cancelled" for this event.
     * After success, the list is reloaded to reflect the new status.
     *
     * @param entrant Entrant whose invitation is being cancelled.
     */
    private void cancelInvitation(Entrant entrant) {
        if (eventId == null || entrant == null || entrant.getId() == null) {
            Toast.makeText(getContext(), "Error: Cannot cancel invitation.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(eventId)
                .collection("waitlist").document(entrant.getId())
                .update("state", "Cancelled")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            entrant.getName() + "'s invitation has been cancelled.",
                            Toast.LENGTH_SHORT).show();
                    loadWaitlistEntrants(); // Refresh list
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to cancel invitation. Please try again.",
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads all entrants associated with this event from:
     * - waitlist
     * - registrations
     * - declined
     *
     * Then joins them with user details from "users" and builds the in-memory list used by the UI.
     */
    private void loadWaitlistEntrants() {
        if (eventId == null) return;

        emptyTextView.setText("Loading entrants...");
        allEntrants.clear();
        applyFilter();

        Task<QuerySnapshot> waitlistTask =
                db.collection("events").document(eventId).collection("waitlist").get();
        Task<QuerySnapshot> registrationsTask =
                db.collection("events").document(eventId).collection("registrations").get();
        Task<QuerySnapshot> declinedTask =
                db.collection("events").document(eventId).collection("declined").get();

        Tasks.whenAllSuccess(waitlistTask, registrationsTask, declinedTask)
                .addOnSuccessListener(snapshots -> {
                    Map<String, String> entrantStates = new HashMap<>();

                    // Declined entrants
                    QuerySnapshot declinedSnapshot = (QuerySnapshot) snapshots.get(2);
                    for (QueryDocumentSnapshot doc : declinedSnapshot) {
                        entrantStates.put(doc.getId(), "Declined");
                    }

                    // Accepted / registered entrants
                    QuerySnapshot registrationsSnapshot = (QuerySnapshot) snapshots.get(1);
                    for (QueryDocumentSnapshot doc : registrationsSnapshot) {
                        entrantStates.put(doc.getId(), "Accepted");
                    }

                    // Waitlist entries (only if not already labeled above)
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

                    // Now fetch user details for each entrant
                    List<Task<DocumentSnapshot>> userDetailTasks = new ArrayList<>();
                    for (String entrantId : entrantStates.keySet()) {
                        userDetailTasks.add(
                                db.collection("users").document(entrantId).get()
                        );
                    }

                    Tasks.whenAllSuccess(userDetailTasks).addOnSuccessListener(userSnapshots -> {
                        List<Entrant> newEntrantsList = new ArrayList<>();
                        for (Object snapshot : userSnapshots) {
                            DocumentSnapshot userDoc = (DocumentSnapshot) snapshot;
                            if (!userDoc.exists()) continue;

                            String entrantId = userDoc.getId();
                            String name = userDoc.getString("name");
                            String displayName = (name != null) ? name : entrantId;
                            String status = entrantStates.get(entrantId);

                            if (status != null) {
                                String displayStatus =
                                        status.substring(0, 1).toUpperCase() + status.substring(1);
                                newEntrantsList.add(new Entrant(entrantId, displayName, displayStatus));
                            }
                        }

                        // Sort entrants alphabetically by name
                        Collections.sort(newEntrantsList,
                                (e1, e2) -> e1.getName().compareToIgnoreCase(e2.getName()));

                        allEntrants.clear();
                        allEntrants.addAll(newEntrantsList);
                        applyFilter();

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

    /**
     * Applies the current filter selection from the Spinner to the full list
     * and updates the visible ListView entries.
     */
    private void applyFilter() {
        if (filterSpinner == null || allEntrants == null) {
            return;
        }

        String selectedFilter = filterSpinner.getSelectedItem().toString();
        List<Entrant> filteredList = new ArrayList<>();

        if (selectedFilter.equals("All")) {
            filteredList.addAll(allEntrants);
        } else {
            for (Entrant entry : allEntrants) {
                if (entry.getStatus().equalsIgnoreCase(selectedFilter)) {
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

    /**
     * Exports the currently visible entrants (after filtering) to a CSV file
     * and then calls {@link #shareCsv(File)} to share it via an external app.
     */
    private void exportWaitlistToCsv() {
        if (entrantsList.isEmpty()) {
            Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Name,Status\n");

        for (Entrant entry : entrantsList) {
            csvBuilder.append(entry.getName()).append(",").append(entry.getStatus()).append("\n");
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

    /**
     * Shares the generated CSV file using an ACTION_SEND intent and FileProvider.
     *
     * @param file CSV file to be shared.
     */
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

    /**
     * Enables handling of the ActionBar back/home button to pop this fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Handles toolbar home (up) presses to navigate back in the stack.
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
     * Refreshes the entrants list whenever the fragment is resumed,
     * ensuring the UI reflects latest states (accepted/declined/etc.).
     */
    @Override
    public void onResume() {
        super.onResume();
        if (eventId != null) {
            loadWaitlistEntrants();
        }
    }
}
