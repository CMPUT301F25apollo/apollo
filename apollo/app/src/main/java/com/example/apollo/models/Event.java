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

    /** The title or name of the event. */
    private String title;

    /** A brief description summarizing the event’s purpose or content. */
    private String description;

    /** The physical or virtual location where the event will take place. */
    private String location;

    /** The time at which the event is scheduled to occur. */
    private String time;

    /** The date or time when event registration opens. */
    private String registrationStart;

    /** The date or time when event registration closes. */
    private String registrationEnd;

    /** The URL of the poster image associated with the event. */
    private String eventPosterUrl;

    // Empty constructor... required for Firebase or data binding

    public Event() {}

    /**
     * Constructs a fully initialized {@code Event} instance.
     *
     * @param title             the event title
     * @param description       the event description
     * @param location          the event location
     * @param time              the scheduled time of the event
     * @param registrationStart when registration opens
     * @param registrationEnd   when registration closes
     * @param eventPosterUrl    the URL for the event’s poster image
     */
    public Event(String title, String description, String location, String time,
                 String registrationStart, String registrationEnd, String eventPosterUrl) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.time = time;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.eventPosterUrl = eventPosterUrl;
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    /** @return the event title */
    public String getTitle() { return title; }

    /** @return the event description */
    public String getDescription() { return description; }

    /** @return the event location */
    public String getLocation() { return location; }
    public String getTime() { return time; }
    public String getRegistrationStart() { return registrationStart; }

    /** @return the registration end date or time */
    public String getRegistrationEnd() { return registrationEnd; }

    /** @return the poster image URL for the event */
    public String getEventPosterUrl() { return eventPosterUrl; }

    // ------------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------------

    /** @param title sets the event title */
    public void setTitle(String title) { this.title = title; }

    /** @param description sets the event description */
    public void setDescription(String description) { this.description = description; }

    /** @param location sets the event location */
    public void setLocation(String location) { this.location = location; }

    /** @param time sets the scheduled event time */
    public void setTime(String time) { this.time = time; }

    /** @param registrationStart sets the registration start date or time */
    public void setRegistrationStart(String registrationStart) { this.registrationStart = registrationStart; }

    /** @param registrationEnd sets the registration end date or time */
    public void setRegistrationEnd(String registrationEnd) { this.registrationEnd = registrationEnd; }

    /** @param eventPosterUrl sets the URL of the event’s poster image */
    public void setEventPosterUrl(String eventPosterUrl) { this.eventPosterUrl = eventPosterUrl; }
}