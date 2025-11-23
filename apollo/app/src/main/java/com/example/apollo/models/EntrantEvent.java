package com.example.apollo.models;

import java.util.ArrayList;
import java.util.List;

/**
 * EntrantEvent.java
 *
 * Represents an event that an entrant can register for.
 * Each event stores its ID, name, date, and a waiting list of user IDs.
 *
 * This model is designed for use with Firebase Firestore or similar databases,
 * where objects are serialized and deserialized automatically.
 * It includes an empty constructor for compatibility with Firebase.
 *
 * The class also provides basic waiting list management methods for
 * adding, removing, and checking user participation.
 */

public class EntrantEvent {

    //unique identifier for the event
    private String id;

    //display name or title of the event
    private String name;

    // Date of the event
    private String date;

    // list of user IDs currently on the waiting list
    private List<String> waitingList = new ArrayList<>();

    /**
     * Constructs a new EntrantEvent with the given properties.
     *
     * @param id   the unique ID of the event
     * @param name the event name
     * @param date the event date
     */
    public EntrantEvent(String id, String name, String date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }

    // Empty constructor
    public EntrantEvent() {}

    // waitlist logic


    /**
     * Adds a user to the waiting list if they are not already in it.
     *
     * @param userId the ID of the user to add
     */
    public void addToWaitingList(String userId) {
        if (!waitingList.contains(userId)) {
            waitingList.add(userId);
        }
    }

    /**
     * Removes a user from the waiting list if they are currently on it.
     *
     * @param userId the ID of the user to remove
     */
    public void removeFromWaitingList(String userId) {
        waitingList.remove(userId);
    }



    /**
     * Retrieves the list of user IDs currently on the waiting list.
     *
     * @return a list of user IDs
     */
    public List<String> getWaitingList() {
        return waitingList;
    }


    /**
     * Checks whether a given user is currently on the waiting list.
     *
     * @param userId the ID of the user to check
     * @return true if the user is on the waiting list, false otherwise
     */
    public boolean isUserOnWaitingList(String userId) {
        return waitingList.contains(userId);
    }

    // getters
    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDate() {
        return date;
    }

    // setters
    public void setId(String id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setDate(String date) {
        this.date = date;
    }
}