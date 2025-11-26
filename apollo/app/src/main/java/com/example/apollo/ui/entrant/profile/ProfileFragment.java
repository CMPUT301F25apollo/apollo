package com.example.apollo.ui.entrant.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.ui.login.LoginActivity;
import com.example.apollo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * ProfileFragment.java
 *
 * Purpose:
 * Displays the user's profile information and provides options to navigate to settings
 * or log out. Handles both guest users and registered users by dynamically showing
 * the appropriate UI elements.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern by mediating between the profile view (TextViews,
 * Buttons, Groups) and the model (Firebase Authentication and Firestore user data).
 *
 * Outstanding Issues / TODOs:
 * - Add error handling for Firestore failures.
 * - Improve UI feedback for loading profile data.
 * - Consider caching user data to reduce repeated Firestore calls.
 */
public class ProfileFragment extends Fragment {

    private TextView tvName;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Button buttonLogin;
    private Group profileGroup;

    /**
     * Inflates the fragment's layout.
     *
     * @param inflater The LayoutInflater object used to inflate views.
     * @param container The parent ViewGroup the fragment's UI should attach to.
     * @param savedInstanceState Bundle containing saved state, if any.
     * @return The root View for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    /**
     * Called after the view has been created. Initializes UI components, handles guest
     * vs registered user flows, loads user profile data, and sets up button click listeners.
     *
     * @param v The View returned by onCreateView().
     * @param savedInstanceState Bundle containing saved state, if any.
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        tvName = v.findViewById(R.id.tvName);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        buttonLogin = v.findViewById(R.id.buttonLogin);
        profileGroup = v.findViewById(R.id.profile_group);

        boolean isGuest = getActivity().getIntent().getBooleanExtra("isGuest", false);

        if (isGuest) {
            // Guest user flow
            profileGroup.setVisibility(View.GONE);
            buttonLogin.setVisibility(View.VISIBLE);
            buttonLogin.setOnClickListener(view -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        } else {
            // Registered user flow
            profileGroup.setVisibility(View.VISIBLE);
            buttonLogin.setVisibility(View.GONE);
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String uid = currentUser.getUid();
                DocumentReference userRef = db.collection("users").document(uid);
                userRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String name = document.getString("name");
                            tvName.setText(name);
                        }
                    }
                });
            }

            v.findViewById(R.id.btnSettings).setOnClickListener(view ->
                    NavHostFragment.findNavController(ProfileFragment.this)
                            .navigate(R.id.action_navigation_profile_to_navigation_settings)            );

            Button btnLogout = v.findViewById(R.id.btnLogout);
            btnLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            });
        }
    }
}
