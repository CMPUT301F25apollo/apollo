package com.example.apollo.ui.entrant.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.apollo.databinding.FragmentNotificationsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationsFragment.java
 *
 * Purpose:
 * Displays a list of notifications for the current user.
 * Retrieves notification data from Firestore and shows it using a RecyclerView.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern, connecting the Firestore data (model)
 * with the RecyclerView adapter (view).
 *
 * Notes:
 * - Currently, notifications are non-clickable.
 * - Can be extended later to support marking notifications as read or opening details.
 */
public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration reg;
    private NotificationsAdapter adapter;

    /**
     * Called when the fragment’s view is being created.
     * Sets up the RecyclerView and initializes Firebase instances.
     *
     * @param inflater Used to inflate the fragment layout.
     * @param container The parent view that the fragment attaches to.
     * @param savedInstanceState The saved state of the fragment, if available.
     * @return The root view for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Set up RecyclerView layout
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Adapter without click functionality for now
        adapter = new NotificationsAdapter(null);
        binding.recycler.setAdapter(adapter);

        // Show "empty" message until data is loaded
        binding.empty.setVisibility(View.VISIBLE);

        // No Firestore listener is attached here (handled in onStart)
        return binding.getRoot();
    }

    /**
     * Called when the fragment becomes visible.
     * Starts listening for real-time updates from Firestore and updates the RecyclerView.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        // Listen for notifications from Firestore, ordered by newest first
        reg = db.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    List<NotificationsViewModel> fresh = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        fresh.add(NotificationsViewModel.from(d));
                    }

                    adapter.setData(fresh);
                    binding.empty.setVisibility(fresh.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    /**
     * Called when the fragment is no longer visible.
     * Removes the Firestore listener to avoid memory leaks.
     */
    @Override
    public void onStop() {
        if (reg != null) {
            reg.remove();
            reg = null;
        }
        super.onStop();
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
