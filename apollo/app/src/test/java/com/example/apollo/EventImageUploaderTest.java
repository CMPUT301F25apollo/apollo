package com.example.apollo;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import android.net.Uri;

import com.example.apollo.ui.organizer.events.EventImageUploader;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.OnSuccessListener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EventImageUploaderTest {

    @Mock private FirebaseStorage mockStorage;
    @Mock private FirebaseFirestore mockFirestore;
    @Mock private StorageReference mockRootRef;
    @Mock private StorageReference mockPosterRef;
    @Mock private UploadTask mockUploadTask;
    @Mock private CollectionReference mockEventsCollection;
    @Mock private DocumentReference mockEventDoc;

    private EventImageUploader uploader;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockStorage.getReference()).thenReturn(mockRootRef);
        when(mockRootRef.child(anyString())).thenReturn(mockPosterRef);
        when(mockFirestore.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(anyString())).thenReturn(mockEventDoc);
        uploader = new EventImageUploader(mockStorage, mockFirestore);
    }

    @Test
    public void uploadPoster_success_updatesFirestore() {
        Uri mockUri = mock(Uri.class);
        when(mockUri.toString()).thenReturn("file://image.jpg");

        when(mockPosterRef.putFile(mockUri)).thenReturn(mockUploadTask);
        when(mockUploadTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            OnSuccessListener<UploadTask.TaskSnapshot> listener =
                    (OnSuccessListener<UploadTask.TaskSnapshot>) invocation.getArgument(0);
            listener.onSuccess(mock(UploadTask.TaskSnapshot.class));
            return mockUploadTask;
        });
        when(mockPosterRef.getDownloadUrl()).thenReturn(Tasks.forResult(Uri.parse("https://example.com/image.jpg")));
        when(mockEventDoc.update(eq("eventPosterUrl"), anyString())).thenReturn(Tasks.forResult(null));

        uploader.uploadPoster("E123", mockUri,
                url -> System.out.println("Success URL: " + url),
                e -> fail("Should not fail"));

        verify(mockPosterRef).putFile(mockUri);
        verify(mockEventDoc).update(eq("eventPosterUrl"), anyString());
    }
}
