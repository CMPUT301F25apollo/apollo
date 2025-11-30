package com.example.apollo.ui.entrant.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;

// ML Kit imports (NOTE: Barcode is in .barcode.common)
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrScannerFragment extends Fragment {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private PreviewView previewView;
    private ImageButton backButton;
    private TextView overlayText;

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private boolean hasScannedResult = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.previewView);
        backButton = view.findViewById(R.id.back_button);
        overlayText = view.findViewById(R.id.scanOverlayText);
        hasScannedResult = false;

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Only scan QR codes
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        backButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );

        // Check camera permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );
            } catch (ExecutionException | InterruptedException e) {
                Log.e("QrScannerFragment", "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @ExperimentalGetImage
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (hasScannedResult) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes == null || barcodes.isEmpty()) return;

                    Barcode barcode = barcodes.get(0);
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null && !hasScannedResult) {
                        rawValue = rawValue.trim();                       // 👈 remove any spaces/newlines
                        Log.d("QrScannerFragment", "QR raw value = " + rawValue);
                        hasScannedResult = true;
                        showResultDialog(rawValue);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("QrScannerFragment", "Barcode scanning failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void showResultDialog(String eventQrValue) {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("QR Code detected")
                .setMessage("Open this event?")
                .setPositiveButton("OK", (dialog, which) -> {

                    // 1️⃣ Query Firestore: find event where eventQR == scanned QR code
                    FirebaseFirestore.getInstance()
                            .collection("events")
                            .whereEqualTo("eventQR", eventQrValue)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.isEmpty()) {
                                    Toast.makeText(getContext(),
                                            "No event found for this QR code",
                                            Toast.LENGTH_SHORT).show();

                                    hasScannedResult = false; // allow scanning again
                                    return;
                                }

                                // 2️⃣ Get the REAL Firestore document ID
                                String realEventId = snapshot.getDocuments().get(0).getId();

                                // 3️⃣ Pass that eventId to EventDetailsFragment
                                Bundle args = new Bundle();
                                args.putString("eventId", realEventId);

                                NavHostFragment.findNavController(this)
                                        .navigate(R.id.action_qrScannerFragment_to_navigation_event_details, args);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(),
                                        "Error checking event QR",
                                        Toast.LENGTH_SHORT).show();
                                hasScannedResult = false;
                            });

                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    hasScannedResult = false;
                    // restart camera + analyzer
                    startCamera();
                })
                .setCancelable(false)
                .show();
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                // No permission → go back
                NavHostFragment.findNavController(this).navigateUp();
            }
        }
    }
}

