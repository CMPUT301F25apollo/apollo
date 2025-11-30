package com.example.apollo.ui.entrant.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.apollo.R;
import com.example.apollo.databinding.FragmentNotificationsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        adapter = new NotificationsAdapter((notification, position) -> {
            if (!"lottery_win".equals(notification.type)) return;

            // Pass BOTH eventId + notificationId
            showInviteDialog(notification.eventId, notification.id);
        });

        FirebaseFirestore.getInstance()
                .collection("zzz_test")
                .document("ping")
                .set(Collections.singletonMap("ok", true))
                .addOnSuccessListener(v -> Log.d("FIRESTORE", "Firestore WORKS"))
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Firestore FAIL: " + e.getMessage(), e));

        binding.recycler.setAdapter(adapter);

        // Show "empty" message until data is loaded
        binding.empty.setVisibility(View.VISIBLE);

        // No Firestore listener is attached here (handled in onStart)
        return binding.getRoot();


    }

    private void showInviteDialog(String eventId, String notificationId) {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_invite_response, null);

        TextView eventTitle = view.findViewById(R.id.textEventTitle);
        Button accept = view.findViewById(R.id.buttonPopupAccept);
        Button decline = view.findViewById(R.id.buttonPopupDecline);

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(view)
                        .create();

        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        // Load event name
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String title = doc.getString("title");
                        eventTitle.setText("You were selected for:\n" + title);
                    }
                });

        accept.setOnClickListener(v -> {
            dialog.dismiss();
            acceptInvite(eventId, notificationId);
        });

        decline.setOnClickListener(v -> {
            dialog.dismiss();
            declineInvite(eventId, notificationId);
        });

        dialog.show();


    }

    private void acceptInvite(String eventId, String notificationId) {
        String uid = auth.getCurrentUser().getUid();

        DocumentReference regRef = db.collection("events")
                .document(eventId)
                .collection("registrations")
                .document(uid);

        DocumentReference waitRef = db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(uid);

        DocumentReference inviteRef = db.collection("events")
                .document(eventId)
                .collection("invites")
                .document(uid);

        DocumentReference notifRef = db.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId);

        HashMap<String, Object> reg = new HashMap<>();
        reg.put("state", "registered");
        reg.put("registeredAt", FieldValue.serverTimestamp());

        regRef.set(reg)
                .addOnSuccessListener(ok -> {
                    waitRef.delete();
                    inviteRef.delete();
                    notifRef.delete()
                            .addOnSuccessListener(u -> Log.d("TEST", "Notification removed after decline"));



                    Toast.makeText(getContext(), "You are now registered!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void declineInvite(String eventId, String notificationId) {
        String uid = auth.getCurrentUser().getUid();

        DocumentReference inviteRef = db.collection("events")
                .document(eventId)
                .collection("invites")
                .document(uid);

        DocumentReference waitRef = db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(uid);

        DocumentReference notifRef = db.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId);

        // Set their status as declined
        HashMap<String, Object> update = new HashMap<>();
        update.put("state", "declined");
        update.put("updatedAt", FieldValue.serverTimestamp());

        waitRef.set(update)
                .addOnSuccessListener(ok -> {
                    inviteRef.delete();
                    notifRef.delete();


                    waitRef.delete(); // fully remove from waitlist

                    Toast.makeText(getContext(), "Invitation declined.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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