package com.onyx.dailydiary.calendar;


import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.onyx.dailydiary.R;
import com.onyx.dailydiary.ical.iCalParser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarViewHolder>
{
    private final ArrayList<String> daysOfMonth;
    private final OnItemListener onItemListener;
    private iCalParser parser;
    public CalendarViewHolder todayHolder = null;
    private final LocalDate selectedDate;
    private final LocalDate currentDate;
    private static final String TAG = CalendarAdapter.class.getSimpleName();

    public CalendarAdapter(iCalParser parser, ArrayList<String> daysOfMonth, LocalDate selectedDate, OnItemListener onItemListener)
    {
        this.daysOfMonth = daysOfMonth;
        this.onItemListener = onItemListener;
        this.selectedDate = selectedDate;
        this.currentDate = LocalDate.now();
        this.parser = parser;


    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.calendar_cell, parent, false);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int) (parent.getHeight() * 0.166666666);
        return new CalendarViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position)
    {

        if (daysOfMonth.get(position)=="") {
            holder.layout.setBackgroundColor(Color.LTGRAY);
            holder.headerlayout.setBackgroundColor(Color.LTGRAY);

            holder.eventsText.setText("");
        }
        else {
            LocalDate holderDate = selectedDate.withDayOfMonth(Integer.parseInt(daysOfMonth.get(position)));

            List<String> events = parser.get_day_events(holderDate);
            String eventList = "";
            for (String tempstring : events) {
//                Log.d(TAG, tempstring);
                eventList = eventList + tempstring + "\n";
            }
            Log.d(TAG, eventList);
            holder.eventsText.setText(eventList);
        }

        holder.dayOfMonth.setText(daysOfMonth.get(position));
        holder.currentDay.setImageResource(R.drawable.white_circle);


        if (selectedDate.getMonthValue()==currentDate.getMonthValue() && selectedDate.getYear()==currentDate.getYear())
        {
            try{
                if (currentDate.getDayOfMonth()== Integer.valueOf(daysOfMonth.get(position))) {
                    holder.currentDay.setImageResource(R.drawable.filled_circle);
                    todayHolder = holder;
                }
            }
            catch (Exception e)
            {
                Log.d(TAG, e.getMessage());

            }


        }
    }

    @Override
    public int getItemCount()
    {
        return daysOfMonth.size();
    }

    public interface  OnItemListener
    {
        void onItemClick(int position, String dayText, CalendarViewHolder holder);
    }
}
