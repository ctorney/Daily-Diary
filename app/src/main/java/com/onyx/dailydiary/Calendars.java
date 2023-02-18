package com.onyx.dailydiary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class Calendars extends AppCompatActivity {

    ArrayList<String> mItems;
    int ITEMS_COUNT =10;
    RecyclerView mRecentRecyclerView;
    LinearLayoutManager mRecentLayoutManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendars);
        getSupportActionBar().hide();
        initData();
        initRecyclerView();

    }

    private void initData() {
        mItems = new ArrayList<String>();
        for (int i = 0; i < ITEMS_COUNT; i++) {
            mItems.add("Item " + (i + 1));
        }
    }

    private void initRecyclerView() {
        mRecentRecyclerView = (RecyclerView) findViewById(R.id.calendarICSRecyclerView);
        mRecentRecyclerView.setHasFixedSize(true);
        mRecentLayoutManager = new LinearLayoutManager(this);
        mRecentRecyclerView.setLayoutManager(mRecentLayoutManager);


        RecyclerView.Adapter<CustomViewHolder> mAdapter = new RecyclerView.Adapter<CustomViewHolder>() {
            @Override
            public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.ics_text_row_item
                        , viewGroup, false);
                return new CustomViewHolder(view);
            }

            @Override
            public void onBindViewHolder(CustomViewHolder viewHolder, int i) {
                viewHolder.noticeSubject.setText(mItems.get(i));
            }

            @Override
            public int getItemCount() {
                return mItems.size();
            }

        };
        mRecentRecyclerView.setAdapter(mAdapter);
    }
    private class CustomViewHolder extends RecyclerView.ViewHolder {

        private TextView noticeSubject;

        public CustomViewHolder(View itemView) {
            super(itemView);

            noticeSubject = (TextView) itemView.findViewById(R.id.textViewICS);
        }
    }
}