package com.example.stepbystep.session;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionManager {

    private SessionData currentSession;
    private SessionListener listener;

    private boolean isRunning = false;

    private long startTimeMillis = 0;
    private long durationMillis = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                updateDuration();
                notifySessionUpdated();
                handler.postDelayed(this, 1000);
            }
        }
    };

    public SessionManager() {
        currentSession = new SessionData();
    }

    public void setListener(SessionListener listener) {
        this.listener = listener;
    }

    public void startSession() {

        if (isRunning) {
            return;
        }

        currentSession = new SessionData();

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault());

        currentSession.dateTime = dateFormat.format(new Date());

        startTimeMillis = SystemClock.elapsedRealtime();
        durationMillis = 0;

        isRunning = true;

        handler.removeCallbacks(timerRunnable);
        handler.post(timerRunnable);

        notifySessionUpdated();
    }

    public SessionData stopSession() {

        if (!isRunning) {
            return currentSession;
        }

        updateDuration();

        isRunning = false;

        handler.removeCallbacks(timerRunnable);

        notifySessionFinished();

        return currentSession;
    }

    public void onStepCountChanged(int steps) {

        if (!isRunning) {
            return;
        }

        currentSession.steps = steps;

        notifySessionUpdated();
    }

    public void onShakeDetected() {

        if (!isRunning) {
            return;
        }

        currentSession.shakes++;

        notifySessionUpdated();
    }

    public SessionData getCurrentSession() {

        updateDuration();

        return currentSession;
    }

    public double getStepsPerMinute() {

        if (currentSession.duration <= 0) {
            return 0.0;
        }

        return currentSession.steps
                / (currentSession.duration / 60.0);
    }

    public double getShakesPerMinute() {

        if (currentSession.duration <= 0) {
            return 0.0;
        }

        return currentSession.shakes
                / (currentSession.duration / 60.0);
    }

    private void updateDuration() {

        if (isRunning) {

            durationMillis =
                    SystemClock.elapsedRealtime() - startTimeMillis;

            currentSession.duration = durationMillis / 1000;
        }
    }

    private void notifySessionUpdated() {

        if (listener != null) {
            listener.onSessionUpdated(currentSession);
        }
    }

    private void notifySessionFinished() {

        if (listener != null) {
            listener.onSessionFinished(currentSession);
        }
    }

}
