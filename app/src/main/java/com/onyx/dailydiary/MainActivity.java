package com.onyx.dailydiary;

import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
// import android.widget.Toolbar;
import org.lsposed.hiddenapibypass.HiddenApiBypass;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.rx.RxManager;
import com.onyx.dailydiary.calendar.CalendarAdapter;
import com.onyx.dailydiary.calendar.CalendarViewHolder;
import com.onyx.dailydiary.databinding.ActivityMainBinding;
import com.onyx.dailydiary.ical.CalendarActivity;
import com.onyx.dailydiary.ical.iCalParser;
import com.onyx.dailydiary.utils.BitmapView;
import com.onyx.dailydiary.utils.GlobalDeviceReceiver;
import com.onyx.dailydiary.utils.PenCallback;
import com.onyx.dailydiary.writer.WriterActivity;

public class MainActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener, View.OnClickListener
{
    private ActivityMainBinding binding;
    private TextView monthText;
    private RecyclerView calendarRecyclerView;
    private LocalDate selectedDate;
    public TouchHelper touchHelper;



    private CalendarViewHolder lastHolder = null;

    private final float STROKE_WIDTH = 4.0f;
    private String DayofMonth;
    private List<Rect> limitRectList = new ArrayList<>();
    private static final String TAG = MainActivity.class.getSimpleName();
    private iCalParser parser;
    private GlobalDeviceReceiver deviceReceiver = new GlobalDeviceReceiver();
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        deviceReceiver.enable(this, true);
        View view = binding.getRoot();
        setContentView(view);

        selectedDate = LocalDate.now();
        initWidgets();
        initReceiver();

        // fix for latest tablets (from https://github.com/gaborauth/toolsboox-android/issues/305)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HiddenApiBypass.addHiddenApiExemptions("");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d");
        DayofMonth = selectedDate.format(formatter);
        List<BitmapView> viewList = new ArrayList<>();
        viewList.add(binding.taskssurfaceview);
        viewList.add(binding.summarysurfaceview);
        PenCallback penCallback = new PenCallback(this,viewList);
        touchHelper = TouchHelper.create(getWindow().getDecorView().getRootView(), penCallback);
        touchHelper.debugLog(false);
        touchHelper.setRawInputReaderEnable(true);
        penCallback.setTouchHelper(touchHelper);

        String summaryFilename =  getCurrentDateString() + ".png";
        String tasksFilename = "tasks.png";
        initSurfaceView(binding.taskssurfaceview, tasksFilename, R.drawable.tasks_bkgrnd);
        initSurfaceView(binding.summarysurfaceview,summaryFilename, R.drawable.summary_bkgrnd);



        Button clear_all = (Button) view.findViewById(R.id.clearsummary);
        clear_all.setOnClickListener(this);
        Button open_diary = (Button) view.findViewById(R.id.opendiary);
        open_diary.setOnClickListener(this);
        Button clear_tasks = (Button) view.findViewById(R.id.clear_tasks);
        clear_tasks.setOnClickListener(this);
        parser = new iCalParser(getApplicationContext());
        parser.loadCalendars();
        setMonthView();


    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        touchHelper.setRawDrawingEnabled(false);
        touchHelper.setRawDrawingRenderEnabled(false);
        touchHelper.closeRawDrawing();
        binding.taskssurfaceview.redrawSurface();
        binding.summarysurfaceview.redrawSurface();
        binding.taskssurfaceview.saveBitmap();
        binding.summarysurfaceview.saveBitmap();

    }

    @Override
    public void onResume() {

        Log.d(TAG, "onResume");
        super.onResume();
        startTouchHelper();

        return;
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.clear_tasks:
                binding.taskssurfaceview.resetBitmap();
                binding.taskssurfaceview.redrawSurface();
                break;
            case R.id.clearsummary:
                binding.summarysurfaceview.resetBitmap();
                binding.summarysurfaceview.redrawSurface();
                break;
            case R.id.opendiary:
                binding.taskssurfaceview.saveBitmap();
                binding.summarysurfaceview.saveBitmap();
                openPage();
                break;
        }
        Log.d(TAG, "onClick");
    }

    @Override
    public void onItemClick(int position, String dayText, CalendarViewHolder holder)
    {
        if(!dayText.equals(""))
        {

            binding.summarysurfaceview.saveBitmap();

            DayofMonth = dayText;
            String summaryFilename =  getCurrentDateString() + ".png";

            binding.summarysurfaceview.setFilename(summaryFilename);
            binding.summarysurfaceview.redrawSurface();

            if (lastHolder!=null){
                lastHolder.layout.setBackgroundColor(Color.WHITE);
                lastHolder.headerlayout.setBackgroundColor(Color.WHITE);
                lastHolder.eventsText.setTextColor(Color.BLACK);
                lastHolder.dayOfMonth.setTextColor(Color.BLACK);
            }
            holder.layout.setBackgroundColor(Color.DKGRAY);
            holder.dayOfMonth.setTextColor(Color.WHITE);
            holder.eventsText.setTextColor(Color.WHITE);
            holder.headerlayout.setBackgroundColor(Color.DKGRAY);

            lastHolder = holder;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reload:
                parser.sync_calendars();
                setMonthView();
                break;
            case R.id.edit_calendars:
                // opening a new intent to open calendar settings activity.
                Intent i = new Intent(MainActivity.this, CalendarActivity.class);
                startActivity(i);
                break;
            default:
                break;
        }

        return true;
    }

    private void initReceiver() {
        deviceReceiver.setSystemNotificationPanelChangeListener(new GlobalDeviceReceiver.SystemNotificationPanelChangeListener() {
            @Override
            public void onNotificationPanelChanged(boolean open) {
                touchHelper.setRawDrawingEnabled(!open);
                Log.d(TAG, "onNotificationPanelChanged " + open);
                binding.taskssurfaceview.saveBitmap();
                binding.summarysurfaceview.saveBitmap();
            }
        }).setSystemScreenOnListener(new GlobalDeviceReceiver.SystemScreenOnListener() {
            @Override
            public void onScreenOn() {
                Log.d(TAG, "onScreenOn");
                selectedDate = LocalDate.now();
                setMonthView();
            }
            @Override
            public void onScreenOff() {
                Log.d(TAG, "onScreenOff");
                onPause();
               }
        });
    }




    private void initSurfaceView(BitmapView surfaceView, String filename, int background) {

        surfaceView.setBackground(background);
        String filepath = "DailyNotes";
        surfaceView.setFilepath(filepath);
        surfaceView.setFilename(filename);
        Log.d(TAG, "initSurfaceView");



        final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "Tasks surfaceCreated");

                Rect limit = new Rect();
                surfaceView.getGlobalVisibleRect(limit);
                limitRectList.add(limit);
                startTouchHelper();
                surfaceView.redrawSurface();

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "Tasks surfaceChanged");
            }


            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                holder.removeCallback(this);
            }
        };
        surfaceView.getHolder().addCallback(surfaceCallback);
    }



    private void initWidgets()
    {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        monthText = findViewById(R.id.monthTV);
    }


    private void startTouchHelper() {
        if (limitRectList.size() < 2) {
            return;
        }
        touchHelper.setStrokeWidth(STROKE_WIDTH);
        touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER);
        touchHelper.setStrokeColor(Color.BLACK);
        touchHelper.setLimitRect(limitRectList, new ArrayList<Rect>())
                .openRawDrawing();

        touchHelper.setRawDrawingEnabled(false);
