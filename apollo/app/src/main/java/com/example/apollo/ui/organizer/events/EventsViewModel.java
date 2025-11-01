package com.example.apollo.ui.organizer.events;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import java.util.ArrayList;
import com.example.apollo.ui.profile.entrants.EntrantEvent;

public class EventsViewModel extends ViewModel {
    private final MutableLiveData<String> mText;
    private final MutableLiveData<List<EntrantEvent>> events = new MutableLiveData<>(new ArrayList<>());

    public EventsViewModel() {
        mText = new MutableLiveData<>();

        mText.setValue("This is Organizer Events fragment");
        List<EntrantEvent> sampleEvents = new ArrayList<>();
        sampleEvents.add(new EntrantEvent("1", "Hackathon", "Nov 5"));
        sampleEvents.add(new EntrantEvent("2", "Art Night", "Nov 10"));
        events.setValue(sampleEvents);
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void joinWaitingList(String eventId, String userId) {
        List<EntrantEvent> eventList = events.getValue();
        if (eventList == null) return;

        for (EntrantEvent e : eventList) {
            if (e.getId().equals(eventId)) {
                e.addToWaitingList(userId);
                break;
            }
        }

        // Update LiveData so UI can refresh
        events.setValue(eventList);
    }

}

