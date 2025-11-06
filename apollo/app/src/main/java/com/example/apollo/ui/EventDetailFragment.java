package com.example.apollo.ui.home;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class EventDetailsFragment extends Fragment {

    private MaterialButton joinButton;
    private ImageButton backButton;
    private TextView titleView, infoView;


    private FirebaseFirestore db;
    private FirebaseAuth auth;


    private String eventId;
    private String uid;
    private boolean joined = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleView   = view.findViewById(R.id.text_event_name);
        infoView    = view.findViewById(R.id.text_event_info);
        joinButton  = view.findViewById(R.id.button_join_waitlist);
        backButton  = view.findViewById(R.id.back_button);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Bundle args = getArguments();
        String eventTitle        = args != null ? args.getString("eventTitle", "Unknown Event") : "Unknown Event";
        String eventLocation     = args != null ? args.getString("eventLocation", "Unknown Location") : "Unknown Location";
        String eventTime         = args != null ? args.getString("eventTime", "Unknown Time") : "Unknown Time";
        String eventRegistration = args != null ? args.getString("eventRegistration", "N/A") : "N/A";
        eventId                  = args != null ? args.getString("eventId") : null;


        titleView.setText(eventTitle);
        infoView.setText(eventTitle + "\n" + "Location: " + eventLocation + "\n" +  "Time: " + eventTime + "\n" + "Registration between " + eventRegistration);


        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());


        if (eventId == null) {
            toast("Missing eventId");
            joinButton.setEnabled(false);
            return;
        }


        // ensure user exists
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                    .addOnSuccessListener(r -> {
                        uid = auth.getCurrentUser().getUid();
                        initWaitlistState();
                    })
                    .addOnFailureListener(e -> toast("Auth failed: " + e.getMessage()));
        } else {

            uid = auth.getCurrentUser().getUid();
            initWaitlistState();
        }
    }


    private void initWaitlistState() {
        setLoading(true);
        waitlistRef().get()
                .addOnSuccessListener(snap -> {
                    joined = snap.exists();
                    renderButton();
                    setLoading(false);


                    joinButton.setOnClickListener(v -> {
                        setLoading(true);
                        if (joined) {
                            waitlistRef().delete()
                                    .addOnSuccessListener(ok -> {
                                        joined = false;
                                        renderButton();
                                        setLoading(false);
                                        toast("Left waitlist");
                                    })
                                    .addOnFailureListener(e -> {
                                        setLoading(false);
                                        toast("Failed to leave: " + e.getMessage());
                                    });
                        } else {
                            HashMap<String,Object> data = new HashMap<>();
                            data.put("joinedAt", FieldValue.serverTimestamp());
                            waitlistRef().set(data)
                                    .addOnSuccessListener(ok -> {
                                        joined = true;
                                        renderButton();
                                        setLoading(false);
                                        toast("Joined waitlist");
                                    })
                                    .addOnFailureListener(e -> {
                                        setLoading(false);
                                        toast("Failed to join: " + e.getMessage());
                                    });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Check failed: " + e.getMessage());

                });
    }


    private DocumentReference waitlistRef() {
        return db.collection("events").document(eventId)
                .collection("waitlist").document(uid);
    }


    private void renderButton() {
        if (joined) {
            joinButton.setText("LEAVE WAITING LIST");
            joinButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), android.R.color.black));
            joinButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        } else {
            joinButton.setText("JOIN WAITING LIST");
            joinButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.lightblue));
            joinButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        }
        joinButton.setEnabled(true);
    }


    private void setLoading(boolean loading) {
        joinButton.setEnabled(!loading);
        if (loading) joinButton.setText("Please wait…");
    }


    private void toast(String m) {
        if (getContext() != null) Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show();
    }
}
