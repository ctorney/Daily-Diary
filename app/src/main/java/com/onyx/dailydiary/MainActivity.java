package com.onyx.dailydiary;

import static java.lang.System.currentTimeMillis;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
// import android.widget.Toolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.pen.data.TouchPointList;
//import androidx.navigation.NavController;
//import androidx.navigation.Navigation;
//import androidx.navigation.ui.AppBarConfiguration;
//import androidx.navigation.ui.NavigationUI;
//import com.example.dailynotes.databinding.ActivityMainBinding;
//
//
//import android.view.Menu;
//import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener
{
    private TextView monthText;
    private RecyclerView calendarRecyclerView;
    private LocalDate selectedDate;
    public TouchHelper touchHelper;
    private boolean redrawRunning = false;

    private final float STROKE_WIDTH = 4.0f;
    private boolean needsSave = false;
    private long lastDraw = 0;
    private long refreshInterval = 500;
    public boolean writeTasks;
    private RawInputCallback rawInputCallback;
    private String DayofMonth;
    private List<Rect> limitRectList = new ArrayList<>();
    private static final String TAG = MainActivity.class.getSimpleName();

//    private AppBarConfiguration appBarConfiguration;
//    private ActivityMainBinding binding;
//    private String filename = "SampleFile.pdf";
    private String filepath = "DailyNotes";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWidgets();
        selectedDate = LocalDate.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d");
        DayofMonth = selectedDate.format(formatter);

        setMonthView();
        touchHelper = TouchHelper.create(getWindow().getDecorView().getRootView(), getRawInputCallback());
        touchHelper.debugLog(true);

    }
    @Override
    protected void onDestroy() {
        touchHelper.closeRawDrawing();
        super.onDestroy();
        Log.d(TAG, "onDestroy");

    }

    public String getCurrentDateString(){
        String filename =  DayofMonth + "-" + monthYearFromDate(selectedDate);
        return filename;
    }
    public void openPage(){
        try {

//            String filename =  DayofMonth + "-" + monthYearFromDate(selectedDate) + ".pdf";
//
//            File myExternalFile = new File(getExternalFilesDir(filepath), filename);
//
//            if (!myExternalFile.exists())
//                generatePDF(myExternalFile);
//
//            Uri path = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID.toString() + ".provider", myExternalFile);
//
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setDataAndType(path,"application/pdf");
//            intent.putExtra("pageno", 2);
//
////            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            startActivity(intent);

            Intent intent = new Intent(MainActivity.this, WriterActivity.class);

            intent.putExtra("date-string", getCurrentDateString()); //Optional parameters
            intent.putExtra("stroke-width", STROKE_WIDTH);
            startActivity(intent);

        }
        catch (Exception e)
        {

            Toast.makeText(MainActivity.this, "Unable to open daily notes file.", Toast.LENGTH_LONG).show();

        }

    }

    public void debugTouchHelper(){
        Log.d(TAG, "vals" + touchHelper.isRawDrawingCreated() + touchHelper.isRawDrawingRenderEnabled()+touchHelper.isRawDrawingInputEnabled());

    }
    private void initWidgets()
    {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        monthText = findViewById(R.id.monthTV);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        TasksFragment tasksFragment = TasksFragment.GetInstance();
        tasksFragment.saveBitmap();


        SummaryFragment summaryFragment = SummaryFragment.GetInstance();
        summaryFragment.saveBitmap();
//        limitRectList.clear();
//        touchHelper.closeRawDrawing();
        touchHelper.closeRawDrawing();
        Log.d(TAG, "- ON PAUSE -");
    }

    public TouchHelper getTouchHelper() {
        return touchHelper;
    }

    @Override
    public void onResume() {
        super.onResume();
//        touchHelper = TouchHelper.create(getWindow().getDecorView().getRootView(), callback);
        touchHelper.setStrokeWidth(STROKE_WIDTH).setLimitRect(limitRectList, new ArrayList<Rect>()).setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER);
