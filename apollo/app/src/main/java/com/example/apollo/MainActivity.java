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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            setupNavController(navController, new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_notifications, R.id.navigation_profile).build());
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
                        setupNavController(navController, new AppBarConfiguration.Builder(R.id.navigation_organizer_events, R.id.navigation_organizer_add_event, R.id.navigation_profile).build());
                    } else {
                        // Entrant UI
                        navView.getMenu().clear();
                        navView.inflateMenu(R.menu.bottom_nav_menu);
                        navController.setGraph(R.navigation.mobile_navigation);
                        setupNavController(navController, new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_notifications, R.id.navigation_profile).build());
                    }
                }
            });
        } else {
            // Default to guest if no user and not explicitly guest
            navView.getMenu().clear();
            navView.inflateMenu(R.menu.bottom_nav_menu);
            navController.setGraph(R.navigation.mobile_navigation);
            navView.getMenu().findItem(R.id.navigation_profile).setVisible(true);
            setupNavController(navController, new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_notifications, R.id.navigation_profile).build());
        }
    }

    private void setupNavController(NavController navController, AppBarConfiguration appBarConfiguration) {
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
