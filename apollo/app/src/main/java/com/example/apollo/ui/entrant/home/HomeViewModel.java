package com.example.apollo.ui.entrant.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * HomeViewModel.java
 *
 * A simple ViewModel used to provide text data for the HomeFragment.
 * This class holds a piece of observable UI state and exposes it as LiveData.
 * It follows the MVVM pattern by separating data from the UI controller.
 */
public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    /**
     * Initializes the ViewModel with default text.
     */
    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    /**
     * Returns a read-only LiveData stream of the current text value.
     *
     * @return LiveData containing the text shown on the home screen.
     */
    public LiveData<String> getText() {
        return mText;
    }
}
