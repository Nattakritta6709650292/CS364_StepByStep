package com.example.stepbystep.session;

public interface SessionListener {
    void onSessionUpdated(SessionData data);
    void onSessionFinished(SessionData data);
}