//        touchHelper.setMultiRegionMode();
        touchHelper.setSingleRegionMode();
        touchHelper.setRawDrawingEnabled(true);
        touchHelper.enableFingerTouch(true);
        touchHelper.setRawDrawingRenderEnabled(true);

    }

    public void openPage(){
        try {
            Intent intent = new Intent(MainActivity.this, WriterActivity.class);
            intent.putExtra("date-string", getCurrentDateString()); //Optional parameters
            intent.putExtra("stroke-width", STROKE_WIDTH);
            startActivity(intent);

        }
        catch (Exception e){
            Toast.makeText(MainActivity.this, "Unable to open daily notes.", Toast.LENGTH_LONG).show();
        }

    }

    public String getCurrentDateString(){
        String currentDate =  DayofMonth + "-" + monthYearFromDate(selectedDate);
        return currentDate;
    }

    private void setMonthView()
    {
        Log.d(TAG, "setMonthView");

        monthText.setText(monthFromDate(selectedDate));
        ArrayList<String> daysInMonth = daysInMonthArray(selectedDate);
        HolderListener HolderListener = this::onTodayHolderCreated;
        CalendarAdapter calendarAdapter = new CalendarAdapter(parser, daysInMonth, selectedDate, this, HolderListener);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 7);


        calendarRecyclerView.setAdapter(null);
        calendarRecyclerView.setLayoutManager(null);

        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);

        calendarAdapter.notifyDataSetChanged();

    }

    public interface HolderListener
    {
        void onTodayHolderCreated(CalendarViewHolder holder);
    }

    void onTodayHolderCreated(CalendarViewHolder holder){
        lastHolder = holder;
    }

    private ArrayList<String> daysInMonthArray(LocalDate date)
    {
        ArrayList<String> daysInMonthArray = new ArrayList<>();
        YearMonth yearMonth = YearMonth.from(date);

        int daysInMonth = yearMonth.lengthOfMonth();

        LocalDate firstOfMonth = selectedDate.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() ;

        for(int i = 1; i <= 42; i++)
        {
            if(i < dayOfWeek || i >= daysInMonth + dayOfWeek)
            {
                daysInMonthArray.add("");
            }
            else
            {
                daysInMonthArray.add(String.valueOf(i - dayOfWeek + 1));
            }
        }
        return  daysInMonthArray;
    }

    private String monthYearFromDate(LocalDate date)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM-yyyy");
        return date.format(formatter);
    }

    private String monthFromDate(LocalDate date)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM");
        return date.format(formatter);
    }

    public void previousMonthAction(View view)
    {
        selectedDate = selectedDate.minusMonths(1);
        setMonthView();
    }

    public void nextMonthAction(View view)
    {
        selectedDate = selectedDate.plusMonths(1);
        setMonthView();
    }

}


