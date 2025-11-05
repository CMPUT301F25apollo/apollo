package com.example.apollo.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.example.apollo.databinding.FragmentHomeBinding;
import com.example.apollo.databinding.FragmentOrganizerEventsBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        loadEvents();

        return root;
    }

    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    LinearLayout container = binding.eventsContainer;
                    container.removeAllViews();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String eventId = document.getId();
                        String title = document.getString("title");
                        String description = document.getString("description");
                        String location = document.getString("location");
                        String time = document.getString("time");
                        String date = document.getString("date");

                        View card = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_event_card, container, false);

                        TextView titleView = card.findViewById(R.id.eventTitle);

                        titleView.setText(title);

                        card.setOnClickListener(v -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("eventId", eventId);

                            NavController navController = NavHostFragment.findNavController(this);
                            navController.navigate(R.id.action_navigation_home_to_navigation_event_details, bundle);
                        });

                        container.addView(card);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading events", e));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
