package com.onyx.dailydiary.utils;

import static java.lang.System.currentTimeMillis;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;

import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.pen.data.TouchPointList;
import com.onyx.android.sdk.rx.RxCallback;
import com.onyx.android.sdk.rx.RxManager;

import java.util.ArrayList;
import java.util.List;

public class PenCallback extends RawInputCallback {
    private static final String TAG = PenCallback.class.getSimpleName();
    private List<TouchPoint> points = new ArrayList<>();
    private RxManager rxManager;

    private final List<BitmapView> views;
    private boolean needsSave = false;

    private boolean useOffset = false;
    private boolean rawDrawing = false;
    private boolean redrawRunning = false;
    private long lastDraw = 0;
    private final long refreshInterval = 1000;
    private TouchHelper touchHelper = null;
    private final Context mContext;

    public PenCallback(Context context, List<BitmapView> views, boolean useOffset) {


        this.mContext = context;
        this.useOffset = useOffset;
        this.views = views;
    }

    public void setTouchHelper(TouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }


    public boolean needsSave() {
        return needsSave;
    }

    public void setNeedsSave(boolean needsSave)
    {
        this.needsSave = needsSave;
    }

    @Override
    public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
        Log.d(TAG, "onBeginRawDrawing");

        points.clear();
        rawDrawing = true;
    }

    @Override
    public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
        Log.d(TAG, "onEndRawDrawing");

        rawDrawing = false;

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
//                    binding.writerview.redrawSurface();
                
                
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
        needsSave=true;

        for (BitmapView view:views) {
            view.drawToBitmap(touchPointList.getPoints());    
        }
        
    }

    @Override
    public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
        Log.d(TAG, "onBeginRawErasing");

        points.clear();
        rawDrawing = true;
        for (BitmapView view:views) {
            view.redrawSurface();
        }
        
        
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
        needsSave=true;
        for (BitmapView view:views) {
            view.eraseBitmap(pointList);
            view.redrawSurface();
        }
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
            needsSave=true;

            for (BitmapView view:views) {
                view.eraseBitmap(pointList);
                view.redrawSurface();
            }
        }
    }

    @Override
    public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
        Log.d(TAG, "onRawErasingTouchPointListReceived");
        needsSave=true;


        for (BitmapView view:views) {
            view.eraseBitmap(touchPointList.getPoints());
            view.redrawSurface();
        }

    }

    @Override
    public void onPenUpRefresh(RectF refreshRect) {
        Log.d(TAG, "onPenUpRefresh " + rawDrawing);
        Rect limit = new Rect();
        Point offset = new Point();

        for (BitmapView view:views) {
            if (useOffset) {
                view.getGlobalVisibleRect(limit, offset);
                refreshRect.offset(-offset.x, -offset.y);
            }
//            binding.writerview.partialRedraw(refreshRect);
            getRxManager().enqueue(new PartialRefreshRequest(mContext, view, refreshRect)
                            .setBitmap(view.mBitmap),
                    new RxCallback<PartialRefreshRequest>() {
                        @Override
                        public void onNext(@NonNull PartialRefreshRequest partialRefreshRequest) {
                        }
                    });
        }
    }

    private RxManager getRxManager() {
        if (rxManager == null) {
            RxManager.Builder.initAppContext(mContext);
            rxManager = RxManager.Builder.sharedSingleThreadManager();
        }
        return rxManager;
    }
};

