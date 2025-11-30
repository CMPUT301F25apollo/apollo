//package com.example.apollo.ui.entrant.notifications;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.navigation.NavController;
//import androidx.navigation.fragment.NavHostFragment;
//
//import com.example.apollo.R;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.FieldValue;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//import java.util.HashMap;
//
//public class InviteResponseFragment extends Fragment {
//
//    private FirebaseFirestore db;
//    private String eventId;
//    private String uid;
//
//    private TextView textInviteMessage;
//    private Button buttonAccept, buttonDecline;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater,
//                             @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//
//        View view = inflater.inflate(R.layout.fragment_invite_response, container, false);
//
//        db = FirebaseFirestore.getInstance();
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//
//        if (user == null) {
//            Toast.makeText(getContext(), "You must be logged in.", Toast.LENGTH_SHORT).show();
//            return view;
//        }
//
//        uid = user.getUid();
//        textInviteMessage = view.findViewById(R.id.textInviteMessage);
//        buttonAccept = view.findViewById(R.id.buttonAcceptInvite);
//        buttonDecline = view.findViewById(R.id.buttonDeclineInvite);
//
//        if (getArguments() != null) {
//            eventId = getArguments().getString("eventId");
//            Log.d("Invite", "DEBUG eventId loaded: " + eventId);
//            Toast.makeText(getContext(), "DEBUG eventId = " + eventId, Toast.LENGTH_SHORT).show();
//        } else {
//            Log.d("Invite", "DEBUG NO ARGUMENTS");
//            Toast.makeText(getContext(), "DEBUG NO ARGUMENTS", Toast.LENGTH_SHORT).show();
//        }
//
//
//        buttonAccept.setOnClickListener(v -> acceptInvite());
//        buttonDecline.setOnClickListener(v -> declineInvite());
//
//        return view;
//    }
//
//    private void acceptInvite() {
//        if (eventId == null || uid == null) return;
//
//        // 1. Create registration entry
//        DocumentReference regRef = db.collection("events")
//                .document(eventId)
//                .collection("registrations")
//                .document(uid);
//
//        HashMap<String, Object> regData = new HashMap<>();
//        regData.put("registeredAt", FieldValue.serverTimestamp());
//
//        // 2. Delete waitlist entry
//        DocumentReference waitRef = db.collection("events")
//                .document(eventId)
//                .collection("waitlist")
//                .document(uid);
//
//        // 3. Delete invite entry
//        DocumentReference inviteRef = db.collection("events")
//                .document(eventId)
//                .collection("invites")
//                .document(uid);
//
//        regRef.set(regData)
//                .addOnSuccessListener(ok -> {
//                    waitRef.delete();
//                    inviteRef.delete();
//
//                    Toast.makeText(getContext(), "You are now registered!", Toast.LENGTH_SHORT).show();
//                    goBack();
//                })
//                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//    }
//
//    private void declineInvite() {
//        if (eventId == null || uid == null) return;
//
//        // 1. Create DECLINED entry
//        DocumentReference declinedRef = db.collection("events")
//                .document(eventId)
//                .collection("declined")
//                .document(uid);
//
//        HashMap<String, Object> declinedData = new HashMap<>();
//        declinedData.put("state", "declined");
//        declinedData.put("declinedAt", FieldValue.serverTimestamp());
//
//        // 2. References to delete
//        DocumentReference inviteRef = db.collection("events")
//                .document(eventId)
//                .collection("invites")
//                .document(uid);
//
//        DocumentReference waitRef = db.collection("events")
//                .document(eventId)
//                .collection("waitlist")
//                .document(uid);
//
//        // 3. Write declined entry, then delete invite + waitlist
//        declinedRef.set(declinedData)
//                .addOnSuccessListener(ok -> {
//                    Log.d("Invite", "DECLINED WRITE DONE at: events/" + eventId + "/declined/" + uid);
//                    Toast.makeText(getContext(), "Invitation declined.", Toast.LENGTH_SHORT).show();
//                    inviteRef.delete();
//                    waitRef.delete();
//                    goBack();
//                })
//
//                .addOnFailureListener(e -> {
//                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
//
//
//    private void goBack() {
//        NavController navController = NavHostFragment.findNavController(this);
//        navController.popBackStack();
//    }
//}