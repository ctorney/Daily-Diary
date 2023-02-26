package com.onyx.dailydiary;

import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
    private final String tasksfilename = "tasks.png";
    private final String filepath = "DailyNotes";

    private Bitmap tasksBitmap;
    private Bitmap summaryBitmap;
    public List<TouchPoint> points = new ArrayList<>();
    private RxManager rxManager;


    private CalendarViewHolder lastHolder = null;
    private boolean redrawRunning = false;
    private boolean rawDrawing = false;

    Paint penPaint;
    Paint eraserPaint;
    private final float STROKE_WIDTH = 4.0f;
    private boolean needsSave = false;
    private long lastDraw = 0;
    private final long refreshInterval = 1000;
    private RawInputCallback rawInputCallback;
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
        initPaint();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d");
        DayofMonth = selectedDate.format(formatter);
        List<BitmapView> viewList = new ArrayList<>();
        viewList.add(binding.taskssurfaceview);
        viewList.add(binding.summarysurfaceview);
        PenCallback penCallback = new PenCallback(this,viewList,false);
        touchHelper = TouchHelper.create(getWindow().getDecorView().getRootView(), penCallback);
        touchHelper.debugLog(false);
        touchHelper.setRawInputReaderEnable(true);
        penCallback.setTouchHelper(touchHelper);

//        mDetector = new GestureDetectorCompat(this,new GestureListener(){
//            @Override
//            public void onSwipeRight() {
//                    selectedDate = selectedDate.minusMonths(1);
//                    setMonthView();
//            }
//
//            @Override
//            public void onSwipeLeft() {
//                selectedDate = selectedDate.plusMonths(1);
//                setMonthView();
//            }
//
//        });

        //            tasksBitmap = safeLoadBitmap(tasksBitmap, tasksfilename, binding.taskssurfaceview, R.drawable.lines);
            String summaryFilename =  getCurrentDateString() + ".png";
//            summaryBitmap = safeLoadBitmap(summaryBitmap, summaryFilename, binding.summarysurfaceview, R.drawable.finelines);

        initSurfaceView(binding.taskssurfaceview,tasksfilename,R.drawable.tasks_bkgrnd);
        initSurfaceView(binding.summarysurfaceview,summaryFilename, R.drawable.summary_bkgrnd);



        Button clear_all = (Button) view.findViewById(R.id.clearsummary);
        clear_all.setOnClickListener(this);
        Button open_diary = (Button) view.findViewById(R.id.opendiary);
        open_diary.setOnClickListener(this);
        Button clear_tasks = (Button) view.findViewById(R.id.clear_tasks);
        clear_tasks.setOnClickListener(this);
        parser = new iCalParser(getApplicationContext());
//        registerReceiver(onCompleteDownload,
//                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        parser.loadCalendars();
        setMonthView();


    }
//    BroadcastReceiver onCompleteDownload=new BroadcastReceiver() {
//        public void onReceive(Context ctxt, Intent intent) {
//            parser.loadCalendars();
//            setMonthView();
//
//        }
//    };
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
        binding.taskssurfaceview.saveBitmap();
//        saveBitmap(tasksBitmap, tasksfilename);
//        String summaryFilename =  getCurrentDateString() + ".png";
//        binding.summarysurfaceview.setFilename(summaryFilename);
        binding.summarysurfaceview.saveBitmap();
//        saveBitmap(summaryBitmap, summaryFilename);
    }
