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
    private float STROKE_WIDTH = 4.0f;
    private boolean redrawRunning = false;
    private String filepath = "DailyDiary";
    private String filename;
    public Bitmap bitmap;
    private Paint penPaint;// = new Paint();
    private Paint eraserPaint;// = new Paint();

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
//        binding = ActivityWriterBinding.inflate(getLayoutInflater());
//        View view = binding.getRoot();
//        setContentView(view);

//        binding = DataBindingUtil.setContentView(this, R.layout.activity_writer);
//        initSurfaceView(); //debug
//        initPaint();

        STROKE_WIDTH = getIntent().getFloatExtra("stroke-width",STROKE_WIDTH);

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
//                updatePage(true);
                break;
            case R.id.prevpage:
//                updatePage(false);
                break;
            case R.id.addpage:
//                addPage();
                break;
            case R.id.deletepage:
//                deletePage();
                break;
            case R.id.save:
//                savePages();
                break;


        }
        Log.d(TAG, "Writer onClick");

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
    private void initPaint(){

//        path = new Path();

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
//
    }
}