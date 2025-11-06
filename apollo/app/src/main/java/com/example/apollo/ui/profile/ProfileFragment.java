package com.example.apollo.ui.profile;

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

public class ProfileFragment extends Fragment {

    private TextView tvName;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Button buttonLogin;
    private Group profileGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

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
                            .navigate(R.id.navigation_settings)
            );

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
