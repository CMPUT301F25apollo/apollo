package com.example.apollo.ui.admin.images;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.apollo.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ImagesFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout imagesContainer;
    private EditText searchInput;

    // Stores every image event for filtering
    private final List<ImageItem> allImages = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_images_admin, container, false);

        db = FirebaseFirestore.getInstance();
        imagesContainer = view.findViewById(R.id.imagesContainer);
        searchInput = view.findViewById(R.id.search_images_input);

        loadImagesFromFirestore();

        // 🔎 live filtering as user types — matching EventsFragment
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterImages(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    // ========================= LOAD IMAGES FIRESTORE ==========================
    private void loadImagesFromFirestore() {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allImages.clear();

                    for (QueryDocumentSnapshot document : querySnapshot) {

                        String eventId = document.getId();
                        String title = document.getString("title");
                        String posterUrl = document.getString("eventPosterUrl");

                        // Only store events that have valid images
                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            allImages.add(new ImageItem(eventId, title, posterUrl));
                            filterImages(searchInput.getText().toString()); // refresh UI live
                        }
                    }

                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "❌ Error loading images", e));
    }


    // ========================= FILTER + DISPLAY  ==============================
    private void filterImages(String query) {
        imagesContainer.removeAllViews();
        String lowerQuery = query.toLowerCase();

        for (ImageItem item : allImages) {
            if (item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerQuery)) {

                View card = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_image_card_admin, imagesContainer, false);

                ImageView posterView = card.findViewById(R.id.eventPosterImage);
                TextView titleView = card.findViewById(R.id.eventTitle);
                ImageView deleteButton = card.findViewById(R.id.delete_button);

                titleView.setText(item.getTitle());

                Glide.with(getContext())
                        .load(item.getPosterUrl())
                        .into(posterView);

                deleteButton.setOnClickListener(v ->
                        showDeleteDialog(item.getId(), card, item.getTitle()));

                imagesContainer.addView(card);
            }
        }
    }

    // ========================= DATA MODEL ==========================
    private static class ImageItem {
        private final String id;
        private final String title;
        private final String posterUrl;

        public ImageItem(String id, String title, String posterUrl) {
            this.id = id;
            this.title = title;
            this.posterUrl = posterUrl;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getPosterUrl() { return posterUrl; }
    }


    // ========================= DELETE IMAGE ==========================
    private void showDeleteDialog(String eventId, View card, String eventTitle) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Poster")
                .setMessage("Delete image for \"" + eventTitle + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteImage(eventId, card))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImage(String eventId, View card) {
        db.collection("events").document(eventId)
                .update("eventPosterUrl", "")
                .addOnSuccessListener(aVoid -> {
                    imagesContainer.removeView(card);
                    Toast.makeText(getContext(), "Image removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to delete image", Toast.LENGTH_SHORT).show()
                );
    }
}
