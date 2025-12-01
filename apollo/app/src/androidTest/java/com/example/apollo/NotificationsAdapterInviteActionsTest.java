package com.example.apollo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.apollo.R;
import com.example.apollo.ui.entrant.notifications.NotificationsAdapter;
import com.example.apollo.ui.entrant.notifications.NotificationsViewModel;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NotificationsAdapterInviteActionsTest
 *  US 01.05.01
 *  US 01.05.02
 *  US 01.05.03
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotificationsAdapterInviteActionsTest {

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * Helper to:
     * - mount the adapter on a RecyclerView
     * - bind exactly one notification
     * - force layout
     * - return the first row view so I can inspect its buttons
     */
    private View bindSingleItem(NotificationsAdapter adapter, NotificationsViewModel item) {
        Context ctx = getContext();
        java.util.concurrent.atomic.AtomicReference<View> rowRef =
                new java.util.concurrent.atomic.AtomicReference<>();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            RecyclerView recyclerView = new RecyclerView(ctx);
            recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
            recyclerView.setItemAnimator(null); // no animations in tests
            recyclerView.setAdapter(adapter);

            adapter.setData(Collections.singletonList(item));

            recyclerView.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.AT_MOST)
            );
            recyclerView.layout(0, 0, 1080, 1920);

            rowRef.set(recyclerView.getChildAt(0));
        });

        return rowRef.get();
    }

    /**
     * US 01.05.02 + US 01.05.03
     * If I get a lottery win:
     * - I should see Accept and Decline buttons.
     * - Tapping them should trigger the listener callbacks.
     */
    @Test
    public void lotteryWin_showsAcceptAndDeclineButtons_andTriggersCallbacks() {
        AtomicBoolean acceptCalled = new AtomicBoolean(false);
        AtomicBoolean declineCalled = new AtomicBoolean(false);

        NotificationsAdapter adapter = new NotificationsAdapter(
                new NotificationsAdapter.OnNotificationAction() {
                    @Override
                    public void onAccept(NotificationsViewModel n, int position) {
                        acceptCalled.set(true);
                    }

                    @Override
                    public void onDecline(NotificationsViewModel n, int position) {
                        declineCalled.set(true);
                    }
                });

        NotificationsViewModel win = new NotificationsViewModel();
        win.id = "notif1";
        win.type = "lottery_win";
        win.title = "You were selected!";
        win.message = "Tap to accept or decline.";
        win.status = null;

        View row = bindSingleItem(adapter, win);

        Button acceptBtn = row.findViewById(R.id.btnAccept);
        Button declineBtn = row.findViewById(R.id.btnDecline);

        // Buttons are visible + enabled
        assertEquals(View.VISIBLE, acceptBtn.getVisibility());
        assertEquals(View.VISIBLE, declineBtn.getVisibility());
        assertTrue(acceptBtn.isEnabled());
        assertTrue(declineBtn.isEnabled());


        acceptBtn.performClick();
        assertTrue("onAccept should be called", acceptCalled.get());

        declineBtn.performClick();
        assertTrue("onDecline should be called", declineCalled.get());
    }

    /**
     * Non-win notifications should not show any Accept/Decline buttons.
     */
    @Test
    public void nonWinNotification_hidesAcceptAndDeclineButtons() {
        NotificationsAdapter adapter = new NotificationsAdapter(
                new NotificationsAdapter.OnNotificationAction() {
                    @Override public void onAccept(NotificationsViewModel n, int position) {}
                    @Override public void onDecline(NotificationsViewModel n, int position) {}
                });

        NotificationsViewModel lose = new NotificationsViewModel();
        lose.id = "notif2";
        lose.type = "lottery_loss";
        lose.title = "Not selected";
        lose.message = "Better luck next time.";
        lose.status = null;

        View row = bindSingleItem(adapter, lose);

        Button acceptBtn = row.findViewById(R.id.btnAccept);
        Button declineBtn = row.findViewById(R.id.btnDecline);

        // No actions for non-win notifications
        assertEquals(View.GONE, acceptBtn.getVisibility());
        assertEquals(View.GONE, declineBtn.getVisibility());
    }

    /**
     * Once I’ve already accepted or declined, the row should be “locked”
     * where buttons disabled
     * and row visually dimmed
     */
    @Test
    public void alreadyAcceptedOrDeclined_disablesButtonsAndDimsRow() {
        NotificationsAdapter adapter = new NotificationsAdapter(
                new NotificationsAdapter.OnNotificationAction() {
                    @Override public void onAccept(NotificationsViewModel n, int position) {}
                    @Override public void onDecline(NotificationsViewModel n, int position) {}
                });

        NotificationsViewModel alreadyAccepted = new NotificationsViewModel();
        alreadyAccepted.id = "notif3";
        alreadyAccepted.type = "lottery_win";
        alreadyAccepted.title = "You were selected!";
        alreadyAccepted.message = "Already accepted.";
        alreadyAccepted.status = "accepted";

        View row = bindSingleItem(adapter, alreadyAccepted);

        Button acceptBtn = row.findViewById(R.id.btnAccept);
        Button declineBtn = row.findViewById(R.id.btnDecline);

        // Can’t press anything again
        assertFalse(acceptBtn.isEnabled());
        assertFalse(declineBtn.isEnabled());

        // Row should look done
        float alpha = row.getAlpha();
        assertTrue("Row should be dimmed when already answered", alpha <= 0.6f);
    }
}
