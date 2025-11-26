package com.example.apollo.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
/**
 * QrScannerFragment.java
 *
 * This fragment provides a live QR code scanner using CameraX and Google ML Kit.
 * When a QR code is detected, the app extracts its value (usually an event ID)
 * and navigates to the event details screen if the event exists in Firestore.
 *
 * Technologies used:
 * - CameraX: Handles camera input and preview lifecycle.
 * - ML Kit Barcode Scanning: Processes camera frames to detect QR codes.
 * - Firestore: Verifies that scanned event IDs exist in the database.
 * - AndroidX Navigation: Manages transitions between the Home and Event Details screens.
 *
 * Design Notes:
 * This fragment follows an MVC-like structure where Firestore acts as the Model (data),
 * the camera preview is the View, and this fragment acts as the Controller.
 * The camera lifecycle is automatically managed using CameraX.
 *
 * Known Issues:
 * - No loading indicator or visual feedback during scanning
 * - Processes only one QR code per frame (skips duplicates)
 * - No error dialog for invalid or unrecognized codes
 */

public class QrScannerFragment extends Fragment {

    private PreviewView previewView;
    private FirebaseFirestore db;
    private boolean isProcessing = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCamera();
                else Log.e("QR", "Camera permission denied");
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_scanner, container, false);
        previewView = view.findViewById(R.id.previewView);

        db = FirebaseFirestore.getInstance();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack(); // goes back to Home
        });

        return view;
    }
    /**
     * Starts the camera asynchronously using CameraX. Once available,
     * binds the preview and image analysis use cases.
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("QR", "Camera start error", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * Binds the camera preview and barcode analysis pipeline to the fragment's lifecycle.
     *
     * @param cameraProvider The CameraX provider managing camera lifecycles.
     */
    private void bindCamera(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        BarcodeScanner scanner = BarcodeScanning.getClient();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), image -> {
            if (isProcessing) {
                image.close();
                return;
            }
            isProcessing = true;

            @SuppressWarnings("UnsafeExperimentalUsageError")
            @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
            InputImage inputImage = InputImage.fromMediaImage(
                    image.getImage(),
                    image.getImageInfo().getRotationDegrees()
            );

            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null) {
                                handleScannedCode(rawValue);
                                break;
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("QR", "Scan failed", e))
                    .addOnCompleteListener(task -> {
                        image.close();
                        isProcessing = false;
                    });
        });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }
    /**
     * Handles a successfully scanned QR code.
     * If the code matches the expected format it checks Firestore
     * for a matching event and navigates to its details page.
     * @param rawValue The decoded string value from the QR code.
     */

    private void handleScannedCode(String rawValue) {
        Log.d("QR", "Scanned: " + rawValue);

        // Example expected QR content: "eventId:abc123"
        if (rawValue.startsWith("eventId:")) {
            String eventId = rawValue.substring(8);

            // (optional) Verify the event exists before navigation
            db.collection("events").document(eventId).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            // Navigate to the OrganizerEventDetailsFragment with that eventId
                            Bundle bundle = new Bundle();
                            bundle.putString("eventId", eventId);
                            NavController navController = NavHostFragment.findNavController(this);
                            navController.navigate(R.id.action_qrScannerFragment_to_navigation_event_details, bundle);
                        } else {
                            Log.w("QR", "No such event found for scanned ID: " + eventId);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("QR", "Firestore check failed", e));
        }
    }
}
