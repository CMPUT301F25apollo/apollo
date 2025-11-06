package com.example.apollo.ui.organizer.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ProfileViewModel.java (Organizer)
 *
 * Purpose:
 * Acts as the ViewModel for the Organizer's ProfileFragment.
 * It provides reactive data to the UI using LiveData, supporting the MVVM design pattern.
 *
 * Design Pattern:
 * - Implements the ViewModel role in the MVVM (Model-View-ViewModel) architecture.
 * - Separates UI logic from data handling, allowing the UI to observe changes to LiveData.
 *
 * Outstanding Issues / TODOs:
 * - Future enhancement: Fetch real organizer profile data from Firestore or another data source.
 * - Consider adding LiveData fields for organizer name, email, and event statistics.
 */
public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    /**
     * Initializes the ViewModel with default text for the Organizer Profile fragment.
     */
    public ProfileViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is an Organizer Profile fragment");
    }

    /**
     * Returns the text LiveData object so the UI can observe and display its value.
     *
     * @return a LiveData<String> representing the text for the Organizer Profile fragment.
     */
    public LiveData<String> getText() {
        return mText;
    }
}
