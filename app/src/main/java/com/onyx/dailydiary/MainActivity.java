package com.onyx.dailydiary;

import static java.lang.System.currentTimeMillis;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
// import android.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;
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
import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateMode;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.pen.data.TouchPointList;
import com.onyx.android.sdk.rx.RxManager;
import com.onyx.dailydiary.databinding.ActivityMainBinding;
import com.onyx.dailydiary.databinding.ActivityWriterBinding;

public class MainActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener, View.OnClickListener
{
    private ActivityMainBinding binding;
    private TextView monthText;
    private RecyclerView calendarRecyclerView;
    private LocalDate selectedDate;
    public TouchHelper touchHelper;
    private final String tasksfilename = "tasks.png";
    private Bitmap tasksBitmap;
    private Bitmap summaryBitmap;
    public List<TouchPoint> points = new ArrayList<>();
    private List<TouchPointList> pointLists = new ArrayList<>();


    private CalendarViewHolder lastHolder = null;
    private boolean redrawRunning = false;
    Paint penPaint;
    Paint eraserPaint;
    private final float STROKE_WIDTH = 4.0f;
    private boolean needsSave = false;
    private long lastDraw = 0;
    private long refreshInterval = 1000;
    private RawInputCallback rawInputCallback;
    private String DayofMonth;
    private List<Rect> limitRectList = new ArrayList<>();
    private static final String TAG = MainActivity.class.getSimpleName();

    private GlobalDeviceReceiver deviceReceiver = new GlobalDeviceReceiver();
    private RxManager rxManager;
    private String filepath = "DailyNotes";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        deviceReceiver.enable(this, true);
        View view = binding.getRoot();
        setContentView(view);
        initWidgets();
        initReceiver();
        initPaint();
        selectedDate = LocalDate.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d");
        DayofMonth = selectedDate.format(formatter);

        setMonthView();
        touchHelper = TouchHelper.create(getWindow().getDecorView().getRootView(), getRawInputCallback());


        initSurfaceView(binding.taskssurfaceview);
        initSurfaceView(binding.summarysurfaceview);

        Button clear_all = (Button) view.findViewById(R.id.clearsummary);
        clear_all.setOnClickListener(this);
        Button open_diary = (Button) view.findViewById(R.id.opendiary);
        open_diary.setOnClickListener(this);

        Button clear_tasks = (Button) view.findViewById(R.id.clear_tasks);
        clear_tasks.setOnClickListener(this);

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
        saveBitmap(tasksBitmap, tasksfilename);
        String summaryFilename =  getCurrentDateString() + ".png";
        saveBitmap(summaryBitmap, summaryFilename);
    }

    @Override
    public void onResume() {

        Log.d(TAG, "onResume");
        super.onResume();
        View view = binding.getRoot();
        EpdController.repaintEveryThing(UpdateMode.DU_QUALITY);
        Runnable thread = new Runnable()
        {
            public void run()
            {
                tasksBitmap = safeLoadBitmap(tasksBitmap, tasksfilename, binding.taskssurfaceview, R.drawable.lines);
                String summaryFilename =  getCurrentDateString() + ".png";
                summaryBitmap = safeLoadBitmap(summaryBitmap, summaryFilename, binding.summarysurfaceview, R.drawable.finelines);
                touchHelper.setStrokeWidth(STROKE_WIDTH);
                touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER);
                touchHelper.setStrokeColor(Color.BLACK);
                touchHelper.setLimitRect(limitRectList, new ArrayList<Rect>())
                        .openRawDrawing();
                touchHelper.setRawDrawingEnabled(false);
                touchHelper.setMultiRegionMode();
                touchHelper.setRawDrawingEnabled(true);
                touchHelper.enableFingerTouch(true);
                touchHelper.setRawDrawingRenderEnabled(true);
            }
        };
        new Thread(thread).start();

        return;
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.clear_tasks:
                tasksBitmap = resetBitmap(tasksBitmap, binding.taskssurfaceview, R.drawable.lines);
                redrawSurface(tasksBitmap, binding.taskssurfaceview);
                break;
            case R.id.clearsummary:
                summaryBitmap = resetBitmap(summaryBitmap, binding.summarysurfaceview, R.drawable.finelines);
                redrawSurface(summaryBitmap, binding.summarysurfaceview);
                break;
            case R.id.opendiary:
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
            String summaryFilename =  getCurrentDateString() + ".png";
            saveBitmap(summaryBitmap, summaryFilename);
            DayofMonth = dayText;
