package com.example.apollo.ui.home;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;


import com.example.apollo.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class HomeFragment extends Fragment {


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);


        //grab existing buttons
        Button event1 = view.findViewById(R.id.button_event1);
        Button event2 = view.findViewById(R.id.button_event2);
        Button event3 = view.findViewById(R.id.button_event3);
        Button event4 = view.findViewById(R.id.button_event4);
        Button[] buttons = new Button[]{event1, event2, event3, event4};


        //show loading state
        for (Button b : buttons) {
            b.setEnabled(false);
            b.setText("Loading…");
        }


        // first few events from firestore and fill the buttons
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events")
                .get()
                .addOnSuccessListener(snaps -> {
                    int i = 0;
                    for (DocumentSnapshot d : snaps) {
                        if (i >= buttons.length) break;