//    @Override
//    public boolean onTouchEvent(MotionEvent event){
//        Log.d(TAG, "onTouchEvent");
//
//        if (this.mDetector.onTouchEvent(event)) {
//            return true;
//        }
//        return super.onTouchEvent(event);
//    }
    @Override
    public void onResume() {

        Log.d(TAG, "onResume");
        super.onResume();
//        parser.loadCalendars();
        startTouchHelper();
//        EpdController.repaintEveryThing(UpdateMode.DU_QUALITY);
//        Runnable thread = () -> {
////            tasksBitmap = safeLoadBitmap(tasksBitmap, tasksfilename, binding.taskssurfaceview, R.drawable.lines);
////            String summaryFilename =  getCurrentDateString() + ".png";
////            summaryBitmap = safeLoadBitmap(summaryBitmap, summaryFilename, binding.summarysurfaceview, R.drawable.finelines);
//            touchHelper.setStrokeWidth(STROKE_WIDTH);
//            touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER);
//            touchHelper.setStrokeColor(Color.BLACK);
//            touchHelper.setLimitRect(limitRectList, new ArrayList<Rect>())
//                    .openRawDrawing();
//            touchHelper.setRawDrawingEnabled(false);
//            touchHelper.setMultiRegionMode();
//            touchHelper.setRawDrawingEnabled(true);
//            touchHelper.enableFingerTouch(true);
//            touchHelper.setRawDrawingRenderEnabled(true);
////            parser.loadCalendars();
//        };
//        new Thread(thread).start();

        return;
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.clear_tasks:
                binding.taskssurfaceview.resetBitmap();
                binding.taskssurfaceview.redrawSurface();
//                tasksBitmap = resetBitmap(tasksBitmap, binding.taskssurfaceview, R.drawable.lines);
//                redrawSurface(tasksBitmap, binding.taskssurfaceview);
                break;
            case R.id.clearsummary:
                binding.summarysurfaceview.resetBitmap();
                binding.summarysurfaceview.redrawSurface();
//                summaryBitmap = resetBitmap(summaryBitmap, binding.summarysurfaceview, R.drawable.finelines);
//                redrawSurface(summaryBitmap, binding.summarysurfaceview);
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
//            String summaryFilename =  getCurrentDateString() + ".png";
//            saveBitmap(summaryBitmap, summaryFilename);

            binding.summarysurfaceview.saveBitmap();

            DayofMonth = dayText;
//            String message = "Selected Date " + DayofMonth + " " + monthYearFromDate(selectedDate);
//            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            String summaryFilename =  getCurrentDateString() + ".png";

            binding.summarysurfaceview.setFilename(summaryFilename);
            binding.summarysurfaceview.redrawSurface();
//            summaryBitmap = loadBitmap(summaryBitmap,summaryFilename,binding.summarysurfaceview, R.drawable.finelines);
//            redrawSurface(summaryBitmap,binding.summarysurfaceview);

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

//                String summaryFilename =  getCurrentDateString() + ".png";
//                binding.summarysurfaceview.setFilename(summaryFilename);
                binding.summarysurfaceview.saveBitmap();
//                saveBitmap(tasksBitmap, tasksfilename);
//                String summaryFilename =  getCurrentDateString() + ".png";
//                saveBitmap(summaryBitmap, summaryFilename);
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
            @Override
            public void onScreenOff() {
                Log.d(TAG, "onScreenOff");
                onPause();
               }
        });
    }




    private void initSurfaceView(BitmapView surfaceView, String filename, int background) {

        surfaceView.setBackground(background);
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

//        Runnable thread = new Runnable()
//        {
//            public void run()
//            {
//                while (calendarAdapter.todayHolder==null)
//                {
////                    Log.d(TAG, "waiting on a holder");
//
//                }
//                lastHolder = calendarAdapter.todayHolder;
////                String summaryFilename =  getCurrentDateString() + ".png";
////                summaryBitmap = loadBitmap(summaryBitmap,summaryFilename,binding.summarysurfaceview, R.drawable.finelines);
////                redrawSurface(summaryBitmap,binding.summarysurfaceview);
//            }
//        };
//        if (selectedDate.getMonthValue()==LocalDate.now().getMonthValue() && selectedDate.getYear()==LocalDate.now().getYear()) {
//            new Thread(thread).start();
//        }

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

//
//
//    public Bitmap resetBitmap(Bitmap bitmap, SurfaceView surfaceView, int background) {
//        Log.d(TAG, "resetBitmap");
//        try {
//            bitmap = null;
//            Drawable drawable = ResourcesCompat.getDrawable(getResources(), background, null);
//            bitmap = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);
//            drawable.setBounds(0, 0, surfaceView.getWidth(), surfaceView.getHeight());
//            Canvas canvas = new Canvas(bitmap);
//            drawable.draw(canvas);
//        }
//        catch (Exception e) {
//            Log.d("resetBitmap Error: ", e.getMessage(), e);
//        }
//        return bitmap;
//    }
//    public void redrawSurface(Bitmap bitmap, SurfaceView surfaceView) {
//        if (!surfaceView.getHolder().getSurface().isValid()){
//            return;
//        }
//        Log.d(TAG, "redrawSurface");
//        touchHelper.setRawDrawingRenderEnabled(false);
//        Canvas lockCanvas = surfaceView.getHolder().lockCanvas();
//        lockCanvas.drawColor(Color.WHITE);
//        lockCanvas.drawBitmap(bitmap, 0, 0, null);
//        surfaceView.getHolder().unlockCanvasAndPost(lockCanvas);
//        touchHelper.setRawDrawingRenderEnabled(true);
//    }
//
//    public Bitmap loadBitmap(Bitmap bitmap, String filename, SurfaceView surfaceView, int background) {
//        try {
//            Log.d(TAG, "loadBitmap");
//            File myExternalFile = new File(getExternalFilesDir(filepath), filename);
//            if (myExternalFile.exists())
//            {
//                BitmapFactory.Options opt = new BitmapFactory.Options();
//                opt.inScaled = true;
//                opt.inMutable = true;
//                bitmap = BitmapFactory.decodeStream(new FileInputStream(myExternalFile),null, opt);
//            }
//            else
//            {
////                bitmap = resetBitmap(bitmap, surfaceView, background);
//
//            }
//        } catch (Exception e) {
//            Log.d("loadBitmap Error: ", e.getMessage(), e);
//        }
//
//        return bitmap;
//    }
//
//    public Bitmap safeLoadBitmap(Bitmap bitmap, String filename, SurfaceView surfaceView, int background) {
//        while (!surfaceView.getHolder().getSurface().isValid()) {
//            // waits for the surface to be drawn
//        }
//        bitmap = loadBitmap(bitmap, filename, surfaceView, background);
//        if (bitmap == null) {
//            bitmap = resetBitmap(bitmap, surfaceView, background);
//        }
//        Canvas lockCanvas = surfaceView.getHolder().lockCanvas();
//        Rect rect = new Rect(0, 0, surfaceView.getWidth(), surfaceView.getHeight());
//        lockCanvas.drawBitmap(bitmap, null, rect, null);
//        surfaceView.getHolder().unlockCanvasAndPost(lockCanvas);
//        return bitmap;
//    }
//
//    public void saveBitmap(Bitmap bitmap, String filename) {
//        Log.d(TAG, "saveBitmap");
//        File myExternalFile = new File(getExternalFilesDir(filepath), filename);
//        try {
//            FileOutputStream fos =  new FileOutputStream(myExternalFile);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
//            fos.close();
//        } catch (Exception e) {
//            Log.d("SAVE_IMAGE", e.getMessage(), e);
//        }
//    }
//
//    public void drawToBitmap(Bitmap bitmap, SurfaceView surfaceView, int background, List<TouchPoint> list) {
//
//        Canvas canvas = new Canvas(bitmap);
//        Rect limit = new Rect();
//        Point offset = new Point();
//        surfaceView.getGlobalVisibleRect(limit,offset);
//
//        Path path = new Path();
////
////        for (TouchPointList pointList : pointLists) {
////            List<TouchPoint> list = pointList.getRenderPoints();
//
//
//            PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
//            path.moveTo(prePoint.x-offset.x, prePoint.y-offset.y);
//            for (TouchPoint point : list) {
//                path.quadTo(prePoint.x-offset.x, prePoint.y-offset.y, point.x-offset.x, point.y-offset.y);
//                prePoint.x = point.x;
//                prePoint.y = point.y;
//            }
////        }
//
//        canvas.drawPath(path, penPaint);
//
////        Drawable drawable = ResourcesCompat.getDrawable(getResources(), background, null);
////        drawable.setBounds(0, 0,surfaceView.getWidth(), surfaceView.getHeight());
////        drawable.draw(canvas);
//
//    }
//
//    public void eraseBitmap(Bitmap bitmap, SurfaceView surfaceView, int background, List<TouchPoint> list) {
//
//        Canvas canvas = new Canvas(bitmap);
//        Rect limit = new Rect();
//        Point offset = new Point();
//        surfaceView.getGlobalVisibleRect(limit,offset);
//
//        Path path = new Path();
//        PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
//        path.moveTo(prePoint.x-offset.x, prePoint.y-offset.y);
//        for (TouchPoint point : list) {
//            path.quadTo(prePoint.x-offset.x, prePoint.y-offset.y, point.x-offset.x, point.y-offset.y);
//            prePoint.x = point.x;
//            prePoint.y = point.y;
//        }
//
//        canvas.drawPath(path, eraserPaint);
//
//        Drawable drawable = ResourcesCompat.getDrawable(getResources(), background, null);
//        drawable.setBounds(0, 0,surfaceView.getWidth(), surfaceView.getHeight());
//        drawable.draw(canvas);
//
//    }
//
//
//    public RawInputCallback getRawInputCallback() {
//        if (rawInputCallback == null) {
//            rawInputCallback = new RawInputCallback() {
//                @Override
//                public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
//                    Log.d(TAG, "onBeginRawDrawing");
//                    points.clear();
//                }
//
//                @Override
//                public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
//                    Log.d(TAG, "onEndRawDrawing");
//
//                    lastDraw = currentTimeMillis();
//                    if (!redrawRunning) {
//                        redrawRunning = true;
//                        Runnable thread = () -> {
//                            long currentTime = currentTimeMillis();
//                            while (currentTime < lastDraw + refreshInterval) {
//                                currentTime = currentTimeMillis();
//                            }
//                            touchHelper.setRawDrawingEnabled(false);
//                            touchHelper.setRawDrawingEnabled(true);
//                            Log.d(TAG, "thread: redrawing");
////
////                                redrawSurface(tasksBitmap, binding.taskssurfaceview);
////                                redrawSurface(summaryBitmap, binding.summarysurfaceview);
//
//                            redrawRunning = false;
//
//                        };
//                        new Thread(thread).start();
//                    }
//
//                }
//
//                @Override
//                public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
//                    lastDraw = currentTimeMillis();
//                }
//
//                @Override
//                public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
//                    binding.taskssurfaceview.drawToBitmap(touchPointList.getPoints());
//                    binding.summarysurfaceview.drawToBitmap(touchPointList.getPoints());
////                    drawToBitmap(tasksBitmap,binding.taskssurfaceview, R.drawable.lines,touchPointList.getPoints());
////                    drawToBitmap(summaryBitmap,binding.summarysurfaceview, R.drawable.finelines,touchPointList.getPoints());
//
//
//                }
//
//                @Override
//                public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
//
//                    points.clear();
//                    binding.taskssurfaceview.redrawSurface();
//                    binding.summarysurfaceview.redrawSurface();
////                    redrawSurface(summaryBitmap, binding.summarysurfaceview);
//                }
//
//                @Override
//                public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
////                    redrawSurface(tasksBitmap, binding.taskssurfaceview);
////                    redrawSurface(summaryBitmap, binding.summarysurfaceview);
//                    binding.taskssurfaceview.redrawSurface();
//                    binding.summarysurfaceview.redrawSurface();
//                }
//
//                @Override
//                public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
//                    points.add(touchPoint);
//                    if (points.size() >= 50) {
//                        List<TouchPoint> pointList = new ArrayList<>(points);
//                        points.clear();
//                        TouchPointList touchPointList = new TouchPointList();
//                        for (TouchPoint point : pointList) {
//                            touchPointList.add(point);
//                        }
//                        binding.taskssurfaceview.eraseBitmap(touchPointList.getPoints());
//                        binding.summarysurfaceview.eraseBitmap(touchPointList.getPoints());
////                        redrawSurface(tasksBitmap, binding.taskssurfaceview);
////                        redrawSurface(summaryBitmap, binding.summarysurfaceview);
//                        binding.taskssurfaceview.redrawSurface();
//                        binding.summarysurfaceview.redrawSurface();
//                    }
//
//                }
//
//                @Override
//                public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
//                    Log.d(TAG, "onRawErasingTouchPointListReceived");
//                    binding.taskssurfaceview.eraseBitmap(touchPointList.getPoints());
//                    binding.summarysurfaceview.eraseBitmap(touchPointList.getPoints());
////
////                    eraseBitmap(tasksBitmap,binding.taskssurfaceview, R.drawable.lines, touchPointList.getPoints());
////                    eraseBitmap(summaryBitmap,binding.summarysurfaceview, R.drawable.finelines, touchPointList.getPoints());
//                    binding.taskssurfaceview.redrawSurface();
//                    binding.taskssurfaceview.redrawSurface();
//                }
//
//                @Override
//                public void onPenUpRefresh(RectF refreshRect) {
//                    Log.d(TAG, "onPenUpRefresh " + rawDrawing);
////            // this on and off seems to be important for stopping the thread - but also may cause the writing pause
////            if (!rawDrawing) {
////                touchHelper.setRawDrawingEnabled(false);
////                touchHelper.setRawDrawingEnabled(true);
////            }
////            touchHelper.
////
//                    getRxManager().enqueue(new PartialRefreshRequest(MainActivity.this, binding.taskssurfaceview, refreshRect)
//                                    .setBitmap(binding.taskssurfaceview.getBitmap()),
//                            new RxCallback<PartialRefreshRequest>() {
//                                @Override
//                                public void onNext(@NonNull PartialRefreshRequest partialRefreshRequest) {
//                                }
//                            });
//                    getRxManager().enqueue(new PartialRefreshRequest(MainActivity.this, binding.summarysurfaceview, refreshRect)
//                                    .setBitmap(binding.summarysurfaceview.getBitmap()),
//                            new RxCallback<PartialRefreshRequest>() {
//                                @Override
//                                public void onNext(@NonNull PartialRefreshRequest partialRefreshRequest) {
//                                    Log.d(TAG, "onNext " );
//
//                                }
//                            });
//                }
//            };
//        }
//        return rawInputCallback;
//    }
//
//    private RxManager getRxManager() {
//        if (rxManager == null) {
//            RxManager.Builder.initAppContext(this);
//            rxManager = RxManager.Builder.sharedSingleThreadManager();
//        }
//        return rxManager;
//    }

}


