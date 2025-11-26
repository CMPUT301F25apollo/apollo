package com.example.apollo.ui.entrant.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ProfileViewModel.java
 *
 * Purpose:
 * Serves as the ViewModel for the ProfileFragment. Provides a LiveData<String> object
 * that can be observed by the UI to display dynamic text or other data.
 *
 * Design Pattern:
 * Implements the ViewModel in the MVVM (Model-View-ViewModel) pattern to separate
 * UI logic from data handling and allow lifecycle-aware updates.
 *
 * Outstanding Issues / TODOs:
 * - Currently only provides static text. Extend this ViewModel to hold and update
 *   real user profile data from Firebase or another data source.
 */
public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    /**
     * Constructor initializes the LiveData with default text.
     */
    public ProfileViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Profile fragment");
    }

    /**
     * Returns the LiveData<String> object that can be observed by the UI.
     *
     * @return LiveData containing the current text for the profile fragment
     */
    public LiveData<String> getText() {
        return mText;
    }
}
