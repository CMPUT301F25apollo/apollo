package com.example.apollo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.apollo.databinding.ActivityAdminBinding;
import com.example.apollo.databinding.ActivityOrganiserBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * AdminActivity.java
 *
 * Purpose:
 * This activity serves as the main entry point for admins in the Apollo app.
 * It manages navigation between admin-related fragments such as events, profiles,
 * notifications, and images using a bottom navigation bar.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern — connects admin fragments (views)
 * to underlying app logic and manages navigation events.
 *
 * Outstanding Issues / TODOs:
 * - Implement state restoration when configuration changes occur.
 * - Add admin-specific logic such as dashboard analytics or user moderation.
 */
public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate layout via View Binding
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize bottom navigation
        BottomNavigationView navViewAdmin = binding.navViewAdmin;

        // Define top-level destinations for the admin navigation
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_events,
                R.id.navigation_profiles,
                R.id.navigation_notifications,
                R.id.navigation_images
        ).build();

        // Find the NavController for the Admin Navigation Host
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_admin);

        // Hook up the ActionBar and BottomNavigationView with the NavController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navViewAdmin, navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_admin);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
