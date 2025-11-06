/**
 * EventsViewModel.java
 * --------------------
 * ViewModel class that manages and stores data related to events created or managed by organizers.
 *
 * <p>This ViewModel provides observable data (using LiveData) to the UI components, ensuring
 * that event data persists through configuration changes such as screen rotations.</p>
 *
 * <p><b>Role in Application:</b><br>
 * Acts as the data layer for the Organizer Events screen. It holds a list of {@link EntrantEvent} objects,
 * manages updates such as users joining waiting lists, and provides observable data to fragments.</p>
 *
 * <p><b>Outstanding Issues:</b><br>
 * - Currently uses a static sample list of events instead of dynamic data from Firebase or another backend.<br>
 * - Future enhancement: integrate with repository pattern for real-time event updates.</p>
 */

package com.example.apollo.ui.organizer.events;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import java.util.ArrayList;
import com.example.apollo.models.EntrantEvent;

public class EventsViewModel extends ViewModel {

    /** Observable text used to display static or informational messages on the events screen. */
    private final MutableLiveData<String> mText;

    /** LiveData list holding all events visible to the organizer. */
    private final MutableLiveData<List<EntrantEvent>> events = new MutableLiveData<>(new ArrayList<>());

    /**
     * Default constructor that initializes demo data for testing or UI preview purposes.
     * <p>Populates the list with sample events and sets a default fragment label.</p>
     */
    public EventsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Organizer Events fragment");

        List<EntrantEvent> sampleEvents = new ArrayList<>();
        sampleEvents.add(new EntrantEvent("1", "Hackathon", "Nov 5"));
        sampleEvents.add(new EntrantEvent("2", "Art Night", "Nov 10"));
        events.setValue(sampleEvents);
    }

    /**
     * Returns observable text data for the fragment’s title or description.
     *
     * @return LiveData<String> that provides UI with descriptive text.
     */
    public LiveData<String> getText() {
        return mText;
    }

    /**
     * Adds a user to the waiting list of a specific event.
     * <p>Finds the event by its ID and calls {@link EntrantEvent#addToWaitingList(String)}
     * to update its waiting list. After modification, the LiveData is updated so
     * that any observers (e.g., RecyclerView adapters) refresh automatically.</p>
     *
     * @param eventId The unique identifier of the event.
     * @param userId  The ID of the user joining the waiting list.
     */
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
