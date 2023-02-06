package com.onyx.dailydiary;

import static java.lang.Integer.max;
import static java.lang.System.currentTimeMillis;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.pen.data.TouchPointList;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.Touch;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.onyx.dailydiary.databinding.ActivityWriterBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WriterActivity extends AppCompatActivity implements View.OnClickListener{

    private ActivityWriterBinding binding;
    private static final String TAG = WriterActivity.class.getSimpleName();

    private TouchHelper touchHelper;
    private final float STROKE_WIDTH = 4.0f;
    private boolean redrawRunning = false;
    private String filepath = "DailyDiary";
    private String filename;
    public Bitmap bitmap;
    private Paint mPaint = new Paint();
    private Paint erasePaint = new Paint();

//    SurfaceWriter surfaceWriter;
    private boolean needsSave = false;
    private long lastDraw = 0;
    private long refreshInterval = 1000;
    public List<TouchPoint> points = new ArrayList<>();
    String currentdatestring;
    int daypage;
    int daypageCount;
    TextView datebox;
    Date currentdate;
    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writer);
        getSupportActionBar().hide();

        binding = DataBindingUtil.setContentView(this, R.layout.activity_writer);
        initSurfaceView(); //debug
        initPaint();

        currentdatestring = getIntent().getStringExtra("date-string");
        try {
            currentdate=new SimpleDateFormat("dd-MMMM-yyyy").parse(currentdatestring);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        daypage = 1;
        daypageCount = countDayPages();

        datebox = (TextView)findViewById(R.id.date_text);
        datebox.setText(new SimpleDateFormat("EEEE, d MMMM yyyy (").format(currentdate) + String.valueOf(daypage) + "/" + String.valueOf(daypageCount)+")");

        filename = new SimpleDateFormat("yyyyMMdd-").format(currentdate) + String.valueOf(daypage) + ".png";


//        surfaceWriter = ((MainActivity)).getSurfaceWriter();//SurfaceWriter.GetInstance();

        ImageButton back_button = (ImageButton) findViewById(R.id.back_button);
        back_button.setOnClickListener(this);

        ImageButton nextpage_button = (ImageButton) findViewById(R.id.nextpage);
        nextpage_button.setOnClickListener(this);

        ImageButton prevpage_button = (ImageButton) findViewById(R.id.prevpage);
        prevpage_button.setOnClickListener(this);

        ImageButton addpage_button = (ImageButton) findViewById(R.id.addpage);
        addpage_button.setOnClickListener(this);

        ImageButton deletepage_button = (ImageButton) findViewById(R.id.deletepage);
        deletepage_button.setOnClickListener(this);

        ImageButton savepage_button = (ImageButton) findViewById(R.id.save);
        savepage_button.setOnClickListener(this);
    }

    private int countDayPages()
    {
        File dir = getExternalFilesDir(filepath);

        File [] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(new SimpleDateFormat("yyyyMMdd").format(currentdate));
            }
        });

        return max(files.length,1);
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.back_button:
                onBackPressed();
                break;
            case R.id.nextpage:
                updatePage(true);
                break;
            case R.id.prevpage:
                updatePage(false);
                break;
            case R.id.addpage:
                addPage();
                break;
            case R.id.deletepage:
                deletePage();
                break;
            case R.id.save:
                savePages();
                break;


        }
        Log.d(TAG, "Writer onClick");

    }
    public void onResume() {
//        initSurfaceView();
//        touchHelper.setRawDrawingRenderEnabled(true);
        Log.d(TAG, "onResume");
        super.onResume();
        Runnable thread = new Runnable()
        {
            public void run()
            {
                safeLoadBitmap(); //debug
                redrawSurface(); //debug
                Rect limit = new Rect();
                binding.writerview.getLocalVisibleRect(limit);
//                surfaceWriter.startWriter(limit);
                touchHelper.setLimitRect(limit, new ArrayList<Rect>())
                        .openRawDrawing();
                touchHelper.setRawDrawingEnabled(false);
                touchHelper.setSingleRegionMode();
                touchHelper.setRawDrawingEnabled(true);
                touchHelper.enableFingerTouch(true);
                touchHelper.setRawDrawingRenderEnabled(true);

                Log.d(TAG, "Thread complete");

            }
        };
        Log.d(TAG, "start thread");
        new Thread(thread).start();    //use start() instead of run()
        Log.d(TAG, "returning");
        return;



    }



    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
