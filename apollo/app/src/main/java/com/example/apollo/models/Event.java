package com.example.apollo.models;

/**
 * Represents an event created within the Apollo application.
 * <p>
 * This model class defines the core information for an event, including
 * its title, description, location, scheduled time, registration period,
 * and an optional poster image URL.
 * <p>
 * Instances of this class are typically stored in and retrieved from
 * Firebase Firestore. An empty constructor is included for Firebase’s
 * automatic data mapping.
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

    /**
     * Default no-argument constructor required for Firebase and data binding.
     * <p>
     * Firebase automatically uses this constructor when deserializing
     * documents into {@code Event} objects.
     */
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

    /** @return the event time */
    public String getTime() { return time; }

    /** @return the registration start date or time */
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
