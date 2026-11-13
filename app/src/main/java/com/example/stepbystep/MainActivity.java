package com.example.stepbystep;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbystep.database.ActivityRepository;
import com.example.stepbystep.database.HistoryAdapter;
import com.example.stepbystep.sensor.ShakeDetector;
import com.example.stepbystep.sensor.StepCounterManager;
import com.example.stepbystep.session.SessionData;
import com.example.stepbystep.session.SessionListener;
import com.example.stepbystep.session.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SessionListener {

    private static final int REQUEST_ACTIVITY_RECOGNITION = 100;

    private SessionManager sessionManager;
    private StepCounterManager stepCounterManager;
    private ShakeDetector shakeDetector;
    private ActivityRepository repository;
    private HistoryAdapter historyAdapter;

    // All database work runs on this single thread (repository must not be used on the UI thread).
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    private boolean sessionRunning = false;

    private TextView tvHeaderTimer, tvTimer;
    private TextView tvStepsPerMinute, tvShakesPerMinute;
    private TextView tvStepCount, tvShakeCount, tvStepStatus;
    private TextView tvTotalSummary, tvTotalSessions;
    private TextView tvNewStepRecord, tvNewShakeRecord, tvHistoryEmpty;
    private TextView tvShakeStatus;
    private RecyclerView recyclerHistory;
    private Button buttonStartSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dark app background, so keep the status/navigation bar icons light.
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();

        repository = new ActivityRepository(this);

        historyAdapter = new HistoryAdapter(null);
        historyAdapter.setOnItemClickListener(this::showDetailDialog);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistory.setAdapter(historyAdapter);

        sessionManager = new SessionManager();
        sessionManager.setListener(this);

        stepCounterManager = new StepCounterManager(this);
        stepCounterManager.setListener(steps -> sessionManager.onStepCountChanged(steps));

        shakeDetector = new ShakeDetector(this);
        shakeDetector.setListener(() -> sessionManager.onShakeDetected());

        tvShakeStatus.setVisibility(
            shakeDetector.isSensorAvailable()
                ? View.GONE
                : View.VISIBLE
        );

        // One button: "start" when idle, "save" (stop + insert) while a session is running.
        buttonStartSave.setOnClickListener(v -> {
            if (sessionRunning) stopActivity();
            else onStartClicked();
        });

        showStepStatus();
        updateButtons();
        refreshData();
    }

    private void bindViews() {
        tvHeaderTimer = findViewById(R.id.tvHeaderTimer);
        tvTimer = findViewById(R.id.tv_session_timer);
        tvStepsPerMinute = findViewById(R.id.tv_steps_per_minute);
        tvShakesPerMinute = findViewById(R.id.tv_shakes_per_minute);
        buttonStartSave = findViewById(R.id.button_start_session);

        tvStepCount = findViewById(R.id.tvStepCount);
        tvStepStatus = findViewById(R.id.tvStepStatus);
        tvShakeCount = findViewById(R.id.tvShakeCount);
        tvShakeStatus = findViewById(R.id.tvShakeStatus);

        tvTotalSummary = findViewById(R.id.tvTotalSummary);
        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvNewStepRecord = findViewById(R.id.tvNewStepRecord);
        tvNewShakeRecord = findViewById(R.id.tvNewShakeRecord);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);
        recyclerHistory = findViewById(R.id.recyclerHistory);
    }

    // ---- Start / Stop -------------------------------------------------------------------

    private void onStartClicked() {
        if (sessionRunning) return;
        if (stepCounterManager.isSensorAvailable()
                && !StepCounterManager.hasPermission(this)) {
            StepCounterManager.requestPermission(this, REQUEST_ACTIVITY_RECOGNITION);
            return;
        }
        startActivityTracking();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_ACTIVITY_RECOGNITION) return;

        showStepStatus();
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        // Without the permission steps stay at 0, but shakes can still be tracked.
        if (!granted) {
            Toast.makeText(this, R.string.step_counter_no_permission, Toast.LENGTH_LONG).show();
        }
        startActivityTracking();
    }

    private void startActivityTracking() {
        tvNewStepRecord.setVisibility(View.GONE);
        tvNewShakeRecord.setVisibility(View.GONE);
        sessionRunning = true;

        sessionManager.startSession();
        stepCounterManager.start();
        shakeDetector.start();

        updateButtons();
    }

    private void stopActivity() {
        if (!sessionRunning) return;
        sessionRunning = false;

        stepCounterManager.stop();
        shakeDetector.stop();
        sessionManager.stopSession(); // triggers onSessionFinished

        updateButtons();
    }

    // ---- SessionListener ----------------------------------------------------------------

    @Override
    public void onSessionUpdated(SessionData data) {
        String duration = formatDuration(data.duration);
        tvHeaderTimer.setText(duration);
        tvTimer.setText(duration);

        tvStepCount.setText(String.format(Locale.getDefault(), "%,d", data.steps));
        tvShakeCount.setText(String.format(Locale.getDefault(), "%,d", data.shakes));

        tvStepsPerMinute.setText(getString(R.string.rate_format,
                sessionManager.getStepsPerMinute()));
        tvShakesPerMinute.setText(getString(R.string.rate_format,
                sessionManager.getShakesPerMinute()));
    }

    @Override
    public void onSessionFinished(SessionData data) {
        onSessionUpdated(data);
        saveSession(new SessionData(data.steps, data.shakes, data.duration, data.dateTime));
    }

    private void saveSession(SessionData session) {
        dbExecutor.execute(() -> {
            boolean saved;
            boolean stepRecord = false;
            boolean shakeRecord = false;
            try {
                int bestSteps = 0;
                int bestShakes = 0;
                for (SessionData s : repository.getAllActivities()) {
                    bestSteps = Math.max(bestSteps, s.steps);
                    bestShakes = Math.max(bestShakes, s.shakes);
                }

                saved = repository.insertActivity(session) != -1;
                stepRecord = saved && session.steps > bestSteps;
                shakeRecord = saved && session.shakes > bestShakes;
            } catch (RuntimeException e) {
                saved = false;
            }

            final boolean finalSaved = saved;
            final boolean finalStepRecord = stepRecord;
            final boolean finalShakeRecord = shakeRecord;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                Toast.makeText(this, finalSaved ? R.string.saved : R.string.save_failed,
                        Toast.LENGTH_SHORT).show();
                tvNewStepRecord.setVisibility(finalStepRecord ? View.VISIBLE : View.GONE);
                tvNewShakeRecord.setVisibility(finalShakeRecord ? View.VISIBLE : View.GONE);
            });
            loadData();
        });
    }

    // ---- Totals and history -------------------------------------------------------------

    private void refreshData() {
        dbExecutor.execute(this::loadData);
    }

    /** Must be called on the database thread. */
    private void loadData() {
        final List<SessionData> history = repository.getAllActivities();
        final int steps = repository.getTotalSteps();
        final int shakes = repository.getTotalShakes();
        final int sessions = repository.getTotalSessions();
        runOnUiThread(() -> {
            if (isDestroyed()) return;
            tvTotalSummary.setText(getString(R.string.total_summary_format, steps, shakes));
            tvTotalSessions.setText(getString(R.string.total_sessions_format, sessions));

            historyAdapter.setItems(history);
            boolean empty = history.isEmpty();
            tvHistoryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    private void showDetailDialog(SessionData session) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_activity_detail, null);
        ((TextView) view.findViewById(R.id.tvDetailDateTime)).setText(session.dateTime);
        ((TextView) view.findViewById(R.id.tvDetailSteps)).setText(
                getString(R.string.detail_steps, session.steps));
        ((TextView) view.findViewById(R.id.tvDetailShakes)).setText(
                getString(R.string.detail_shakes, session.shakes));
        ((TextView) view.findViewById(R.id.tvDetailDuration)).setText(
                getString(R.string.detail_duration, HistoryAdapter.formatDuration(session.duration)));
        ((TextView) view.findViewById(R.id.tvDetailStepsPerMin)).setText(
                getString(R.string.detail_steps_per_min,
                        HistoryAdapter.perMinute(session.steps, session.duration)));
        ((TextView) view.findViewById(R.id.tvDetailShakesPerMin)).setText(
                getString(R.string.detail_shakes_per_min,
                        HistoryAdapter.perMinute(session.shakes, session.duration)));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.detail_title)
                .setView(view)
                .setPositiveButton(R.string.detail_close, null)
                .setNegativeButton(R.string.detail_delete, (dialog, which) -> deleteSession(session))
                .show();
    }

    private void deleteSession(SessionData session) {
        dbExecutor.execute(() -> {
            repository.deleteActivity(session.id);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show();
                // Totals and record badges are recalculated from the remaining rows.
                tvNewStepRecord.setVisibility(View.GONE);
                tvNewShakeRecord.setVisibility(View.GONE);
            });
            loadData();
        });
    }

    // ---- UI helpers ---------------------------------------------------------------------

    private void updateButtons() {
        buttonStartSave.setText(sessionRunning ? R.string.button_save : R.string.button_start);
    }

    private void showStepStatus() {
        switch (stepCounterManager.getStatus()) {
            case NO_SENSOR:
                tvStepStatus.setText(R.string.step_counter_no_sensor);
                tvStepStatus.setVisibility(View.VISIBLE);
                break;
            case NO_PERMISSION:
                tvStepStatus.setText(R.string.step_counter_no_permission);
                tvStepStatus.setVisibility(View.VISIBLE);
                break;
            default:
                tvStepStatus.setVisibility(View.GONE);
        }
    }

    private static String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return h > 0
                ? String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
                : String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    // ---- Lifecycle ----------------------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        showStepStatus();
        if (sessionRunning) {
            stepCounterManager.resume();
            shakeDetector.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Sensors are released while in the background; the session clock keeps running.
        stepCounterManager.pause();
        shakeDetector.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sessionRunning) {
            stepCounterManager.stop();
            shakeDetector.stop();
            sessionManager.setListener(null);
            sessionManager.stopSession();
        }
        dbExecutor.execute(repository::close);
        dbExecutor.shutdown();
    }
}