//        touchHelper.closeRawDrawing();
        //touchHelper.setRawDrawingEnabled(false);
//        touchHelper.setRawDrawingRenderEnabled(false);
//        touchHelper.closeRawDrawing();

        super.onPause();
        saveBitmap();
//        surfaceWriter.stopWriter();
        touchHelper.setRawDrawingEnabled(false);
        touchHelper.setRawDrawingRenderEnabled(false);
        touchHelper.closeRawDrawing();

    }
    public void onDestroy(){
        Log.d(TAG, "onDestroy");
//        touchHelper.closeRawDrawing();
//        touchHelper.setRawDrawingEnabled(false);
//        touchHelper.setRawDrawingRenderEnabled(false);
//        touchHelper = null;
//        redrawSurface();
//        touchHelper.setRawDrawingRenderEnabled(false);
        super.onDestroy();
        saveBitmap();



    }
    private void initSurfaceView() {

        binding.writerview.setBackgroundColor(Color.WHITE);
        binding.writerview.setZOrderOnTop(true);
        binding.writerview.getHolder().setFormat(PixelFormat.TRANSPARENT);

//        surfaceWriter = new SurfaceWriter(binding.writerview, this);

        touchHelper = TouchHelper.create(binding.writerview, callback); // debug
//        surfaceWriter.bindView();
        binding.writerview.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int
                    oldRight, int oldBottom) {
                Log.d(TAG, "- onLayoutChange");

                Rect limit = new Rect();
                binding.writerview.getLocalVisibleRect(limit);
//                surfaceWriter.startWriter(limit);
                touchHelper.setLimitRect(limit, new ArrayList<Rect>())
                        .openRawDrawing();
                touchHelper.setRawDrawingEnabled(false);
                touchHelper.setSingleRegionMode();
                touchHelper.setRawDrawingEnabled(true);
                touchHelper.enableFingerTouch(true);
                touchHelper.setRawDrawingRenderEnabled(true);


//                // debug
//                touchHelper.setStrokeWidth(STROKE_WIDTH).setLimitRect(limit,null).setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER)
//                        .openRawDrawing();
//
//                touchHelper.setStrokeColor(Color.BLACK);
//                touchHelper.setRawDrawingEnabled(true);
//                touchHelper.setRawDrawingRenderEnabled(true);
//                touchHelper.setSingleRegionMode();
//                touchHelper.enableFingerTouch(true);

                binding.writerview.addOnLayoutChangeListener(this);
            }
        });


        binding.writerview.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "surfaceView.setOnTouchListener - onTouch::action - " + event.getAction());
                Log.d(TAG, "width height " + binding.writerview.getWidth() + " " + binding.writerview.getHeight());

                return true;
            }
        });

        final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");


            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");holder.removeCallback(this);
            }
        };
        binding.writerview.getHolder().addCallback(surfaceCallback);
    }

    public void pointsHandler(){
        Log.d(TAG,  "pointsHandler");

    }

    private void initPaint(){

//        path = new Path();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(STROKE_WIDTH);

        erasePaint = new Paint();
        erasePaint.setAntiAlias(true);
        erasePaint.setDither(true);
        erasePaint.setColor(Color.WHITE);
        erasePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setStrokeCap(Paint.Cap.SQUARE);
        erasePaint.setStrokeWidth(10*STROKE_WIDTH);
//
    }

    private void updatePage(boolean forward)
    {
        saveBitmap();

        // convert date to calendar
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentdate);

        if (forward){
            if (daypage < daypageCount){
                daypage++;
            }
            else{
                daypage=1;
                calendar.add(Calendar.DATE, 1); //same with c.add(Calendar.DAY_OF_MONTH, 1);

                currentdate = calendar.getTime();
                daypageCount = countDayPages();
            }

        }
        else {
            if (daypage > 1){
                daypage--;
            }
            else{
                // manipulate date
                calendar.add(Calendar.DATE, -1); //same with c.add(Calendar.DAY_OF_MONTH, 1);

                currentdate = calendar.getTime();
                daypageCount = countDayPages();
                daypage=daypageCount;
            }
        }


        datebox = (TextView)findViewById(R.id.date_text);
        datebox.setText(new SimpleDateFormat("EEEE, d MMMM yyyy (").format(currentdate) + String.valueOf(daypage) + "/" + String.valueOf(daypageCount)+")");

        filename = new SimpleDateFormat("yyyyMMdd-").format(currentdate) + String.valueOf(daypage) + ".png";

        safeLoadBitmap();


    }

    private void addPage(){

        needsSave = true;
        saveBitmap();
        daypageCount++;
        updatePage(true);

    }

    private void deletePage(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Confirm delete");
        builder.setMessage("You are about to permanently delete the current page. Are you sure?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog

                int deletePage = daypage;

                try {
                    File externalFile;
                    externalFile = new File(getExternalFilesDir(filepath), filename);
                    if (externalFile.exists())
                    {
                        externalFile.delete();
                    }

                    for (int i = daypage; i < daypageCount; i++) {
                        System.out.println(i);
                        String newfilename = new SimpleDateFormat("yyyyMMdd-").format(currentdate) + String.valueOf(i) + ".png";

                        String oldfilename = new SimpleDateFormat("yyyyMMdd-").format(currentdate) + String.valueOf(i+1) + ".png";
                        externalFile = new File(getExternalFilesDir(filepath), oldfilename);
                        File newExternalFile = new File(getExternalFilesDir(filepath), newfilename);
                        if (externalFile.exists())
                        {
                            externalFile.renameTo(newExternalFile);

                        }

                    }
                    daypageCount--;
                    needsSave=false;
                    updatePage(false);
                    if (deletePage==1){
                        updatePage(true);
                    }
                } catch (Exception e) {
                    Log.d("loadBitmap Error: ", e.getMessage(), e);
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    private void savePages(){

        saveBitmap();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MaterialThemeDialog);
        String item[] = { "Day", "Month", "Year"};

        final int[] timeframe = {1};
        builder.setTitle("Select time interval for export")
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setSingleChoiceItems(item, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                timeframe[0] = i;
                                Toast.makeText(WriterActivity.this, "Item " + i, Toast.LENGTH_SHORT).show();
                            }


                        })
                // Set the action buttons
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the selectedItems results somewhere
                        // or return them to the component that opened the dialog
                        writeToPDF(timeframe[0]);
                        dialog.dismiss();

                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();

                    }
                });

