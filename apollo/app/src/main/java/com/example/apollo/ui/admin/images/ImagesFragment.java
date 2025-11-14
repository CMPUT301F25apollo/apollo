package com.example.apollo.ui.admin.images;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.apollo.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ImagesFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout imagesContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_images_admin, container, false);

        db = FirebaseFirestore.getInstance();
        imagesContainer = view.findViewById(R.id.imagesContainer);

        loadImages();

        return view;
    }

    private void loadImages() {
        db.collection("events").get()
                .addOnSuccessListener(query -> {
                    imagesContainer.removeAllViews();

                    for (QueryDocumentSnapshot doc : query) {
                        String eventId = doc.getId();
                        String title = doc.getString("title");
                        String url = doc.getString("eventPosterUrl");

                        if (url == null || url.isEmpty()) continue;

                        // Inflate the new vertical card layout
                        View item = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_image_card_admin, imagesContainer, false);

                        ImageView img = item.findViewById(R.id.eventPosterImage);
                        TextView titleTxt = item.findViewById(R.id.eventTitle);
                        ImageView deleteBtn = item.findViewById(R.id.delete_button);

                        titleTxt.setText(title != null ? title : "Untitled Event");
                        Glide.with(this).load(url).into(img);

                        deleteBtn.setOnClickListener(v -> showDeleteDialog(eventId, item));

                        imagesContainer.addView(item);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load images", Toast.LENGTH_SHORT).show()
                );
    }

    private void showDeleteDialog(String eventId, View itemView) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Image")
                .setMessage("Remove this image from the event?")
                .setPositiveButton("Delete", (dialog, which) -> deleteImage(eventId, itemView))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImage(String eventId, View itemView) {
        db.collection("events").document(eventId)
                .update("eventPosterUrl", "")
                .addOnSuccessListener(a -> {
                    imagesContainer.removeView(itemView);
                    Toast.makeText(getContext(), "Image removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to remove image", Toast.LENGTH_SHORT).show()
                );
    }
}
