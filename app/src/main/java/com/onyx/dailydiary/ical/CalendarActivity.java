package com.onyx.dailydiary.ical;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.onyx.dailydiary.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

public class CalendarActivity extends AppCompatActivity implements iCalAdapter.OnItemListener, View.OnClickListener {
    private static final String TAG = CalendarActivity.class.getSimpleName();

    private final String filepath = "Calendars";
    private final String filename = "calendar_list.txt";


    iCalAdapter mAdapter;

    EditText cal_name;
    EditText cal_url;
    ArrayList<ArrayList<String>> calendarList= new ArrayList<>();
    RecyclerView mRecentRecyclerView;
    LinearLayoutManager mRecentLayoutManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendars);
        getSupportActionBar().hide();
        initData();
        initRecyclerView();

        ImageButton back_button = findViewById(R.id.back_button);
        back_button.setOnClickListener(this);

        ImageButton add_button = findViewById(R.id.cal_add_button);
        add_button.setOnClickListener(this);

        cal_name = findViewById(R.id.name_text_input);
        cal_url = findViewById(R.id.url_text_input);
    }


    private void initData() {
        Log.d(TAG, "initData");


        File calendarFile = new File(getExternalFilesDir(filepath), filename);

        if (calendarFile.exists())
        {
            FileInputStream is = null;
            try {
                is = new FileInputStream(calendarFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            try {
                line = reader.readLine();

                while(line != null){
                    String[] splitLine = line.split(",");

                    if (splitLine.length==3){
                        ArrayList<String> calendarLine = new ArrayList<>(3);
                        calendarLine.add(splitLine[0]);
                        calendarLine.add(splitLine[1]);
                        calendarLine.add(splitLine[2]);
                        calendarList.add(calendarLine);
                    }
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        }
    }

    private void initRecyclerView() {



        mRecentRecyclerView = (RecyclerView) findViewById(R.id.calendarICSRecyclerView);
        mRecentRecyclerView.setHasFixedSize(false);
        mRecentLayoutManager = new LinearLayoutManager(this);
        mRecentRecyclerView.setLayoutManager(mRecentLayoutManager);

        mAdapter = new iCalAdapter(this, calendarList);

//        RecyclerView.Adapter<CustomViewHolder> mAdapter = new RecyclerView.Adapter<CustomViewHolder>() {
//            @Override
//            public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
//                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.ics_text_row_item
//                        , viewGroup, false);
//                return new CustomViewHolder(view);
//            }
//
//            @Override
//            public void onBindViewHolder(CustomViewHolder viewHolder, int i) {
//                viewHolder.textViewICS.setText(mItems.get(i));
//            }
//
//            @Override
//            public int getItemCount() {
//                return mItems.size();
//            }
//
//
//        };
        mRecentRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.back_button:
//                onBackPressed();
                finish();
                break;
            case R.id.cal_add_button:
                addCaltoList();
                break;



        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();


    }

    @Override
    public void onDestroy(){


        Log.d(TAG, "onDestroy");
        File calendarFile = new File(getExternalFilesDir(filepath), filename);

        try {
            FileWriter writer = new FileWriter(calendarFile);
            for (int i = 0; i < calendarList.size(); i++) {
                writer.write(calendarList.get(i).get(0) + ",");
                writer.write(calendarList.get(i).get(1) + ",");
                writer.write(calendarList.get(i).get(2) + "\n");
            }
            writer.close();
        } catch (IOException ignored) {

        }

        super.onDestroy();



    }
    private void addCaltoList()
    {
        int i = calendarList.size();
        calendarList.add(new ArrayList(2));
        calendarList.get(i).add(String.valueOf(cal_name.getText()).replace(",",""));
        calendarList.get(i).add(String.valueOf(cal_url.getText()).replace(",",""));
        calendarList.get(i).add(randomFileName());
        mAdapter.notifyItemInserted(calendarList.size() );

        cal_name.setText("");
        cal_url.setText("");
    }

    @Override
    public void onItemClick(int position, iCalAdapter.CustomViewHolder holder) {
        calendarList.remove(position);
        mAdapter.notifyItemRemoved(position);
    }

    private String randomFileName() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString + ".ics";
    }
}