//        builder.setMultiChoiceItems()
        AlertDialog alert = builder.create();
        alert.setOnShowListener(arg0 -> {
            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        });
        alert.show();


    }

    private RawInputCallback callback = new RawInputCallback() {

        @Override
        public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onBeginRawDrawing");
            points.clear();
        }

        @Override
        public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onEndRawDrawing");
//            touchHelper.setRawDrawingRenderEnabled(true);

            lastDraw = currentTimeMillis();
            if (!redrawRunning)
            {
                redrawRunning = true;
                Runnable thread = new Runnable()
                {
                    public void run()
                    {
                        long currentTime = currentTimeMillis();
                        while (currentTime<lastDraw + refreshInterval)
                        {
                            currentTime = currentTimeMillis();
                        }
                        Log.d(TAG, "thread: redrawing");

                        redrawSurface();
                        redrawRunning = false;

                    }
                };
                new Thread(thread).start();    //use start() instead of run()
            }
            touchHelper.setRawDrawingEnabled(false);
            touchHelper.setRawDrawingEnabled(true);
        }

        @Override
        public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
            lastDraw = currentTimeMillis();

        }

        @Override
        public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
            Log.d(TAG, "onRawDrawingTouchPointListReceived");
            drawScribbleToBitmap(touchPointList.getPoints(),false);

        }

        @Override
        public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onBeginRawErasing");
            points.clear();
            redrawSurface();

        }

        @Override
        public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onEndRawErasing");
            points.add(touchPoint);
//            if (points.size() >= 500) {
            List<TouchPoint> pointList = new ArrayList<>(points);
            points.clear();
            TouchPointList touchPointList = new TouchPointList();
            for (TouchPoint point : pointList) {
                touchPointList.add(point);
            }
            drawScribbleToBitmap(pointList,true);
            redrawSurface();


