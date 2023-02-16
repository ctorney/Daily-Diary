package com.onyx.dailydiary;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class CalendarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
{
    public final TextView dayOfMonth;
    public final TextView eventsText;

    public final ImageView currentDay;
    public final LinearLayout layout;
    public final LinearLayout headerlayout;


    private final CalendarAdapter.OnItemListener onItemListener;
    public CalendarViewHolder(@NonNull View itemView, CalendarAdapter.OnItemListener onItemListener)
    {
        super(itemView);
        dayOfMonth = itemView.findViewById(R.id.cellDayText);
        eventsText = itemView.findViewById(R.id.eventsText);
        currentDay = itemView.findViewById(R.id.selectedDay);
        layout = itemView.findViewById(R.id.cellDayLayout);
        headerlayout = itemView.findViewById(R.id.headerLayout);
        this.onItemListener = onItemListener;
        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view)
    {
        onItemListener.onItemClick(getAdapterPosition(), (String) dayOfMonth.getText(), this);
    }
}