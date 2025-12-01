package com.example.apollo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.Switch;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.entrant.notifications.NotificationsAdapter;
import com.example.apollo.ui.entrant.notifications.NotificationsFragment;
import com.example.apollo.ui.entrant.notifications.NotificationsViewModel;
import com.example.apollo.ui.entrant.profile.SettingsFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationsFeatureTest
 *  US 01.04.01
 *  US 01.04.02
 *  US 01.04.03
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotificationsFeatureTest {

    // US 01.04.01
    // US 01.04.02
    @Test
    public void notificationsScreen_hasRecyclerAndEmptyState() {
        FragmentScenario<NotificationsFragment> scenario =
                FragmentScenario.launchInContainer(NotificationsFragment.class);

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            RecyclerView recycler = root.findViewById(R.id.recycler);
            View empty = root.findViewById(R.id.empty);

            assertNotNull(recycler);
            assertNotNull(empty);

            // no notifications
            assertEquals(View.VISIBLE, empty.getVisibility());
        });
    }

    // Adapter should hold both "win" and "lose" notifications
    @Test
    public void notificationsAdapter_canHoldWinAndLoseNotifications() {
        NotificationsAdapter adapter =
                new NotificationsAdapter((notification, position) -> {});

        List<NotificationsViewModel> items = new ArrayList<>();

        // win notification
        NotificationsViewModel win = new NotificationsViewModel();
        win.id = "n1";
        win.type = "lottery_win";
        win.title = "You were selected!";
        items.add(win);

        // lose notification
        NotificationsViewModel lose = new NotificationsViewModel();
        lose.id = "n2";
        lose.type = "lottery_lose";
        lose.title = "You were not selected";
        items.add(lose);

        adapter.setData(items);

        assertEquals(2, adapter.getItemCount());
    }

    // Settings screen should show a notification
    @Test
    public void settingsScreen_hasNotificationToggle() {
        FragmentScenario<SettingsFragment> scenario =
                FragmentScenario.launchInContainer(SettingsFragment.class);

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            Switch notifSwitch = root.findViewById(R.id.switchNotifications);
            assertNotNull(notifSwitch);
            assertTrue(notifSwitch.isClickable());
        });
    }

    @Test
    public void settingsNotificationSwitch_canToggleOnOff_withoutCallingFirebase() {
        FragmentScenario<SettingsFragment> scenario =
                FragmentScenario.launchInContainer(SettingsFragment.class);

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);

            Switch notifSwitch = root.findViewById(R.id.switchNotifications);
            assertNotNull(notifSwitch);

            // remove real listener so no backend is called in this test
            notifSwitch.setOnCheckedChangeListener(null);

            boolean initial = notifSwitch.isChecked();


            notifSwitch.performClick();
            assertTrue(notifSwitch.isChecked() != initial);


            notifSwitch.performClick();
            assertEquals(initial, notifSwitch.isChecked());
        });
    }
}
