package com.example.apollo.models;

/**
 * EntrantEvent.java
 *
 * Represents an event that an entrant (user) can register for.
 * Each event stores its ID, name, date, and a waiting list of user IDs.
 *
 * This model is designed for use with Firebase Firestore or similar databases,
 * where objects are serialized and deserialized automatically.
 * It includes an empty constructor for compatibility with Firebase.
 *
 * The class also provides basic waiting list management methods for
 * adding, removing, and checking user participation.
 */
public class Event {
    private String title;
    private String description;
    private String location;
    private String time;
    private String registrationStart;
    private String registrationEnd;
    private String eventPosterUrl;

    // Empty constructor... required for Firebase or data binding

    public Event() {}

    public Event(String title, String description, String location, String time, String registrationStart, String registrationEnd, String eventPosterUrl) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.time = time;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.eventPosterUrl = eventPosterUrl;
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getTime() {
        return time;
    }
    public String getRegistrationStart() { return registrationStart; }
    public String getRegistrationEnd() { return registrationEnd; }
    public String getEventPosterUrl() { return eventPosterUrl; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setTime(String time) { this.time = time; }
    public void setRegistrationStart(String registrationStart) { this.registrationStart = registrationStart; }
    public void setRegistrationEnd(String registrationEnd) { this.registrationEnd = registrationEnd; }
    public void setEventPosterUrl(String eventPosterUrl) { this.eventPosterUrl = eventPosterUrl; }
}