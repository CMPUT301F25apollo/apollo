package com.example.apollo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.apollo.databinding.ActivityOrganiserBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * OrganizerActivity.java
 *
 * Purpose:
 * This activity serves as the main entry point for organizers in the Apollo app.
 * It handles the setup of bottom navigation for navigating between the organizer's
 * events list and profile sections.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern by managing navigation and user interactions
 * between the view (fragments) and any data (model) related to organizer events.
 *
 * Outstanding Issues / TODOs:
 * - Add handling for back button presses if custom behavior is needed.
 * - Consider saving/restoring navigation state during configuration changes.
 */
public class OrganizerActivity extends AppCompatActivity {

    private ActivityOrganiserBinding binding;

    /**
     * Called when the activity is starting. Sets up the view binding, bottom navigation,
     * and connects the NavController to the navigation host fragment.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle). Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout using View Binding
        binding = ActivityOrganiserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up bottom navigation view
        BottomNavigationView navViewOrganiser = findViewById(R.id.nav_view_organiser);

        // Configure AppBar with top-level destinations
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_events, R.id.navigation_profile)
                .build();

        // Find the navigation controller for the nav host fragment
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_organiser);

        // Set up action bar and bottom navigation with the nav controller
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navViewOrganiser, navController);
    }
}
