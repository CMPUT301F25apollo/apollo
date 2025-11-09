package com.example.apollo;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.apollo.databinding.ActivityMainBinding;

/**
 * MainActivity.java
 *
 * Purpose:
 * This activity serves as the entry point for the Apollo app for both guests and registered users.
 * It dynamically sets up the bottom navigation and navigation graphs based on the user's role
 * (entrant, organizer, or guest).
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern by managing user navigation and dynamically configuring
 * the view based on the authenticated user's role.
 *
 * Outstanding Issues / TODOs:
 * - Add error handling for Firestore get() failures.
 * - Optimize navigation setup to avoid multiple repeated calls to inflateMenu().
 * - Consider storing role information locally to reduce repeated network requests.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    /**
     * Called when the activity is starting. Sets up the view binding, determines the user type
     * (guest, entrant, or organizer), and configures the bottom navigation and navController
     * accordingly.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle). Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate layout using View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isGuest = getIntent().getBooleanExtra("isGuest", false);

        if (isGuest) {
            // Guest user flow
            navView.getMenu().clear();
            navView.inflateMenu(R.menu.bottom_nav_menu);
            navController.setGraph(R.navigation.mobile_navigation);
            navView.getMenu().findItem(R.id.navigation_profile).setVisible(true);
            setupNavController(navController, new AppBarConfiguration.Builder(
                    R.id.navigation_home, R.id.navigation_notifications, R.id.navigation_profile).build());
        } else if (currentUser != null) {
            // Registered user flow
            DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String role = documentSnapshot.getString("role");
                    if ("organizer".equals(role)) {
                        // Organizer UI
                        navView.getMenu().clear();
                        navView.inflateMenu(R.menu.bottom_nav_organizer_menu);
                        navController.setGraph(R.navigation.organizer_mobile_navigation);
                        setupNavController(navController, new AppBarConfiguration.Builder(
                                R.id.navigation_organizer_events, R.id.navigation_notifications, R.id.navigation_profile).build());
                    } else {
                        // Entrant UI
                        navView.getMenu().clear();
                        navView.inflateMenu(R.menu.bottom_nav_menu);
                        navController.setGraph(R.navigation.mobile_navigation);
                        setupNavController(navController, new AppBarConfiguration.Builder(
                                R.id.navigation_home, R.id.navigation_notifications, R.id.navigation_profile).build());
                    }
                }
            });
        } else {
            // Default to guest if no user and not explicitly guest
            navView.getMenu().clear();
            navView.inflateMenu(R.menu.bottom_nav_menu);
            navController.setGraph(R.navigation.mobile_navigation);
            navView.getMenu().findItem(R.id.navigation_profile).setVisible(true);
            setupNavController(navController, new AppBarConfiguration.Builder(
                    R.id.navigation_home, R.id.navigation_notifications, R.id.navigation_profile).build());
        }
    }

    /**
     * Helper method to set up the NavController with ActionBar and BottomNavigationView.
     *
     * @param navController The NavController for managing fragment navigation.
     * @param appBarConfiguration Configuration specifying top-level destinations for the ActionBar.
     */
    private void setupNavController(NavController navController, AppBarConfiguration appBarConfiguration) {
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    /**
     * Handles navigation when the up button in the ActionBar is pressed.
     *
     * @return true if navigation was successful, otherwise false
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