//            String message = "Selected Date " + DayofMonth + " " + monthYearFromDate(selectedDate);
//            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            summaryFilename =  getCurrentDateString() + ".png";
            summaryBitmap = loadBitmap(summaryBitmap,summaryFilename,binding.summarysurfaceview, R.drawable.finelines);
            redrawSurface(summaryBitmap,binding.summarysurfaceview);

            if (lastHolder!=null){
                lastHolder.layout.setBackgroundColor(Color.WHITE);
                lastHolder.dayOfMonth.setTextColor(Color.BLACK);
            }
            holder.layout.setBackgroundColor(Color.DKGRAY);
            holder.dayOfMonth.setTextColor(Color.WHITE);
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
                Toast.makeText(this, "Calendar sync is not yet implemented.", Toast.LENGTH_SHORT)
                        .show();
                break;
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

    private void initReceiver() {
        deviceReceiver.setSystemNotificationPanelChangeListener(new GlobalDeviceReceiver.SystemNotificationPanelChangeListener() {
            @Override
            public void onNotificationPanelChanged(boolean open) {
                touchHelper.setRawDrawingEnabled(!open);
                Log.d(TAG, "onNotificationPanelChanged " + open);
                saveBitmap(tasksBitmap, tasksfilename);
                String summaryFilename =  getCurrentDateString() + ".png";
                saveBitmap(summaryBitmap, summaryFilename);
//                renderToScreen(binding.surfaceview, bitmap);
            }
        }).setSystemScreenOnListener(new GlobalDeviceReceiver.SystemScreenOnListener() {
            @Override
            public void onScreenOn() {
                Log.d(TAG, "onScreenOn");
                selectedDate = LocalDate.now();
                setMonthView();
//                renderToScreen(binding.surfaceview, bitmap);
            }
        });
    }




    private void initSurfaceView(final SurfaceView surfaceView) {


        surfaceView.setBackgroundColor(Color.WHITE);
        surfaceView.setZOrderOnTop(true);
        surfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Rect limit = new Rect();
                surfaceView.getGlobalVisibleRect(limit);
                limitRectList.add(limit);
                startTouchHelper(limitRectList);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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

    private void initPaint(){

        penPaint = new Paint();
        penPaint.setAntiAlias(true);
        penPaint.setDither(true);
        penPaint.setColor(Color.BLACK);
        penPaint.setStyle(Paint.Style.STROKE);
        penPaint.setStrokeJoin(Paint.Join.ROUND);
        penPaint.setStrokeCap(Paint.Cap.ROUND);
        penPaint.setStrokeWidth(STROKE_WIDTH);

        eraserPaint = new Paint();
        eraserPaint.setAntiAlias(true);
        eraserPaint.setDither(true);
        eraserPaint.setColor(Color.WHITE);
        eraserPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        eraserPaint.setStrokeJoin(Paint.Join.ROUND);
        eraserPaint.setStrokeCap(Paint.Cap.SQUARE);
        eraserPaint.setStrokeWidth(10*STROKE_WIDTH);
    }

    private void startTouchHelper(List<Rect> limitRectList) {
        if (limitRectList.size() < 2) {
            return;
        }
        touchHelper.setStrokeWidth(STROKE_WIDTH);
        touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER);
        touchHelper.setStrokeColor(Color.BLACK);
        touchHelper.setLimitRect(limitRectList, new ArrayList<Rect>())
                .openRawDrawing();

        touchHelper.setRawDrawingEnabled(false);
        touchHelper.setMultiRegionMode();
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
        String filename =  DayofMonth + "-" + monthYearFromDate(selectedDate);
        return filename;
    }

    private void setMonthView()
    {
        monthText.setText(monthFromDate(selectedDate));
        ArrayList<String> daysInMonth = daysInMonthArray(selectedDate);
        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth, selectedDate, this);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 7);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);
        Runnable thread = new Runnable()
        {
            public void run()
            {
                while (calendarAdapter.todayHolder==null)
                {
                    Log.d(TAG, "waiting on a holder");

                }
                lastHolder = calendarAdapter.todayHolder;
//                String summaryFilename =  getCurrentDateString() + ".png";
//                summaryBitmap = loadBitmap(summaryBitmap,summaryFilename,binding.summarysurfaceview, R.drawable.finelines);
//                redrawSurface(summaryBitmap,binding.summarysurfaceview);
            }
        };
        if (selectedDate.getMonthValue()==LocalDate.now().getMonthValue() && selectedDate.getYear()==LocalDate.now().getYear()) {
            new Thread(thread).start();
        }

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



    public Bitmap resetBitmap(Bitmap bitmap, SurfaceView surfaceView, int background) {
        Log.d(TAG, "resetBitmap");
        try {
            bitmap = null;
            Drawable drawable = ResourcesCompat.getDrawable(getResources(), background, null);
            bitmap = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, surfaceView.getWidth(), surfaceView.getHeight());
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
        }
        catch (Exception e) {
            Log.d("resetBitmap Error: ", e.getMessage(), e);
        }
        return bitmap;
    }
    public void redrawSurface(Bitmap bitmap, SurfaceView surfaceView) {
        if (!surfaceView.getHolder().getSurface().isValid()){
            return;
        }
        Log.d(TAG, "redrawSurface");
        touchHelper.setRawDrawingRenderEnabled(false);
        Canvas lockCanvas = surfaceView.getHolder().lockCanvas();
        lockCanvas.drawColor(Color.WHITE);
        lockCanvas.drawBitmap(bitmap, 0, 0, null);
        surfaceView.getHolder().unlockCanvasAndPost(lockCanvas);
        touchHelper.setRawDrawingRenderEnabled(true);
    }

    public Bitmap loadBitmap(Bitmap bitmap, String filename, SurfaceView surfaceView, int background) {
        try {
            Log.d(TAG, "loadBitmap");
            File myExternalFile = new File(getExternalFilesDir(filepath), filename);
            if (myExternalFile.exists())
            {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inScaled = true;
                opt.inMutable = true;
                bitmap = BitmapFactory.decodeStream(new FileInputStream(myExternalFile),null, opt);
            }
            else
            {
                bitmap = resetBitmap(bitmap, surfaceView, background);

            }
        } catch (Exception e) {
            Log.d("loadBitmap Error: ", e.getMessage(), e);
        }

        return bitmap;
    }

    public Bitmap safeLoadBitmap(Bitmap bitmap, String filename, SurfaceView surfaceView, int background) {
        while (!surfaceView.getHolder().getSurface().isValid()) {
            // waits for the surface to be drawn
        }
        bitmap = loadBitmap(bitmap, filename, surfaceView, background);
        if (bitmap == null) {
            bitmap = resetBitmap(bitmap, surfaceView, background);
        }
        Canvas lockCanvas = surfaceView.getHolder().lockCanvas();
        Rect rect = new Rect(0, 0, surfaceView.getWidth(), surfaceView.getHeight());
        lockCanvas.drawBitmap(bitmap, null, rect, null);
        surfaceView.getHolder().unlockCanvasAndPost(lockCanvas);
        return bitmap;
    }

    public void saveBitmap(Bitmap bitmap, String filename) {
        Log.d(TAG, "saveBitmap");
        File myExternalFile = new File(getExternalFilesDir(filepath), filename);
        try {
            FileOutputStream fos =  new FileOutputStream(myExternalFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.d("SAVE_IMAGE", e.getMessage(), e);
        }
    }

    public void drawToBitmap(Bitmap bitmap, SurfaceView surfaceView, int background) {

        Canvas canvas = new Canvas(bitmap);
        Rect limit = new Rect();
        Point offset = new Point();
        surfaceView.getGlobalVisibleRect(limit,offset);

        Path path = new Path();

        for (TouchPointList pointList : pointLists) {
            List<TouchPoint> list = pointList.getRenderPoints();


            PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
            path.moveTo(prePoint.x-offset.x, prePoint.y-offset.y);
            for (TouchPoint point : list) {
                path.quadTo(prePoint.x-offset.x, prePoint.y-offset.y, point.x-offset.x, point.y-offset.y);
                prePoint.x = point.x;
                prePoint.y = point.y;
            }
        }

        canvas.drawPath(path, penPaint);

        Drawable drawable = ResourcesCompat.getDrawable(getResources(), background, null);
        drawable.setBounds(0, 0,surfaceView.getWidth(), surfaceView.getHeight());
        drawable.draw(canvas);

    }

    public void eraseBitmap(Bitmap bitmap, SurfaceView surfaceView, int background, List<TouchPoint> list) {

        Canvas canvas = new Canvas(bitmap);
        Rect limit = new Rect();
        Point offset = new Point();
        surfaceView.getGlobalVisibleRect(limit,offset);

        Path path = new Path();
        PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
        path.moveTo(prePoint.x-offset.x, prePoint.y-offset.y);
        for (TouchPoint point : list) {
            path.quadTo(prePoint.x-offset.x, prePoint.y-offset.y, point.x-offset.x, point.y-offset.y);
            prePoint.x = point.x;
            prePoint.y = point.y;
        }

        canvas.drawPath(path, eraserPaint);

        Drawable drawable = ResourcesCompat.getDrawable(getResources(), background, null);
        drawable.setBounds(0, 0,surfaceView.getWidth(), surfaceView.getHeight());
        drawable.draw(canvas);

    }


    public RawInputCallback getRawInputCallback() {
        if (rawInputCallback == null) {
            rawInputCallback = new RawInputCallback() {
                @Override
                public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
                    Log.d(TAG, "onBeginRawDrawing");
                    points.clear();
                }

                @Override
                public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
                    Log.d(TAG, "onEndRawDrawing");
                    touchHelper.setRawDrawingEnabled(false);
                    touchHelper.setRawDrawingEnabled(true);
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
                                drawToBitmap(tasksBitmap,binding.taskssurfaceview, R.drawable.lines);
                                drawToBitmap(summaryBitmap,binding.summarysurfaceview, R.drawable.finelines);
                                pointLists.clear();

                                redrawSurface(tasksBitmap, binding.taskssurfaceview);
                                redrawSurface(summaryBitmap, binding.summarysurfaceview);

                                redrawRunning = false;

                            }
                        };
                        new Thread(thread).start();
                    }

                }

                @Override
                public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
                    lastDraw = currentTimeMillis();
                }

                @Override
                public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
                    pointLists.add(touchPointList);

                }

                @Override
                public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
                    points.clear();
                    redrawSurface(tasksBitmap, binding.taskssurfaceview);
                    redrawSurface(summaryBitmap, binding.summarysurfaceview);
                }

                @Override
                public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
                    redrawSurface(tasksBitmap, binding.taskssurfaceview);
                    redrawSurface(summaryBitmap, binding.summarysurfaceview);
                }

                @Override
                public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
                    points.add(touchPoint);
                    if (points.size() >= 100) {
                        List<TouchPoint> pointList = new ArrayList<>(points);
                        points.clear();
                        TouchPointList touchPointList = new TouchPointList();
                        for (TouchPoint point : pointList) {
                            touchPointList.add(point);
                        }
                        eraseBitmap(tasksBitmap,binding.taskssurfaceview, R.drawable.lines, touchPointList.getPoints());
                        eraseBitmap(summaryBitmap,binding.summarysurfaceview, R.drawable.finelines, touchPointList.getPoints());

                    }

                }

                @Override
                public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
                    Log.d(TAG, "onRawErasingTouchPointListReceived");
                    eraseBitmap(tasksBitmap,binding.taskssurfaceview, R.drawable.lines, touchPointList.getPoints());
                    eraseBitmap(summaryBitmap,binding.summarysurfaceview, R.drawable.finelines, touchPointList.getPoints());

                }
            };
        }
        return rawInputCallback;
    }

}


