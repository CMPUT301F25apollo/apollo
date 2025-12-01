package com.example.apollo;

import static org.junit.Assert.assertNotNull;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.apollo.ui.entrant.home.QrScannerFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

/**
 * QrScannerFeatureTest
 *  US 01.06.01
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class QrScannerFeatureTest {

    // Make sure the QR scan result without crashing.
    @Test
    public void qrScan_showResultDialogDoesNotCrash() {
        FragmentScenario<QrScannerFragment> scenario =
                FragmentScenario.launchInContainer(
                        QrScannerFragment.class,
                        new Bundle(),
                        R.style.Theme_Apollo
                );

        scenario.onFragment(fragment -> {
            assertNotNull(fragment);

            try {
                // Call the private showResultDialog method
                Method m = QrScannerFragment.class
                        .getDeclaredMethod("showResultDialog", String.class);
                m.setAccessible(true);

                // Simulate a scanned QR code value
                m.invoke(fragment, "dummyQrValue123");

            } catch (Exception e) {
                throw new RuntimeException(
                        "showResultDialog invocation failed – QR flow is broken.", e);
            }
        });
    }
}
