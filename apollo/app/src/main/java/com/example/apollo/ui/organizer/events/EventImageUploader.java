package com.example.apollo.ui.organizer.events;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * Handles uploading event poster images to Firebase Storage
 * and updating the Firestore document with the download URL.
 */
public class EventImageUploader {

    private final FirebaseStorage storage;
    private final FirebaseFirestore firestore;

    public EventImageUploader(FirebaseStorage storage, FirebaseFirestore firestore) {
        this.storage = storage;
        this.firestore = firestore;
    }

    /**
     * Uploads the given image to Firebase Storage and updates the corresponding
     * event document with its download URL.
     *
     * @param eventId  the Firestore event ID
     * @param imageUri the local URI of the image
     * @param onSuccess callback when upload + update succeed
     * @param onFailure callback when any step fails
     */
    public void uploadPoster(String eventId, Uri imageUri,
                             OnSuccessListener<String> onSuccess,
                             OnFailureListener onFailure) {

        String path = "event_posters/" + eventId + ".jpg";
        StorageReference imageRef = storage.getReference().child(path);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    firestore.collection("events")
                                            .document(eventId)
                                            .update("eventPosterUrl", uri.toString())
                                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(uri.toString()))
                                            .addOnFailureListener(onFailure);
                                })
                                .addOnFailureListener(onFailure))
                .addOnFailureListener(onFailure);
    }
}
