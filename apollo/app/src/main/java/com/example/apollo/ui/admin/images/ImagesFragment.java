/**
 * ImagesFragment.java
 *
 * This fragment displays all event poster images for admin users.
 * It loads event images from Firestore, shows them as cards, supports live
 * search filtering, and allows admins to remove poster images from events.
 *
 * Only events with valid poster URLs are shown.
 */
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

/**
 * Fragment that displays all event poster images for admins.
 * Events with valid poster URLs are shown as cards with title, image preview,
 * and a delete option. Supports live text search for quick filtering.
 */
public class ImagesFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout imagesContainer;
    private EditText searchInput;

    /** Stores all poster items for search and filtering. */
    private final List<ImageItem> allImages = new ArrayList<>();

    /**
     * Inflates the layout, initializes UI components, and begins loading poster images.
     * Also sets up live filtering as the admin types into the search box.
     *
     * @return the root view for this fragment
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_images_admin, container, false);

        db = FirebaseFirestore.getInstance();
        imagesContainer = view.findViewById(R.id.imagesContainer);
        searchInput = view.findViewById(R.id.search_images_input);

        loadImagesFromFirestore();

        // Live filtering of posters
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterImages(s.toString());
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    /**
     * Loads all events from Firestore and extracts only those with valid
     * poster image URLs. Each valid image is stored in {@code allImages} and
     * the UI is refreshed immediately to reflect new data.
     */
    private void loadImagesFromFirestore() {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allImages.clear();

                    for (QueryDocumentSnapshot document : querySnapshot) {

                        String eventId = document.getId();
                        String title = document.getString("title");
                        String posterUrl = document.getString("eventPosterUrl");

                        // Only add events with posters
                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            allImages.add(new ImageItem(eventId, title, posterUrl));
                            filterImages(searchInput.getText().toString()); // refresh instantly
                        }
                    }

                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error loading images", e));
    }

    /**
     * Filters the list of posters based on the admin's search query and
     * repopulates the container with matching cards.
     *
     * @param query text used to match event titles (case-insensitive)
     */
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

    /**
     * Simple model representing an image entry with an event ID,
     * event title, and poster URL.
     */
    private static class ImageItem {
        private final String id;
        private final String title;
        private final String posterUrl;

        /**
         * Constructs a poster item used in this fragment.
         *
         * @param id        the Firestore event ID
         * @param title     title of the event
         * @param posterUrl URL of the poster image
         */
        public ImageItem(String id, String title, String posterUrl) {
            this.id = id;
            this.title = title;
            this.posterUrl = posterUrl;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getPosterUrl() { return posterUrl; }
    }

    /**
     * Shows a confirm dialog before deleting the poster image.
     *
     * @param eventId    the event whose poster is being removed
     * @param card       UI card to remove after deletion
     * @param eventTitle title of the event, used in the dialog message
     */
    private void showDeleteDialog(String eventId, View card, String eventTitle) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Poster")
                .setMessage("Delete image for \"" + eventTitle + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteImage(eventId, card))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the poster URL field from the event document.
     * On success, the UI card is removed and a toast is shown.
     *
     * @param eventId the event whose poster should be cleared
     * @param card    view to remove from the list
     */
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
