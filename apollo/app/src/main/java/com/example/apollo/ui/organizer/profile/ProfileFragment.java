package com.example.apollo.ui.organizer.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.apollo.ui.login.LoginActivity;
import com.example.apollo.R;
import com.example.apollo.databinding.FragmentProfileOrganizerBinding;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ProfileFragment.java (Organizer)
 *
 * Purpose:
 * Displays the organizer's profile interface and allows the organizer to log out.
 * Logging out clears the activity stack and redirects the user to the LoginActivity.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern by mediating between the view (binding UI elements)
 * and the model (Firebase Authentication for user logout).
 *
 * Outstanding Issues / TODOs:
 * - Consider adding organizer-specific profile data display (name, email, etc.).
 * - Optionally add a confirmation dialog before logging out.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileOrganizerBinding binding;

    /**
     * Inflates the fragment layout using View Binding and sets up the logout button click listener.
     *
     * @param inflater LayoutInflater used to inflate the fragment's view.
     * @param container The parent ViewGroup the fragment's UI should attach to.
     * @param savedInstanceState Bundle containing saved state, if any.
     * @return The root view of the fragment's layout.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileOrganizerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        return root;
    }

    /**
     * Called when the view is being destroyed. Cleans up the binding reference to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
