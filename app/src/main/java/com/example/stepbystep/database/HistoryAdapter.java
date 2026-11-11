package com.example.stepbystep.database;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbystep.R;
import com.example.stepbystep.session.SessionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Adapter for the completed-session history list. */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private final List<SessionData> items = new ArrayList<>();

    public HistoryAdapter(List<SessionData> initialItems) {
        setItems(initialItems);
    }

    public void setItems(List<SessionData> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
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
        holder.dateTime.setText(session.dateTime);
        holder.steps.setText(String.format(Locale.getDefault(), "Steps: %,d", session.steps));
        holder.shakes.setText(String.format(Locale.getDefault(), "Shakes: %,d", session.shakes));
        long minutes = session.duration / 60;
        long seconds = session.duration % 60;
        holder.duration.setText(String.format(Locale.getDefault(),
                "Duration: %02d:%02d", minutes, seconds));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final TextView dateTime, steps, shakes, duration;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTime = itemView.findViewById(R.id.textHistoryDateTime);
            steps = itemView.findViewById(R.id.textHistorySteps);
            shakes = itemView.findViewById(R.id.textHistoryShakes);
            duration = itemView.findViewById(R.id.textHistoryDuration);
        }
    }
}
