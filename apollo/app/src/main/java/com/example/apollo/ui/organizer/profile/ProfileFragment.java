package com.example.apollo.ui.organizer.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.apollo.R;
import com.example.apollo.ui.login.LoginActivity;
import com.example.apollo.databinding.FragmentProfileOrganizerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FragmentProfileOrganizerBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileOrganizerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadUserProfile();
        }

        binding.logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        binding.editProfileButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_navigation_profile_to_editProfileFragment);
        });

        return root;
    }

    private void loadUserProfile() {
        DocumentReference docRef = db.collection("Organizers").document(currentUser.getUid());
        docRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                // Handle error
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                binding.profileName.setText(snapshot.getString("name"));
                binding.profileUsername.setText(snapshot.getString("username"));
                binding.profileEmail.setText(snapshot.getString("email"));
                binding.profilePhone.setText(snapshot.getString("phone"));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
