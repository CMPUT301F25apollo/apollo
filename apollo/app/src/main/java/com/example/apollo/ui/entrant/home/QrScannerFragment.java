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

// ML Kit
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrScannerFragment extends Fragment {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private PreviewView previewView;
    private TextView overlayText;

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private boolean hasScanned = false;

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

        overlayText = view.findViewById(R.id.scanOverlayText);

        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();

        barcodeScanner = BarcodeScanning.getClient(options);


        // Permission
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

        hasScanned = false; // Reset scanning each time camera restarts

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {

                ProcessCameraProvider provider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis analysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                provider.unbindAll();
                provider.bindToLifecycle(this, selector, preview, analysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("QrScannerFragment", "Camera error", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @ExperimentalGetImage
    private void analyzeImage(@NonNull ImageProxy imageProxy) {

        if (hasScanned) {
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

                    if (barcodes == null || barcodes.isEmpty())
                        return;

                    Barcode code = barcodes.get(0);
                    String eventId = code.getRawValue();

                    if (eventId != null && !hasScanned) {
                        hasScanned = true;
                        showResultDialog(eventId);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("QrScannerFragment", "Scan failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void showResultDialog(String eventId) {

        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("QR Code Detected")
                .setMessage("Open this event?")
                .setPositiveButton("Open", (dialog, which) -> {

                    Bundle args = new Bundle();
                    args.putString("eventId", eventId);

                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_qrScannerFragment_to_navigation_event_details, args);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    hasScanned = false;
                    startCamera();   // 🔥 restart scanning
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] perms,
                                           @NonNull int[] results) {

        if (requestCode == REQUEST_CAMERA_PERMISSION &&
                results.length > 0 &&
                results[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            NavHostFragment.findNavController(this).navigateUp();
        }
    }
}
