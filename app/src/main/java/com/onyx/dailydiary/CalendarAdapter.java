package com.onyx.dailydiary;


import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.util.InternCache;

import java.time.LocalDate;
import java.util.ArrayList;

class CalendarAdapter extends RecyclerView.Adapter<CalendarViewHolder>
{
    private final ArrayList<String> daysOfMonth;
    private final OnItemListener onItemListener;

    public CalendarViewHolder todayHolder = null;
    private final LocalDate selectedDate;
    private final LocalDate currentDate;
    public CalendarAdapter(ArrayList<String> daysOfMonth, LocalDate selectedDate, OnItemListener onItemListener)
    {
        this.daysOfMonth = daysOfMonth;
        this.onItemListener = onItemListener;
        this.selectedDate = selectedDate;
        this.currentDate = LocalDate.now();

        iCalParser test = new iCalParser();

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
        }

        holder.dayOfMonth.setText(daysOfMonth.get(position));
        holder.currentDay.setImageResource(R.drawable.white_circle);

        if (selectedDate.getMonthValue()==currentDate.getMonthValue() && selectedDate.getYear()==currentDate.getYear())
        {
            try{
                if (currentDate.getDayOfMonth()== Integer.valueOf(daysOfMonth.get(position))) {
                    holder.currentDay.setImageResource(R.drawable.filled_circle);
//                    holder.layout.setBackgroundColor(Color.DKGRAY);
//                    holder.dayOfMonth.setTextColor(Color.WHITE);
                    todayHolder = holder;
                }
            }
            catch (Exception e)
            {

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
