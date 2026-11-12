
package com.example.stepbystep.session;

public class SessionData {

    // Database row id (0 until the session has been loaded from SQLite)
    public int id;

    // Number of steps in the session
    public int steps;

    // Number of shakes in the session
    public int shakes;

    // Session duration in seconds
    public long duration;

    // Session date and time
    public String dateTime;

    // Default constructor
    public SessionData() {
        this.steps = 0;
        this.shakes = 0;
        this.duration = 0;
        this.dateTime = "";
    }

    // Constructor with session data
    public SessionData(int steps, int shakes,
                       long duration, String dateTime) {
        this.steps = steps;
        this.shakes = shakes;
        this.duration = duration;
        this.dateTime = dateTime;
    }
}
