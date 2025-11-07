package com.example.apollo.ui.notifications;

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

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration reg;
    private NotificationsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Recycler
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Non-clickable for today: pass null
        adapter = new NotificationsAdapter(null);
        binding.recycler.setAdapter(adapter);

        // Show empty until data arrives
        binding.empty.setVisibility(View.VISIBLE);

        // IMPORTANT: no Firestore listener here
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        // Single listener that rebuilds the list each time (no diff math)
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
}
