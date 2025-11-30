package com.example.apollo.models;

/**
 * Event.java
 *
 * Represents an event the entrant can join, waitlist for, or view.
 * Stores event metadata and now includes the Firestore document ID.
 */
public class Event {

    /** Firestore document ID */
    private String id;

    private String title;
    private String description;
    private String location;
    private String time;
    private String date;
    private String registrationStart;
    private String registrationEnd;
    private String eventPosterUrl;

    // Required empty constructor for Firestore
    public Event() {}

    public Event(String id,
                 String title,
                 String description,
                 String location,
                 String time,
                 String registrationStart,
                 String registrationEnd,
                 String eventPosterUrl) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.date = date;
        this.time = time;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.eventPosterUrl = eventPosterUrl;
    }

    // ------------------------------------------------------------------------
    // ID (NEW)
    // ------------------------------------------------------------------------

    /** @return the Firestore document ID */
    public String getId() { return id; }

    /** @param id sets the Firestore document ID */
    public void setId(String id) { this.id = id; }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getTime() { return time; }

    /** @return the calendar date of the event (e.g., "12/05/2025") */
    public String getDate() { return date; }
    public String getRegistrationStart() { return registrationStart; }
    public String getRegistrationEnd() { return registrationEnd; }
    public String getEventPosterUrl() { return eventPosterUrl; }

    // ------------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------------

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setTime(String time) { this.time = time; }
    public void setDate(String date) { this.date = date; }
    public void setRegistrationStart(String registrationStart) { this.registrationStart = registrationStart; }
    public void setRegistrationEnd(String registrationEnd) { this.registrationEnd = registrationEnd; }
    public void setEventPosterUrl(String eventPosterUrl) { this.eventPosterUrl = eventPosterUrl; }
}
