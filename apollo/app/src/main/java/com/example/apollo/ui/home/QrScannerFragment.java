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
 * Fragment responsible for handling live QR code scanning using CameraX and ML Kit.
 * <p>
 * When a QR code containing an event ID is scanned (formatted as {@code eventId:<id>}),
 * the fragment verifies the event's existence in Firestore and navigates to
 * the event details screen.
 */
public class QrScannerFragment extends Fragment {

    /** Displays the live camera feed for scanning. */
    private PreviewView previewView;

    /** Firestore instance used to verify scanned event IDs. */
    private FirebaseFirestore db;

    /** Flag indicating whether a frame is currently being processed to avoid overlap. */
    private boolean isProcessing = false;

    /**
     * Activity result launcher for requesting the CAMERA permission at runtime.
     * <p>
     * If permission is granted, the camera starts immediately. Otherwise, an error is logged.
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCamera();
                else Log.e("QR", "Camera permission denied");
            });

    /**
     * Inflates the fragment layout, initializes Firestore, checks for camera permission,
     * and sets up the back button behavior.
     *
     * @param inflater  LayoutInflater used to inflate the layout
     * @param container Optional parent view container
     * @param savedInstanceState previously saved instance state, if any
     * @return the inflated view hierarchy for this fragment
     */
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

        // Back button returns user to the previous fragment (Home)
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
        });

        return view;
    }

    /**
     * Starts the CameraX provider asynchronously and binds it once available.
     * <p>
     * Handles any exceptions that occur during camera initialization.
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
     * Binds the camera lifecycle to this fragment and sets up a real-time analyzer
     * that uses ML Kit to detect barcodes from the preview frames.
     *
     * @param cameraProvider CameraX provider that manages the camera lifecycle
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
     * Processes a successfully scanned QR code string. If it begins with {@code eventId:},
     * the corresponding event document is fetched from Firestore. If found, the user
     * is navigated to the event details screen.
     *
     * @param rawValue the decoded text value from the scanned QR code
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
