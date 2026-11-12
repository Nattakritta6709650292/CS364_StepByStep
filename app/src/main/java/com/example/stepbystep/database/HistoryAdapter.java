package com.example.stepbystep.database;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbystep.R;
import com.example.stepbystep.session.SessionData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Adapter for the completed-session history list. */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(SessionData session);
    }

    private static final String STORED_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final List<SessionData> items = new ArrayList<>();
    private boolean[] stepRecords = new boolean[0];
    private boolean[] shakeRecords = new boolean[0];
    @Nullable private OnItemClickListener clickListener;

    public HistoryAdapter(List<SessionData> initialItems) {
        setItems(initialItems);
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
        this.clickListener = listener;
    }

    /** Items must be newest first (the order returned by ActivityRepository.getAllActivities). */
    public void setItems(List<SessionData> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        computeRecords();
        notifyDataSetChanged();
    }

    /** A session is a record when it beat every session that came before it. */
    private void computeRecords() {
        stepRecords = new boolean[items.size()];
        shakeRecords = new boolean[items.size()];
        int bestSteps = 0;
        int bestShakes = 0;
        for (int i = items.size() - 1; i >= 0; i--) {
            SessionData s = items.get(i);
            if (s.steps > bestSteps) {
                stepRecords[i] = true;
                bestSteps = s.steps;
            }
            if (s.shakes > bestShakes) {
                shakeRecords[i] = true;
                bestShakes = s.shakes;
            }
        }
    }

    public static String formatDuration(long totalSeconds) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                totalSeconds / 60, totalSeconds % 60);
    }

    /** Reformats a stored "yyyy-MM-dd HH:mm:ss" value; returns the raw text if it cannot be parsed. */
    public static String formatDate(String dateTime, String pattern) {
        try {
            Date date = new SimpleDateFormat(STORED_PATTERN, Locale.US).parse(dateTime);
            if (date != null) return new SimpleDateFormat(pattern, Locale.ENGLISH).format(date);
        } catch (ParseException | NullPointerException ignored) {
        }
        return dateTime;
    }

    public static double perMinute(int count, long durationSeconds) {
        if (durationSeconds <= 0) return 0.0;
        return count / (durationSeconds / 60.0);
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SessionData session = items.get(position);
        holder.dateTime.setText(formatDate(session.dateTime, "d MMM HH:mm"));
        holder.date.setText(formatDate(session.dateTime, "d MMM yyyy • HH:mm"));
        holder.summary.setText(String.format(Locale.getDefault(),
                "%,d steps • %,d shakes • %s • %.1f steps/min",
                session.steps, session.shakes, formatDuration(session.duration),
                perMinute(session.steps, session.duration)));
        holder.stepRecord.setVisibility(stepRecords[position] ? View.VISIBLE : View.GONE);
        holder.shakeRecord.setVisibility(shakeRecords[position] ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(session);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final TextView dateTime, date, summary, stepRecord, shakeRecord;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTime = itemView.findViewById(R.id.textHistoryDateTime);
            date = itemView.findViewById(R.id.textHistoryDate);
            summary = itemView.findViewById(R.id.textHistorySummary);
            stepRecord = itemView.findViewById(R.id.badgeStepRecord);
            shakeRecord = itemView.findViewById(R.id.badgeShakeRecord);
        }
    }
}
