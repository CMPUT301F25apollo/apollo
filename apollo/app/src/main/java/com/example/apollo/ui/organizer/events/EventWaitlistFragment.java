package com.example.apollo.ui.organizer.events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EventWaitlistFragment extends Fragment {

    private ListView listView;
    private TextView emptyTextView;
    private ArrayAdapter<String> adapter;
    private final List<String> entrantsList = new ArrayList<>();

    private FirebaseFirestore db;
    private String eventId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_waitlist, container, false);

        listView = view.findViewById(R.id.listView);
        emptyTextView = view.findViewById(R.id.emptyText);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, entrantsList);
        listView.setAdapter(adapter);
        listView.setEmptyView(emptyTextView);

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

                        db.collection("users")
                                .document(entrantId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    String displayName = (name != null) ? name : entrantId;

                                    // Check winner
                                    db.collection("events")
                                            .document(eventId)
                                            .collection("lotteryResults")
                                            .document("winners")
                                            .collection("users")
                                            .document(entrantId)
                                            .get()
                                            .addOnSuccessListener(winnerDoc -> {

                                                if (winnerDoc.exists()) {
                                                    uniqueEntrants.add(displayName + " – winner");
                                                    updateUI(uniqueEntrants);
                                                    return;
                                                }

                                                // Check loser
                                                db.collection("events")
                                                        .document(eventId)
                                                        .collection("lotteryResults")
                                                        .document("losers")
                                                        .collection("users")
                                                        .document(entrantId)
                                                        .get()
                                                        .addOnSuccessListener(loserDoc -> {

                                                            String status = loserDoc.exists() ? "loser" : "waiting";
                                                            uniqueEntrants.add(displayName + " – " + status);

                                                            updateUI(uniqueEntrants);
                                                        });
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    uniqueEntrants.add(entrantId);
                                    updateUI(uniqueEntrants);
                                });
                    }
                })
                .addOnFailureListener(e -> emptyTextView.setText("Failed to load waitlist"));
    }

    private void updateUI(Set<String> data) {
        entrantsList.clear();
        entrantsList.addAll(data);
        adapter.notifyDataSetChanged();
    }

    private void exportWaitlistToCsv() {
        if (entrantsList.isEmpty()) {
            Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Name,Status\n");

        for (String entry : entrantsList) {
            String[] parts = entry.split(" – ");
            String name = parts[0];
            String status = (parts.length > 1) ? parts[1] : "unknown";

            csvBuilder.append(name).append(",").append(status).append("\n");
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
        loadWaitlistEntrants();
    }
}
