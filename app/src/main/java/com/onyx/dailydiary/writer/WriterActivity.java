package com.onyx.dailydiary.writer;

import static java.lang.Integer.max;
import static java.lang.System.currentTimeMillis;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.databinding.DataBindingUtil;

import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.pen.data.TouchPointList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.onyx.android.sdk.rx.RxCallback;
import com.onyx.android.sdk.rx.RxManager;
import com.onyx.dailydiary.R;
import com.onyx.dailydiary.databinding.ActivityWriterBinding;
import com.onyx.dailydiary.utils.GestureListener;
import com.onyx.dailydiary.utils.PartialRefreshRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WriterActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = WriterActivity.class.getSimpleName();

    private ActivityWriterBinding binding;

    private GestureDetectorCompat mDetector;

    private TouchHelper touchHelper;
    private final float STROKE_WIDTH = 4.0f;
    private boolean rawDrawing = false;
    private String filepath = "DailyDiary";
    private String filename;
    public Bitmap bitmap;
    private Paint penPaint = new Paint();
    private Paint eraserPaint = new Paint();
    private RxManager rxManager;
    private boolean needsSave = false;
    private boolean redrawRunning = false;
    private long lastDraw = 0;
    private final long refreshInterval = 1000;
    private List<TouchPoint> points = new ArrayList<>();
    private Canvas canvas;

    String currentdatestring;
    int daypage;
    int daypageCount;
    TextView datebox;
    LocalDate currentdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writer);
        getSupportActionBar().hide();

        binding = DataBindingUtil.setContentView(this, R.layout.activity_writer);
        touchHelper = TouchHelper.create(binding.writerview, callback);

        // initialise surface and paints
        initSurfaceView();
        initPaint();

        // setup the gestures
        mDetector = new GestureDetectorCompat(this,new GestureListener(){
            @Override
            public void onSwipeBottom() {
                if (!rawDrawing){
                    deletePage();
                }
            }
            @Override
            public void onSwipeLeft() {
                if (!rawDrawing){
                    updatePage(true);
                }
            }
            @Override
            public void onSwipeRight() {
                if (!rawDrawing){
                    updatePage(false);
                }
            }
            @Override
            public void onSwipeTop() {
                if (!rawDrawing) {
                    addPage();
                }
            }
        });


        // setup the date
        currentdatestring = getIntent().getStringExtra("date-string");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MMMM-yyyy");
        currentdate = LocalDate.parse(currentdatestring,formatter);

        daypage = 1;
        daypageCount = countDayPages();

        datebox = findViewById(R.id.date_text);
        datebox.setText(currentdate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + String.valueOf(daypage) + "/" + String.valueOf(daypageCount)+")");
        filename = currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + daypage + ".png";


        // setup the buttons
        ImageButton back_button = findViewById(R.id.back_button);
        back_button.setOnClickListener(this);

        ImageButton nextpage_button = findViewById(R.id.nextpage);
        nextpage_button.setOnClickListener(this);

        ImageButton prevpage_button = findViewById(R.id.prevpage);
        prevpage_button.setOnClickListener(this);

        ImageButton addpage_button = findViewById(R.id.addpage);
        addpage_button.setOnClickListener(this);

        ImageButton deletepage_button = findViewById(R.id.deletepage);
        deletepage_button.setOnClickListener(this);

        ImageButton savepage_button = findViewById(R.id.save);
        savepage_button.setOnClickListener(this);
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
    }
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        Runnable thread = () -> {
            safeLoadBitmap();
            redrawSurface();
            Rect limit = new Rect();
            binding.writerview.getLocalVisibleRect(limit);
            touchHelper.setStrokeWidth(STROKE_WIDTH);
            touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER);
            touchHelper.setStrokeColor(Color.BLACK);
            touchHelper.setLimitRect(limit, new ArrayList<Rect>())
                    .openRawDrawing();
            touchHelper.setRawDrawingEnabled(false);
            touchHelper.setSingleRegionMode();
            touchHelper.setRawDrawingEnabled(true);
            touchHelper.enableFingerTouch(true);
            touchHelper.setRawDrawingRenderEnabled(true);
            touchHelper.setRawInputReaderEnable(true);
            touchHelper.setPostInputEvent(true);

        };

        new Thread(thread).start();
        return;



    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }



    @Override
    protected void onPause() {
        super.onPause();
        touchHelper.setRawDrawingEnabled(false);
        touchHelper.setRawDrawingRenderEnabled(false);
        touchHelper.closeRawDrawing();
        saveBitmap();

    }
    public void onDestroy(){
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        saveBitmap();



    }
    private void initSurfaceView() {

        binding.writerview.setBackgroundColor(Color.WHITE);
        binding.writerview.setZOrderOnTop(true);
        binding.writerview.getHolder().setFormat(PixelFormat.TRANSPARENT);


        binding.writerview.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int
                    oldRight, int oldBottom) {
                Rect limit = new Rect();
                binding.writerview.getLocalVisibleRect(limit);

                touchHelper.setStrokeWidth(STROKE_WIDTH);
                touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER);
                touchHelper.setStrokeColor(Color.BLACK);
                touchHelper.setLimitRect(limit, new ArrayList<Rect>()).openRawDrawing();

                touchHelper.setRawDrawingEnabled(false);
                touchHelper.setSingleRegionMode();
                touchHelper.setRawDrawingEnabled(true);
                touchHelper.enableFingerTouch(false);
                touchHelper.setRawDrawingRenderEnabled(true);
                touchHelper.setRawInputReaderEnable(true);


                binding.writerview.addOnLayoutChangeListener(this);
            }
        });


        final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        };
        binding.writerview.getHolder().addCallback(surfaceCallback);
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

    private int countDayPages()
    {
        File dir = getExternalFilesDir(filepath);
        File [] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            }
        });

        return max(files.length,1);
    }
    private void updatePage(boolean forward)
    {
        // move forward or backwards in the diary
        saveBitmap();

        if (forward){
            if (daypage < daypageCount){
                daypage++;
            }
            else{
                daypage=1;
                currentdate = currentdate.plusDays(1);
                daypageCount = countDayPages();
            }

        }
        else {
            if (daypage > 1){
                daypage--;
            }
            else{
                currentdate = currentdate.plusDays(-1);
                daypageCount = countDayPages();
                daypage=daypageCount;
            }
        }


        datebox = findViewById(R.id.date_text);
        datebox.setText(currentdate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + String.valueOf(daypage) + "/" + String.valueOf(daypageCount)+")");

        filename = currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + String.valueOf(daypage) + ".png";
        safeLoadBitmap();
    }

    private void addPage(){
        // add a page to the end and move forward
        needsSave = true;
        saveBitmap();
        daypageCount++;
        updatePage(true);

    }

    private void deletePage(){
        // delete a page
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Confirm delete");
        builder.setMessage("You are about to permanently delete the current page. Are you sure?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
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
                        String newfilename =currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + String.valueOf(i) + ".png";

                        String oldfilename =currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + String.valueOf(i+1) + ".png";
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
        // let the user choose a time frame for export then export to pdf
        saveBitmap();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MaterialThemeDialog);
        String item[] = { "Day", "Month", "Year"};

        final int[] timeframe = {0};
        builder.setTitle("Select time interval for export")
                .setSingleChoiceItems(item, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                timeframe[0] = i;
                            }


                        })
                // Set the action buttons
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            needsSave = true;
                            saveBitmap();
                            writeToPDF(timeframe[0]);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        dialog.dismiss();

                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();

                    }
                });

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

            disableFingerTouch(getApplicationContext());
            points.clear();
            rawDrawing = true;
        }

        @Override
        public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onEndRawDrawing");

            rawDrawing = false;
            enableFingerTouch(getApplicationContext());

            // wait until there's been no writing input then
            // restart the touch helper - this is needed to
            // keep the navigation ball working
            lastDraw = currentTimeMillis();
            if (!redrawRunning)
            {
                redrawRunning = true;
                Runnable thread = () -> {
                    long currentTime = currentTimeMillis();
                    while (currentTime<lastDraw + refreshInterval)
                    {
                        currentTime = currentTimeMillis();
                    }
                    Log.d(TAG, "thread: redrawing");

                    touchHelper.setRawDrawingEnabled(false);
                    touchHelper.setRawDrawingEnabled(true);

                    redrawRunning = false;

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
            drawToBitmap(touchPointList.getPoints());
        }

        @Override
        public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onBeginRawErasing");

            points.clear();
            disableFingerTouch(getApplicationContext());
            rawDrawing = true;
            redrawSurface();
        }

        @Override
        public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onEndRawErasing");

            points.add(touchPoint);
            List<TouchPoint> pointList = new ArrayList<>(points);
            points.clear();
            TouchPointList touchPointList = new TouchPointList();
            for (TouchPoint point : pointList) {
                touchPointList.add(point);
            }
            eraseBitmap(pointList);
            redrawSurface();
            enableFingerTouch(getApplicationContext());
            rawDrawing = false;
        }

        @Override
        public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
            points.add(touchPoint);
            if (points.size() >= 50) {
                List<TouchPoint> pointList = new ArrayList<>(points);
                points.clear();
                TouchPointList touchPointList = new TouchPointList();
                for (TouchPoint point : pointList) {
                    touchPointList.add(point);
                }
                eraseBitmap(pointList);
                redrawSurface();
            }
        }

        @Override
        public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
            Log.d(TAG, "onRawErasingTouchPointListReceived");
            eraseBitmap(touchPointList.getPoints());
            redrawSurface();

        }

        @Override
        public void onPenUpRefresh(RectF refreshRect) {
            getRxManager().enqueue(new PartialRefreshRequest(WriterActivity.this, binding.writerview, refreshRect)
                            .setBitmap(bitmap),
                    new RxCallback<PartialRefreshRequest>() {
                        @Override
                        public void onNext(@NonNull PartialRefreshRequest partialRefreshRequest) {
                        }
                    });
        }
    };

    public static void disableFingerTouch(Context context) {
        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        Rect rect = new Rect(0, 0, width, height);
        Rect[] arrayRect =new Rect[]{rect};
        EpdController.setAppCTPDisableRegion(context, arrayRect);
    }

    public static void enableFingerTouch(Context context) {
        EpdController.appResetCTPDisableRegion(context);
    }

    private RxManager getRxManager() {
        if (rxManager == null) {
            RxManager.Builder.initAppContext(this);
            rxManager = RxManager.Builder.sharedSingleThreadManager();
        }
        return rxManager;
    }

    public void drawToBitmap(List<TouchPoint> list) {
        // save all pen input to a bitmap
        needsSave=true;
        Path path = new Path();

        PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
        path.moveTo(prePoint.x, prePoint.y);
        for (TouchPoint point : list) {
            path.quadTo(prePoint.x, prePoint.y, point.x, point.y);
            prePoint.x = point.x;
            prePoint.y = point.y;
        }

        canvas.drawPath(path, penPaint);
    }

    public void eraseBitmap(List<TouchPoint> list) {
        // save all eraser input to a bitmap
        needsSave=true;

        Path path = new Path();
        PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
        path.moveTo(prePoint.x, prePoint.y);
        for (TouchPoint point : list) {
            path.quadTo(prePoint.x, prePoint.y, point.x, point.y);
            prePoint.x = point.x;
            prePoint.y = point.y;
        }
        
        canvas.drawPath(path, eraserPaint);

        // add the background back on top as we're erasing with white ink
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
                canvas = new Canvas(bitmap);
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
            canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            drawable.draw(canvas);
        }
        catch (Exception e) {
            Log.d("resetBitmap Error: ", e.getMessage(), e);
        }
        return;
    }

    public void safeLoadBitmap()
    {
        while (!binding.writerview.getHolder().getSurface().isValid()) {
            // wait for surfaceview
        }
        loadBitmap();

        if (bitmap == null) {
            resetBitmap();
        }
        redrawSurface();
        return;

    }

    private void writeToPDF(int timeframe) throws FileNotFoundException {

        // this code makes a pdf from the pages of the diary and opens it
        Toast.makeText(this, "Exporting to pdf...", Toast.LENGTH_LONG).show();

        LocalDate startDate = currentdate;
        LocalDate endDate = currentdate;
        String outputFilename = "";

        switch (timeframe) {
            case 0:
                startDate = currentdate;
                endDate = currentdate;
                outputFilename = "Diary-" + currentdate.format(DateTimeFormatter.ofPattern("dd-MMMM-yyyy")) + ".pdf";
                break;
            case 1:
                startDate = currentdate.with(firstDayOfMonth());
                endDate = currentdate.with(lastDayOfMonth());
                outputFilename = "Diary-" + currentdate.format(DateTimeFormatter.ofPattern("MMMM-yyyy")) + ".pdf";
                break;
            case 2:
                startDate = currentdate.with(firstDayOfYear());
                endDate = currentdate.with(lastDayOfYear());
                outputFilename = "Diary-" + currentdate.format(DateTimeFormatter.ofPattern("yyyy")) + ".pdf";

                break;


        }

        PdfDocument pdfDocument = new PdfDocument();

        int pageHeight = 2200;
        int pageWidth = 1650;

        PdfDocument.PageInfo myPageInfo;
        PdfDocument.Page startPage;
        int pageCount = 1;
        for (LocalDate printDate = startDate; !printDate.isAfter(endDate); printDate = printDate.plusDays(1)) {

            File dir = getExternalFilesDir(filepath);
            LocalDate finalPrintDate = printDate;
            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(finalPrintDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                }
            });

            int length = files.length;

            for (int printPage = 1; printPage <= length; printPage++) {
                String pageTitle = printDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + String.valueOf(printPage) + "/" + String.valueOf(length) + ")";
                String printfilename = printDate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + String.valueOf(printPage) + ".png";


                File bitmapFile = new File(getExternalFilesDir(filepath), printfilename);
                if (bitmapFile.exists()) {
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inScaled = true;
                    opt.inMutable = true;
                    bitmap = BitmapFactory.decodeStream(new FileInputStream(bitmapFile), null, opt);

                    myPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
                    startPage = pdfDocument.startPage(myPageInfo);

                    Paint title = new Paint();

                    title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    title.setTextSize(40);
                    title.setColor(Color.WHITE);


                    Paint myPaint = new Paint();
                    myPaint.setColor(Color.rgb(0, 0, 0));
                    myPaint.setStrokeWidth(10);

                    Canvas canvas;
                    canvas = startPage.getCanvas();
                    canvas.drawRect(0, 0, pageWidth, 122, myPaint);
                    canvas.drawText(pageTitle, 110, 100, title);
                    canvas.drawBitmap(bitmap, 0, 122, null);
                    pdfDocument.finishPage(startPage);
                }
            }
        }


        ContentResolver resolver = this.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, outputFilename);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri path = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

        try{
            pdfDocument.writeTo(resolver.openOutputStream(path));
        }
        catch (IOException e){
            e.printStackTrace();
        }
        pdfDocument.close();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "application/pdf");

        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivity(intent);
        return;
    }



}