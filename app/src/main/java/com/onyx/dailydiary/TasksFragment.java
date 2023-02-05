package com.onyx.dailydiary;


import static com.onyx.android.sdk.utils.ApplicationUtil.getApplicationContext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.pen.NeoFountainPen;
import com.onyx.android.sdk.utils.NumberUtils;
import com.onyx.android.sdk.utils.TouchUtils;
import com.onyx.dailydiary.databinding.FragmentTasksBinding;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;

public class TasksFragment extends Fragment implements View.OnClickListener {
    public FragmentTasksBinding binding;
    private static TasksFragment instance;
    private static final String TAG = TasksFragment.class.getSimpleName();


    private String filepath = "Bitmaps";
    private String filename =  "tasks.png";

    private final float STROKE_WIDTH = 4.0f;
    public Bitmap bitmap;
    private Paint mPaint = new Paint();
    private Paint erasePaint = new Paint();

    public List<TouchPoint> points = new ArrayList<>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    public void onResume() {

        Log.d(TAG, "onResume");
        super.onResume();
        Runnable thread = new Runnable()
        {
            public void run()
            {
                safeLoadBitmap();
                redrawSurface();

            }
        };
        new Thread(thread).start();    //use start() instead of run()
        return;
    }



    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        instance = this;
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        initSurfaceView();
        initPaint();

        Button clear_all = (Button) view.findViewById(R.id.button);
        clear_all.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        resetBitmap();
        redrawSurface();
        Log.d(TAG, "onClick");

    }

    public void safeLoadBitmap()
    {
        while (!binding.surfaceview.getHolder().getSurface().isValid()) {

        }
        loadBitmap();

        if (bitmap == null) {
            resetBitmap();
        }
        Canvas lockCanvas = binding.surfaceview.getHolder().lockCanvas();

        Rect rect = new Rect(0, 0, binding.surfaceview.getWidth(), binding.surfaceview.getHeight());
        lockCanvas.drawBitmap(bitmap, null, rect, null);
        binding.surfaceview.getHolder().unlockCanvasAndPost(lockCanvas);
    }

    public static TasksFragment GetInstance()
    {
        return instance;
    }

    public void onDestroyView(){
        Log.d(TAG, "onDestroyView");
//        redrawSurface();
        super.onDestroyView();
        saveBitmap();

    }

    private void initPaint(){
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(5);

        erasePaint = new Paint();
        erasePaint.setAntiAlias(true);
        erasePaint.setDither(true);
        erasePaint.setColor(Color.WHITE);
        erasePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setStrokeCap(Paint.Cap.ROUND);
        erasePaint.setStrokeWidth(10);
//
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initSurfaceView() {
        binding.surfaceview.setBackgroundColor(Color.WHITE);
        binding.surfaceview.setZOrderOnTop(true);
        binding.surfaceview.getHolder().setFormat(PixelFormat.TRANSPARENT);
        binding.surfaceview.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int
                    oldRight, int oldBottom) {

                Log.d(TAG, "surfaceView.onLayoutChange" );
                binding.surfaceview.addOnLayoutChangeListener(this);
            }
        });

        binding.surfaceview.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "surfaceView.setOnTouchListener - onTouch::action - " + event.getAction());
                ((MainActivity)getActivity()).writeTasks = true;
                return true;
            }
        });

        final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");
                safeLoadBitmap();
                Rect limit = new Rect();
                binding.surfaceview.getGlobalVisibleRect(limit);
                ((MainActivity)getActivity()).addRect(limit);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                holder.removeCallback(this);
            }
        };
        binding.surfaceview.getHolder().addCallback(surfaceCallback);



    }


    public void drawScribbleToBitmap(List<TouchPoint> list, boolean eraser) {
        Log.d(TAG, "drawScribbleToBitmap");
        Canvas canvas = new Canvas(bitmap);

        Rect limit = new Rect();
        Point offset = new Point();
        binding.surfaceview.getGlobalVisibleRect(limit,offset);

        Path path = new Path();
        PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
        path.moveTo(prePoint.x-offset.x, prePoint.y-offset.y);
        for (TouchPoint point : list) {
            path.quadTo(prePoint.x-offset.x, prePoint.y-offset.y, point.x-offset.x, point.y-offset.y);
            prePoint.x = point.x;
            prePoint.y = point.y;
        }
        if (eraser){
            canvas.drawPath(path, erasePaint);
        }
        else{
            canvas.drawPath(path, mPaint);
        }
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.lines, null);
        drawable.setBounds(0, 0,binding.surfaceview.getWidth(), binding.surfaceview.getHeight());
        drawable.draw(canvas);

    }

    public void redrawSurface() {
        if (!binding.surfaceview.getHolder().getSurface().isValid()){
            return;
        }
        ((MainActivity)getActivity()).touchHelper.setRawDrawingRenderEnabled(false);


        Log.d(TAG, "redrawSurface");
        Canvas lockCanvas = binding.surfaceview.getHolder().lockCanvas();
        lockCanvas.drawColor(Color.WHITE);
        lockCanvas.drawBitmap(bitmap, 0, 0, null);
        binding.surfaceview.getHolder().unlockCanvasAndPost(lockCanvas);
        ((MainActivity)getActivity()).touchHelper.setRawDrawingRenderEnabled(true);
    }
    public void saveBitmap() {

        Log.d(TAG, "saveBitmap");
        File myExternalFile = new File(getActivity().getExternalFilesDir(filepath), filename);
        try {
            FileOutputStream fos =  new FileOutputStream(myExternalFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.d("SAVE_IMAGE", e.getMessage(), e);
        }
    }

    public void loadBitmap() {
        try {
            Log.d(TAG, "loadBitmap");
            File myExternalFile = new File(getActivity().getExternalFilesDir(filepath), filename);
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
            Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.lines, null);
            bitmap = Bitmap.createBitmap(binding.surfaceview.getWidth(), binding.surfaceview.getHeight(), Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, binding.surfaceview.getWidth(), binding.surfaceview.getHeight());
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
        }
        catch (Exception e) {
            Log.d("resetBitmap Error: ", e.getMessage(), e);
        }
        return;
    }
}
