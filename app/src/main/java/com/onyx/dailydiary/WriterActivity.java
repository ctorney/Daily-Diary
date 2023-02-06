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
    private Paint penPaint = new Paint();
    private Paint eraserPaint = new Paint();

    private boolean needsSave = false;
    private long lastDraw = 0;
    private long refreshInterval = 1000;
    private List<TouchPoint> points = new ArrayList<>();

    private List<TouchPointList> pointLists = new ArrayList<>();
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
        touchHelper = TouchHelper.create(binding.writerview, callback);

        initSurfaceView();
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
                Log.d(TAG, "- onLayoutChange");

                Rect limit = new Rect();
                binding.writerview.getLocalVisibleRect(limit);
                touchHelper.setLimitRect(limit, new ArrayList<Rect>())
                        .openRawDrawing();
                touchHelper.setRawDrawingEnabled(false);
                touchHelper.setSingleRegionMode();
                touchHelper.setRawDrawingEnabled(true);
                touchHelper.enableFingerTouch(true);
                touchHelper.setRawDrawingRenderEnabled(true);

                binding.writerview.addOnLayoutChangeListener(this);
            }
        });


//        binding.writerview.setOnTouchListener(new View.OnTouchListener() {
//            @SuppressLint("ClickableViewAccessibility")
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                Log.d(TAG, "surfaceView.setOnTouchListener - onTouch::action - " + event.getAction());
//                Log.d(TAG, "width height " + binding.writerview.getWidth() + " " + binding.writerview.getHeight());
//
//                return true;
//            }
//        });

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
                return name.contains(new SimpleDateFormat("yyyyMMdd").format(currentdate));
            }
        });

        return max(files.length,1);
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
                calendar.add(Calendar.DATE, 1);

                currentdate = calendar.getTime();
                daypageCount = countDayPages();
            }

        }
        else {
            if (daypage > 1){
                daypage--;
            }
            else{
                calendar.add(Calendar.DATE, -1);
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
            touchHelper.setRawDrawingEnabled(false);
            touchHelper.setRawDrawingEnabled(true);
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

                        drawToBitmap();
                        redrawSurface();

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
            pointLists.add(touchPointList);
//            drawScribbleToBitmap(touchPointList.getPoints(),false);
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
            List<TouchPoint> pointList = new ArrayList<>(points);
            points.clear();
            TouchPointList touchPointList = new TouchPointList();
            for (TouchPoint point : pointList) {
                touchPointList.add(point);
            }
            eraseBitmap(pointList);
            redrawSurface();
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
    };


    public void drawToBitmap() {

        needsSave=true;
        Log.d(TAG, "drawScribbleToBitmap");
        Canvas canvas = new Canvas(bitmap);


        Path path = new Path();

        for (TouchPointList pointList : pointLists) {
            List<TouchPoint> list = pointList.getRenderPoints();
            PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
            path.moveTo(prePoint.x, prePoint.y);
            for (TouchPoint point : list) {
                path.quadTo(prePoint.x, prePoint.y, point.x, point.y);
                prePoint.x = point.x;
                prePoint.y = point.y;
            }
        }
        canvas.drawPath(path, penPaint);
        
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.pagelines, null);
        drawable.setBounds(0, 0,binding.writerview.getWidth(), binding.writerview.getHeight());
        drawable.draw(canvas);
        pointLists.clear();
    }

    public void eraseBitmap(List<TouchPoint> list) {

        needsSave=true;
        Log.d(TAG, "drawScribbleToBitmap");
        Canvas canvas = new Canvas(bitmap);

        Rect limit = new Rect();
        Point offset = new Point();
        binding.writerview.getGlobalVisibleRect(limit,offset);

        Path path = new Path();
        PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
        path.moveTo(prePoint.x, prePoint.y);
        for (TouchPoint point : list) {
            path.quadTo(prePoint.x, prePoint.y, point.x, point.y);
            prePoint.x = point.x;
            prePoint.y = point.y;
        }
        
        canvas.drawPath(path, eraserPaint);
        
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

    private void writeToPDF(int timeframe){
        Toast.makeText(this, "exporting to pdf for timeframe " + timeframe, Toast.LENGTH_LONG).show();
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
//        private void generatePDF(File file) {
//
//        PdfDocument pdfDocument = new PdfDocument();
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
//        String numPagesString = sharedPref.getString("num_pages", "2");
//
//        int numPages = 2;
//        try {
//            numPages = Integer.parseInt(numPagesString);
//        }
//        catch (Exception e)
//        {
//            Toast.makeText(MainActivity.this, "Unable to parse num pages value " + numPagesString + ". Using default value of 2.", Toast.LENGTH_LONG).show();
//        }
//
//
//
//        int pageHeight = 2200;
//        int pageWidth = 1650;
//
//        int linesStart = 140;
//        int linesStop = pageHeight;
//        int lineHeight = 60;
//
//
//        PdfDocument.PageInfo myPageInfo;
//        PdfDocument.Page startPage;
//
//        // setup first page
//        myPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
//        startPage = pdfDocument.startPage(myPageInfo);
//
//        Paint title = new Paint();
//
//        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
//        title.setTextSize(40);
//        title.setColor(ContextCompat.getColor(this, R.color.black));
//
//        Canvas canvas;
//        canvas = startPage.getCanvas();
//        canvas.drawText(DayofMonth + " " + monthFromDate(selectedDate)+ " " + yearFromDate(selectedDate), 110, 135, title);
//        Paint paint;
//        paint = new Paint();
//
//        paint.setColor(ContextCompat.getColor(this, R.color.gray));
//
//        canvas.drawLine(100, 0,100,pageHeight, paint);
//        for (int j = linesStart; j <= linesStop; j += lineHeight) {
//            canvas.drawLine(0, j, pageWidth, j, paint);
//        }
//
//        pdfDocument.finishPage(startPage);
//
//        // add lines to other pages
//        for (int i = 1; i < numPages; i++){
//            myPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i+1).create();
//            startPage = pdfDocument.startPage(myPageInfo);
//
//            canvas = startPage.getCanvas();
//            paint = new Paint();
//            paint.setColor(ContextCompat.getColor(this, R.color.gray));
//            canvas.drawLine(100, 0,100,pageHeight, paint);
//
//
//            for (int j = linesStart; j <= linesStop; j += lineHeight) {
//                canvas.drawLine(0, j, pageWidth, j, paint);
//            }
//
//            pdfDocument.finishPage(startPage);
//
//        }
//
//
//
//        try {
//            // after creating a file name we will
//            // write our PDF file to that location.
//            pdfDocument.writeTo(new FileOutputStream(file));
//
//            // below line is to print toast message
//            // on completion of PDF generation.
//            Toast.makeText(MainActivity.this, "PDF file generated successfully.", Toast.LENGTH_SHORT).show();
//        } catch (IOException e) {
//            // below line is used
//            // to handle error
//            e.printStackTrace();
//        }
//        // after storing our pdf to that
//        // location we are closing our PDF file.
//        pdfDocument.close();
//    }
    }

}