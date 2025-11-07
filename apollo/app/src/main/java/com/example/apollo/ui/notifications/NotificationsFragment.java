package com.example.apollo.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.apollo.R;
import com.example.apollo.databinding.FragmentNotificationsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration reg;
    private NotificationsAdapter adapter;
    private final List<NotificationsViewModel> items = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Recycler setup
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationsAdapter(null);
        binding.recycler.setAdapter(adapter);

// Empty state visible until data arrives
        binding.empty.setVisibility(View.VISIBLE);

// --- Firestore: listen for notifications for the current user ---
        if (auth.getCurrentUser() != null) {
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .collection("notifications")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener((snap, e) -> {
                        if (e != null || snap == null) return;

                        java.util.List<NotificationsViewModel> items = new java.util.ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                            items.add(NotificationsViewModel.from(d));
                        }

                        adapter.setData(items);

                        // toggle empty state visibility
                        binding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    });
        }


        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        reg = db.collection("users").document(uid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    // live diff handling
                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        DocumentSnapshot d = dc.getDocument();
                        NotificationsViewModel n = NotificationsViewModel.from(d);
                        switch (dc.getType()) {
                            case ADDED:
                                items.add(dc.getNewIndex(), n);
                                adapter.notifyItemInserted(dc.getNewIndex());
                                break;
                            case MODIFIED:
                                items.set(dc.getNewIndex(), n);
                                adapter.notifyItemChanged(dc.getNewIndex());
                                break;
                            case REMOVED:
                                items.remove(dc.getOldIndex());
                                adapter.notifyItemRemoved(dc.getOldIndex());
                                break;
                        }
                    }

                    binding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onStop() {
        if (reg != null) { reg.remove(); reg = null; }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void onNotificationClick(NotificationsViewModel n, int position) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        // mark read
        db.collection("users").document(uid)
                .collection("notifications").document(n.id)
                .update("read", true);

        // route
        if ("lottery_win".equals(n.type) && n.eventId != null && !n.eventId.isEmpty()) {
            Bundle b = new Bundle();
            b.putString("eventId", n.eventId);
            NavController nav = NavHostFragment.findNavController(this);
            nav.navigate(R.id.navigation_event_details, b);
        }
        // you can add other types here later
    }
}
