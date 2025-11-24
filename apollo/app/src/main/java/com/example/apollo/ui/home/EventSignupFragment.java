package com.example.apollo.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.apollo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EventSignupFragment extends Fragment {

    private String eventId;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_signup, container, false);

        db = FirebaseFirestore.getInstance();

        TextView eventTitle = view.findViewById(R.id.textEventTitle);
        Button joinBtn = view.findViewById(R.id.buttonJoinWaitlist);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            String title = getArguments().getString("eventTitle");
            eventTitle.setText(title);
        }

        joinBtn.setOnClickListener(v -> joinEvent());

        return view;
    }

    private void joinEvent() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (eventId == null || uid == null) {
            Toast.makeText(getContext(), "Error: Missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference waitRef = db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("state", "waiting");
        data.put("joinedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        data.put("uid", uid);

        waitRef.set(data)
                .addOnSuccessListener(a ->
                        Toast.makeText(getContext(), "You joined the waitlist!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}