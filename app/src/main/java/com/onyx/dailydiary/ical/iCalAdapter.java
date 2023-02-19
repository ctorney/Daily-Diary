package com.onyx.dailydiary.ical;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.onyx.dailydiary.R;

import java.util.ArrayList;

public class iCalAdapter extends RecyclerView.Adapter<iCalAdapter.CustomViewHolder>
{
    private static final String TAG = iCalAdapter.class.getSimpleName();

    private final iCalAdapter.OnItemListener onItemListener;
    ArrayList<ArrayList<String>> calendarList;

    public iCalAdapter(iCalAdapter.OnItemListener onItemListener, ArrayList<ArrayList<String>> calendarList)
    {
            this.onItemListener = onItemListener;
            this.calendarList = calendarList;
    }

    @Override
    public iCalAdapter.CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.ics_text_row_item
                , viewGroup, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull iCalAdapter.CustomViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder");

        holder.textViewCalName.setText(calendarList.get(position).get(0));
        holder.textViewICS.setText(calendarList.get(position).get(1));
    }


//    public void addCalendar(String cal_name, String cal_ics)
//    {
//
//        notifyItemInserted(calendarList.size() + 1);
//
//
//    }


    @Override
    public int getItemCount() {
        return calendarList.size();
    }

    public interface  OnItemListener
    {
        void onItemClick(int position, CustomViewHolder holder);
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView textViewCalName;
        private TextView textViewICS;


        public CustomViewHolder(View itemView) {
            super(itemView);

            textViewCalName = (TextView) itemView.findViewById(R.id.textViewCalName);
            textViewICS = (TextView) itemView.findViewById(R.id.textViewICS);

            ImageButton add_button = itemView.findViewById(R.id.deletecalendar);
            add_button.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onItemListener.onItemClick(getAdapterPosition(), this);

        }
    }
    }