//
//
        touchHelper.setStrokeColor(Color.BLACK);
        touchHelper.setMultiRegionMode();
        touchHelper.setRawDrawingEnabled(false);
        touchHelper.setRawDrawingEnabled(true);
        touchHelper.setRawDrawingRenderEnabled(false);
        touchHelper.setRawDrawingRenderEnabled(true);

        touchHelper.openRawDrawing();


        Log.d(TAG, "- ON RESUME -");

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_reload:
                Toast.makeText(this, "Reload selected", Toast.LENGTH_SHORT)
                        .show();
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:

                // opening a new intent to open settings activity.
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);

                break;
            default:
                break;
        }

        return true;
    }
    private void setMonthView()
    {
        monthText.setText(monthFromDate(selectedDate));
        ArrayList<String> daysInMonth = daysInMonthArray(selectedDate);

        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth, this);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 7);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);
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

    private String dayMonthYearFromDate(LocalDate date)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        return date.format(formatter);
    }

    private String monthFromDate(LocalDate date)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM");
        return date.format(formatter);
    }

    private String yearFromDate(LocalDate date)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
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

    @Override
    public void onItemClick(int position, String dayText)
    {
        if(!dayText.equals(""))
        {
            SummaryFragment fragment = SummaryFragment.GetInstance();
            fragment.saveBitmap();
            DayofMonth = dayText;
            String message = "Selected Date " + DayofMonth + " " + monthYearFromDate(selectedDate);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            fragment.loadBitmap();
            fragment.redrawSurface();

        }
    }

    private void generatePDF(File file) {

        PdfDocument pdfDocument = new PdfDocument();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String numPagesString = sharedPref.getString("num_pages", "2");

        int numPages = 2;
        try {
            numPages = Integer.parseInt(numPagesString);
        }
        catch (Exception e)
        {
            Toast.makeText(MainActivity.this, "Unable to parse num pages value " + numPagesString + ". Using default value of 2.", Toast.LENGTH_LONG).show();
        }



        int pageHeight = 2200;
        int pageWidth = 1650;

        int linesStart = 140;
        int linesStop = pageHeight;
        int lineHeight = 60;


        PdfDocument.PageInfo myPageInfo;
        PdfDocument.Page startPage;

        // setup first page
        myPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        startPage = pdfDocument.startPage(myPageInfo);

        Paint title = new Paint();

        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        title.setTextSize(40);
        title.setColor(ContextCompat.getColor(this, R.color.black));

        Canvas canvas;
        canvas = startPage.getCanvas();
        canvas.drawText(DayofMonth + " " + monthFromDate(selectedDate)+ " " + yearFromDate(selectedDate), 110, 135, title);
        Paint paint;
        paint = new Paint();

        paint.setColor(ContextCompat.getColor(this, R.color.gray));

        canvas.drawLine(100, 0,100,pageHeight, paint);
        for (int j = linesStart; j <= linesStop; j += lineHeight) {
            canvas.drawLine(0, j, pageWidth, j, paint);
        }

        pdfDocument.finishPage(startPage);

        // add lines to other pages
        for (int i = 1; i < numPages; i++){
            myPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i+1).create();
            startPage = pdfDocument.startPage(myPageInfo);

            canvas = startPage.getCanvas();
            paint = new Paint();
            paint.setColor(ContextCompat.getColor(this, R.color.gray));
            canvas.drawLine(100, 0,100,pageHeight, paint);


            for (int j = linesStart; j <= linesStop; j += lineHeight) {
                canvas.drawLine(0, j, pageWidth, j, paint);
            }

            pdfDocument.finishPage(startPage);

        }



        try {
            // after creating a file name we will
            // write our PDF file to that location.
            pdfDocument.writeTo(new FileOutputStream(file));

            // below line is to print toast message
            // on completion of PDF generation.
            Toast.makeText(MainActivity.this, "PDF file generated successfully.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            // below line is used
            // to handle error
            e.printStackTrace();
        }
        // after storing our pdf to that
        // location we are closing our PDF file.
        pdfDocument.close();
    }

    public void addRect(Rect limit)
    {
        Log.d(TAG, "addRect ");
        limitRectList.add(limit);
        if (limitRectList.size() < 2) {
            return;
        }
        touchHelper.setStrokeWidth(STROKE_WIDTH).setLimitRect(limitRectList, new ArrayList<Rect>()).setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER)
                .openRawDrawing();

        touchHelper.setStrokeColor(Color.BLACK);
        touchHelper.setMultiRegionMode();
        touchHelper.setRawDrawingEnabled(true);
        touchHelper.setRawDrawingRenderEnabled(true);
        Log.d(TAG, "setup touchHelper ");




    }
    public RawInputCallback getRawInputCallback() {
        if (rawInputCallback == null) {
            rawInputCallback = new RawInputCallback() {
                @Override
                public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
                    Log.d(TAG, "onBeginRawDrawing");
                    if (writeTasks == true) {
                        TasksFragment fragment = TasksFragment.GetInstance();
                        fragment.points.clear();
                    } else {
                        SummaryFragment fragment = SummaryFragment.GetInstance();
                        fragment.points.clear();
                    }
                }

                @Override
                public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
                    Log.d(TAG, "onEndRawDrawing");
//                    touchHelper.setRawDrawingRenderEnabled(true);
                    lastDraw = currentTimeMillis();
                    if (!redrawRunning) {
                        redrawRunning = true;
                        Runnable thread = new Runnable() {
                            public void run() {
                                long currentTime = currentTimeMillis();
                                while (currentTime < lastDraw + refreshInterval) {
                                    currentTime = currentTimeMillis();
                                }
                                Log.d(TAG, "thread: redrawing");
                                if (writeTasks == true) {
                                    TasksFragment fragment = TasksFragment.GetInstance();
                                    fragment.redrawSurface();
                                } else {
                                    SummaryFragment fragment = SummaryFragment.GetInstance();
                                    fragment.redrawSurface();
                                }
                                redrawRunning = false;

                            }
                        };
                        new Thread(thread).start();    //use start() instead of run()
                    }

                }

                @Override
                public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
                    lastDraw = currentTimeMillis();


                }

                @Override
                public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
                    Log.d(TAG, "onRawDrawingTouchPointListReceived");

                    if (writeTasks == true) {
                        TasksFragment fragment = TasksFragment.GetInstance();
                        fragment.drawScribbleToBitmap(touchPointList.getPoints(), false);
                    } else {
                        SummaryFragment fragment = SummaryFragment.GetInstance();
                        fragment.drawScribbleToBitmap(touchPointList.getPoints(), false);
                    }


                }

                @Override
                public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {

//                    touchHelper.setRawDrawingEnabled(false);
//                    touchHelper.setRawDrawingRenderEnabled(true);

                    if (writeTasks == true) {
                        TasksFragment fragment = TasksFragment.GetInstance();
                        fragment.points.clear();
                        fragment.redrawSurface();

                    } else {
                        SummaryFragment fragment = SummaryFragment.GetInstance();
                        fragment.points.clear();
                        fragment.redrawSurface();
                    }

                }

                @Override
                public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
                    if (writeTasks == true) {
                        TasksFragment fragment = TasksFragment.GetInstance();
                        fragment.redrawSurface();
                    } else {
                        SummaryFragment fragment = SummaryFragment.GetInstance();
                        fragment.redrawSurface();
                    }
                }

                @Override
                public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
                    if (writeTasks == true) {
                        TasksFragment fragment = TasksFragment.GetInstance();
                        fragment.points.add(touchPoint);
                        if (fragment.points.size() >= 100) {
                            List<TouchPoint> pointList = new ArrayList<>(fragment.points);
                            fragment.points.clear();
                            TouchPointList touchPointList = new TouchPointList();
                            for (TouchPoint point : pointList) {
                                touchPointList.add(point);
                            }
                            fragment.drawScribbleToBitmap(pointList, true);

                        }
                    } else {
                        SummaryFragment fragment = SummaryFragment.GetInstance();
                        fragment.points.clear();
                        if (fragment.points.size() >= 100) {
                            List<TouchPoint> pointList = new ArrayList<>(fragment.points);
                            fragment.points.clear();
                            TouchPointList touchPointList = new TouchPointList();
                            for (TouchPoint point : pointList) {
                                touchPointList.add(point);
                            }
                            fragment.drawScribbleToBitmap(pointList, true);

                        }
                    }
                }

                @Override
                public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
                    Log.d(TAG, "onRawErasingTouchPointListReceived");
                    if (writeTasks == true) {
                        TasksFragment fragment = TasksFragment.GetInstance();
                        fragment.drawScribbleToBitmap(touchPointList.getPoints(), true);
                    } else {
                        SummaryFragment fragment = SummaryFragment.GetInstance();
                        fragment.drawScribbleToBitmap(touchPointList.getPoints(), true);
                    }

                }
            };
        }
        return rawInputCallback;
    }
}


