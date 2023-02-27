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

    private int minTouchX;
    private int minTouchY;
    private int maxTouchX;
    private int maxTouchY;
    private boolean rawDrawing = false;
    private boolean redrawRunning = false;
    private long lastDraw = 0;
    private final long refreshInterval = 1000;
    private TouchHelper touchHelper = null;
    private final Context mContext;

    public PenCallback(Context context, List<BitmapView> views) {
        this.mContext = context;
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

    public boolean isRawDrawing() {
        return rawDrawing;
    }

    @Override
    public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
        Log.d(TAG, "onBeginRawDrawing");
        minTouchX = (int)touchPoint.x;
        maxTouchX = (int)touchPoint.x;
        minTouchY = (int)touchPoint.y;
        maxTouchY = (int)touchPoint.y;
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
        if ((int)touchPoint.x<minTouchX)
            minTouchX = (int)touchPoint.x;
        if ((int)touchPoint.x>maxTouchX)
            maxTouchX = (int)touchPoint.x;
        if ((int)touchPoint.y<minTouchY)
            minTouchY = (int)touchPoint.y;
        if ((int)touchPoint.y>maxTouchY)
            maxTouchY = (int)touchPoint.y;
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
        minTouchX = (int)touchPoint.x;
        maxTouchX = (int)touchPoint.x;
        minTouchY = (int)touchPoint.y;
        maxTouchY = (int)touchPoint.y;
        rawDrawing = true;
//         for (BitmapView view:views) {
//             view.redrawSurface();
//         }
        
        
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
            Rect eraseRect = new Rect(minTouchX,minTouchY,maxTouchX,maxTouchY);
            Rect limit = new Rect();
            Point offset = new Point();
            view.getGlobalVisibleRect(limit, offset);
            eraseRect.offset(-offset.x, -offset.y);
            view.partialRedraw(eraseRect);
        }
        rawDrawing = false;
    }

    @Override
    public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
        points.add(touchPoint);
        if ((int)touchPoint.x<minTouchX)
            minTouchX = (int)touchPoint.x;
        if ((int)touchPoint.x>maxTouchX)
            maxTouchX = (int)touchPoint.x;
        if ((int)touchPoint.y<minTouchY)
            minTouchY = (int)touchPoint.y;
        if ((int)touchPoint.y>maxTouchY)
            maxTouchY = (int)touchPoint.y;

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
                Rect eraseRect = new Rect(minTouchX,minTouchY,maxTouchX,maxTouchY);
                Rect limit = new Rect();
                Point offset = new Point();
                view.getGlobalVisibleRect(limit, offset);
                eraseRect.offset(-offset.x, -offset.y);

                view.partialRedraw(eraseRect);
            }
        }
    }

    @Override
    public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
        Log.d(TAG, "onRawErasingTouchPointListReceived");
        needsSave=true;


        for (BitmapView view:views) {
            view.eraseBitmap(touchPointList.getPoints());
//            view.redrawSurface();
            Rect eraseRect = new Rect(minTouchX,minTouchY,maxTouchX,maxTouchY);
            Rect limit = new Rect();
            Point offset = new Point();
            view.getGlobalVisibleRect(limit, offset);
            eraseRect.offset(-offset.x, -offset.y);

            view.partialRedraw(eraseRect);

        }

    }

    @Override
    public void onPenUpRefresh(RectF refreshRect) {
        Log.d(TAG, "onPenUpRefresh " + rawDrawing);


        for (BitmapView view:views) {
            RectF viewRect = new RectF(refreshRect.left,refreshRect.top,refreshRect.right,refreshRect.bottom);

            Rect limit = new Rect();
            Point offset = new Point();
            view.getGlobalVisibleRect(limit, offset);
            viewRect.offset(-offset.x, -offset.y);

            getRxManager().enqueue(new PartialRefreshRequest(mContext, view, viewRect)
                            .setBitmap(view.getBitmap()),
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

