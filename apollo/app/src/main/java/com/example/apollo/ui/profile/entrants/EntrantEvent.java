package com.example.apollo.ui.profile.entrants;

import java.util.ArrayList;
import java.util.List;

public class EntrantEvent {
    private String id;
    private String name;
    private String date;
    private List<String> waitingList = new ArrayList<>();

    // Constructor
    public EntrantEvent(String id, String name, String date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }

    // Empty constructor (important if using Firebase or serialization)
    public EntrantEvent() {}

    // --- Waiting list logic ---
    public void addToWaitingList(String userId) {
        if (!waitingList.contains(userId)) {
            waitingList.add(userId);
        }
    }

    public void removeFromWaitingList(String userId) {
        waitingList.remove(userId);
    }

    public List<String> getWaitingList() {
        return waitingList;
    }

    public boolean isUserOnWaitingList(String userId) {
        return waitingList.contains(userId);
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDate() { return date; }

    // --- Setters (if needed) ---
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDate(String date) { this.date = date; }
}