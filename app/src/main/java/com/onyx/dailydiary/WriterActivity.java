package com.onyx.dailydiary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.pen.data.TouchPointList;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import com.onyx.dailydiary.databinding.ActivityWriterBinding;

import java.util.ArrayList;

public class WriterActivity extends AppCompatActivity {

    private ActivityWriterBinding binding;
    private static final String TAG = WriterActivity.class.getSimpleName();

    private TouchHelper touchHelper;
    private final float STROKE_WIDTH = 4.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writer);
        getSupportActionBar().hide();

        binding = DataBindingUtil.setContentView(this, R.layout.activity_writer);
        initSurfaceView();
    }

    private void initSurfaceView() {

        binding.writerview.setBackgroundColor(Color.WHITE);
        binding.writerview.setZOrderOnTop(true);
        binding.writerview.getHolder().setFormat(PixelFormat.TRANSPARENT);
        touchHelper = TouchHelper.create(binding.writerview, callback);

        binding.writerview.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int
                    oldRight, int oldBottom) {

                Rect limit = new Rect();
                binding.writerview.getLocalVisibleRect(limit);

                touchHelper.setStrokeWidth(STROKE_WIDTH).setLimitRect(limit,null).setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER)
                        .openRawDrawing();

                touchHelper.setStrokeColor(Color.BLACK);
                touchHelper.setRawDrawingEnabled(true);
                touchHelper.setRawDrawingRenderEnabled(true);

                binding.writerview.addOnLayoutChangeListener(this);
            }
        });

        binding.writerview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "surfaceView.setOnTouchListener - onTouch::action - " + event.getAction());
                return true;
            }
        });

        final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                holder.removeCallback(this);
            }
        };
        binding.writerview.getHolder().addCallback(surfaceCallback);
    }

    private RawInputCallback callback = new RawInputCallback() {

        @Override
        public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onBeginRawDrawing");
            Log.d(TAG, touchPoint.getX() + ", " + touchPoint.getY());

        }

        @Override
        public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onEndRawDrawing###");

        }

        @Override
        public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
            Log.d(TAG, "onRawDrawingTouchPointMoveReceived");
            Log.d(TAG, touchPoint.getX() + ", " + touchPoint.getY());

        }

        @Override
        public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
            Log.d(TAG, "onRawDrawingTouchPointListReceived");
        }

        @Override
        public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onBeginRawErasing");
        }

        @Override
        public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onEndRawErasing");
        }

        @Override
        public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
            Log.d(TAG, "onRawErasingTouchPointMoveReceived");
        }

        @Override
        public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
            Log.d(TAG, "onRawErasingTouchPointListReceived");
        }
    };
}