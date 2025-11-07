package com.example.apollo.ui.organizer.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.apollo.R;
import com.example.apollo.databinding.FragmentEditProfileOrganizerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileOrganizerBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileOrganizerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadUserProfile();
        }

        binding.saveButton.setOnClickListener(v -> saveUserProfile());

        binding.cancelButton.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        return root;
    }

    private void loadUserProfile() {
        DocumentReference docRef = db.collection("Organizers").document(currentUser.getUid());
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                binding.editName.setText(documentSnapshot.getString("name"));
                binding.editUsername.setText(documentSnapshot.getString("username"));
                binding.editEmail.setText(documentSnapshot.getString("email"));
                binding.editPhone.setText(documentSnapshot.getString("phone"));
            }
        });
    }

    private void saveUserProfile() {
        String name = binding.editName.getText().toString().trim();
        String username = binding.editUsername.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String phone = binding.editPhone.getText().toString().trim();

        if (name.isEmpty() || username.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("username", username);
        user.put("email", email);
        user.put("phone", phone);

        db.collection("Organizers").document(currentUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(binding.getRoot()).navigateUp();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating profile", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
