package com.sourav.fakecall.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sourav.fakecall.R;
import com.sourav.fakecall.models.ScheduledCall;

import java.util.Calendar;
import java.util.List;

public class ScheduledCallAdapter extends RecyclerView.Adapter<ScheduledCallAdapter.ViewHolder> {

    private List<ScheduledCall> calls;
    private OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(ScheduledCall call);
    }

    public ScheduledCallAdapter(List<ScheduledCall> calls, OnDeleteClickListener listener) {
        this.calls = calls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scheduled_call, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduledCall call = calls.get(position);
        holder.tvCallerName.setText(call.getCallerName());
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(call.getTriggerTime());
        String dateStr = DateFormat.format("dd MMM, hh:mm a", cal).toString();
        holder.tvDateTime.setText(dateStr);

        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(call));
    }

    @Override
    public int getItemCount() {
        return calls.size();
    }

    public void updateData(List<ScheduledCall> newCalls) {
        this.calls = newCalls;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCallerName, tvDateTime;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvCallerName = itemView.findViewById(R.id.tvItemCallerName);
            tvDateTime = itemView.findViewById(R.id.tvItemDateTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