//            redrawSurface();
//            List<TouchPoint> pointList = new ArrayList<>(points);
//            points.clear();
//            TouchPointList touchPointList = new TouchPointList();
//            for (TouchPoint point : pointList) {
//                touchPointList.add(point);
//            }
//            drawScribbleToBitmap(pointList,true);
//            redrawSurface();
        }

        @Override
        public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
            Log.d(TAG, "onRawErasingTouchPointMoveReceived");

            points.add(touchPoint);
            if (points.size() >= 500) {
                List<TouchPoint> pointList = new ArrayList<>(points);
                points.clear();
                TouchPointList touchPointList = new TouchPointList();
                for (TouchPoint point : pointList) {
                    touchPointList.add(point);
                }
                drawScribbleToBitmap(pointList,true);
                redrawSurface();

            }
        }

        @Override
        public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
            Log.d(TAG, "onRawErasingTouchPointListReceived");
            drawScribbleToBitmap(touchPointList.getPoints(),true);
            redrawSurface();

        }
    };


    public void drawScribbleToBitmap(List<TouchPoint> list, boolean eraser) {

        needsSave=true;
        Log.d(TAG, "drawScribbleToBitmap");
        Canvas canvas = new Canvas(bitmap);

        Rect limit = new Rect();
        Point offset = new Point();
        binding.writerview.getGlobalVisibleRect(limit,offset);
//        Log.d(TAG, "drawScribbleToBitmap " + limit + " " + offset);

        Path path = new Path();
        PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
        path.moveTo(prePoint.x, prePoint.y);
        for (TouchPoint point : list) {
            path.quadTo(prePoint.x, prePoint.y, point.x, point.y);
            prePoint.x = point.x;
            prePoint.y = point.y;

//            Log.d(TAG, "drawScribbleToBitmap: " + prePoint.x + " " + prePoint.y);
        }
        if (eraser){
            canvas.drawPath(path, erasePaint);
        }
        else{
            canvas.drawPath(path, mPaint);
        }
//        canvas.drawColor(Color.BLACK);
//        saveBitmap();
//        redrawSurface();
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.pagelines, null);
        drawable.setBounds(0, 0,binding.writerview.getWidth(), binding.writerview.getHeight());
        drawable.draw(canvas);

    }
    public void redrawSurface() {

        if (!binding.writerview.getHolder().getSurface().isValid()){
            return;
        }


        touchHelper.setRawDrawingRenderEnabled(false);// debug
        Log.d(TAG, "redrawSurface");
        Canvas lockCanvas = binding.writerview.getHolder().lockCanvas();
        lockCanvas.drawColor(Color.WHITE);
        lockCanvas.drawBitmap(bitmap, 0, 0, null);
        binding.writerview.getHolder().unlockCanvasAndPost(lockCanvas);
        touchHelper.setRawDrawingRenderEnabled(true); // debug
    }

    public void saveBitmap() {


        Log.d(TAG, "saveBitmap");
        if (needsSave) {
            File myExternalFile = new File(getExternalFilesDir(filepath), filename);
            try {
                FileOutputStream fos = new FileOutputStream(myExternalFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            } catch (Exception e) {
                Log.d("SAVE_IMAGE", e.getMessage(), e);
            }
        }
    }

    public void loadBitmap() {
        try {
            Log.d(TAG, "loadBitmap");
            needsSave = false;
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
                resetBitmap();
            }
        } catch (Exception e) {
            Log.d("loadBitmap Error: ", e.getMessage(), e);
        }
    }

    public void resetBitmap() {
        Log.d(TAG, "resetBitmap");
        try {


            bitmap = null;
            Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.pagelines, null);
            bitmap = Bitmap.createBitmap(binding.writerview.getWidth(), binding.writerview.getHeight(), Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, binding.writerview.getWidth(), binding.writerview.getHeight());
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
        }
        catch (Exception e) {
            Log.d("resetBitmap Error: ", e.getMessage(), e);
        }
        return;
    }

    public void safeLoadBitmap()
    {
//
//        SurfaceHolder holder;
//
        while (!binding.writerview.getHolder().getSurface().isValid()) {

        }
//            holder =
//            if (mHolder != null) {
        loadBitmap();

        if (bitmap == null) {
            resetBitmap();
        }
        redrawSurface();
        return;
//        Canvas lockCanvas = binding.writerview.getHolder().lockCanvas();
//
//        Rect rect = new Rect(0, 0, binding.writerview.getWidth(), binding.writerview.getHeight());
//        lockCanvas.drawBitmap(bitmap, null, rect, null);
//        binding.writerview.getHolder().unlockCanvasAndPost(lockCanvas);


    }

    private void writeToPDF(int timeframe){
        Toast.makeText(this, "exporting to pdf for timeframe " + timeframe, Toast.LENGTH_LONG).show();

    }

}