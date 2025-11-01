package com.example.apollo.models;

public class Event {
    private String title;
    private String description;
    private String location;
    private String time;
    private String registrationStart;
    private String registrationEnd;

    // Empty constructor... required for Firebase or data binding
    public Event() {}

    public Event(String title, String description, String location, String time, String registrationStart, String registrationEnd) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.time = time;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getTime() { return time; }
    public String getRegistrationStart() { return registrationStart; }
    public String getRegistrationEnd() { return registrationEnd; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setTime(String time) { this.time = time; }
    public void setRegistrationStart(String registrationStart) { this.registrationStart = registrationStart; }
    public void setRegistrationEnd(String registrationEnd) { this.registrationEnd = registrationEnd; }
}
