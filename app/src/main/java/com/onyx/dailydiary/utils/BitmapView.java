package com.onyx.dailydiary.utils;

import android.content.Context;
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
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.res.ResourcesCompat;

import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateMode;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.utils.RectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

public class BitmapView extends SurfaceView {
    int mStrokeWidth=4;
    public Bitmap mBitmap = null;
    String mFilename = null;
    int mBackground;
    Paint penPaint;
    Paint eraserPaint;
    String mFilepath = null;

    private Context mContext;

    private static final String TAG = BitmapView.class.getSimpleName();

    public BitmapView(Context context) {
        super(context);
        mContext = context;
//        mSurfaceHolder = getHolder();
//        mSurfaceHolder.addCallback(this);
        initView();
    }

    public BitmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initView();

    }

    public BitmapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initView();

    }


//    @Override
//    protected void onDraw(Canvas canvas) {
//        canvas.drawRGB(mRed, mGreen, mBlue);
//        canvas.rotate(mAngle, mCenterX, mCenterY);
//        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, 6, mVertices, 0, null, 0, mColors, 0, null, 0, 0, mPaint);
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//            case MotionEvent.ACTION_MOVE:
//                synchronized(mSurfaceHolder) {
//                    mRed = (int) (255*event.getX()/getWidth());
//                    mGreen = (int) (255*event.getY()/getHeight());
//                }
//                return true;
//        }
//        return super.onTouchEvent(event);
//    }


//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
////        mVertices[0] = width/2;
////        mVertices[1] = height/2;
////        mVertices[2] = width/2 + width/5;
////        mVertices[3] = height/2 + width/5;
////        mVertices[4] = width/2;
////        mVertices[5] = height/2 + width/5;
////        mCenterX = width/2 + width/10;
////        mCenterY = height/2 + width/10;
////        Log.d(TAG, "surfaceCreated");
//
//    }
//
//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
////        mThread.keepRunning = true;
//        Log.d(TAG, "surfaceCreated");
//
////        mThread.start();
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
////        mThread.keepRunning = false;
//
//    }

//    private class DrawingThread extends Thread {
//        boolean keepRunning = true;
//
//        @Override
//        public void run() {
//            Canvas c;
//            while (keepRunning) {
//                c = null;
//
//                try {
//                    c = mSurfaceHolder.lockCanvas();
//                    synchronized (mSurfaceHolder) {
//                        mAngle += 1;
//                    }
//                } finally {
//                    if (c != null)
//                        mSurfaceHolder.unlockCanvasAndPost(c);
//                }
//
//                // Run the draw loop at 50FPS
//                try {
//                    Thread.sleep(20);
//                } catch (InterruptedException e) {}
//            }
//        }
//    }

    @Override
    protected void onWindowVisibilityChanged (int visibility)
    {
        super.onWindowVisibilityChanged(visibility);
        Log.d(TAG, "onWindowVisibilityChanged " + visibility);
        redrawSurface();

    }

    public void setFilename(String filename)
    {
        mFilename = filename;
        loadBitmap();
        if (mBitmap==null) {
            resetBitmap();
        }
    }

    public void setFilepath(String filepath)
    {
        mFilepath = filepath;

    }


    public void setStrokeWidth(int strokeWidth)
    {
        mStrokeWidth = strokeWidth;
    }

    public void setBackground(int background)
    {
        mBackground = background;
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        Log.d(TAG, "onDraw");

//        canvas.drawColor(Color.WHITE);
//        canvas.drawBitmap(mBitmap, 0, 0, null);
        super.onDraw(canvas);
        if (mBitmap==null)
            loadBitmap();

        if (mBitmap==null)
            resetBitmap();
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(mBitmap, 0, 0, null);
//        redrawSurface();
    }

    public Bitmap getBitmap()
    {
        return mBitmap;
    }

    @Override
    public void onFocusChanged(boolean gainFocus,int direction,Rect previouslyFocusedRect) {
        Log.d(TAG, "onFocusChanged");

        redrawSurface();
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    public void resetBitmap() {
        Log.d(TAG, "resetBitmap");
        try {
            mBitmap = null;
            if (!getHolder().getSurface().isValid())
                return;
            Drawable drawable = ResourcesCompat.getDrawable(getResources(), mBackground, null);
            mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, getWidth(), getHeight());
            Canvas canvas = new Canvas(mBitmap);
            canvas.drawColor(Color.WHITE);
            drawable.draw(canvas);
        }
        catch (Exception e) {
            Log.d("resetBitmap Error: ", e.getMessage(), e);
        }
    }

    public void loadBitmap() {
        try {
            Log.d(TAG, "loadBitmap");
            if (mBitmap!=null)
                mBitmap.recycle();

            File myExternalFile = new File(mContext.getExternalFilesDir(mFilepath), mFilename);
            if (myExternalFile.exists())
            {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inScaled = true;
                opt.inMutable = true;
                mBitmap = BitmapFactory.decodeStream(new FileInputStream(myExternalFile),null, opt);
            }
            else
            {
                resetBitmap();

            }
        } catch (Exception e) {
            Log.d("loadBitmap Error: ", e.getMessage(), e);
        }

    }

    public void redrawSurface() {
        Log.d(TAG, "redrawSurface");
        if (!getHolder().getSurface().isValid())
            return;
        Canvas lockCanvas = getHolder().lockCanvas();
        draw(lockCanvas);
//        lockCanvas.drawColor(Color.WHITE);
//        lockCanvas.drawBitmap(mBitmap, 0, 0, null);
        getHolder().unlockCanvasAndPost(lockCanvas);
//        if (mBitmap==null)
//            loadBitmap();
//
//        if (mBitmap==null)
//            return;
//
//
//        Canvas lockCanvas = getHolder().lockCanvas();
//        lockCanvas.drawColor(Color.WHITE);
//        lockCanvas.drawBitmap(mBitmap, 0, 0, null);
//        getHolder().unlockCanvasAndPost(lockCanvas);

    }



    public void partialRedraw(Rect renderRect) {

        if (!getHolder().getSurface().isValid())
            return;

        Canvas canvas = getHolder().lockCanvas(renderRect);
        if (canvas == null) {
            return;
        }
        try {
            canvas.clipRect(renderRect);
            Rect rect = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
            canvas.drawBitmap(mBitmap, rect, rect, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            getHolder().unlockCanvasAndPost(canvas);
//            EpdController.resetViewUpdateMode(surfaceView);
        }
    }



    public void saveBitmap() {
        Log.d(TAG, "saveBitmap");
        File myExternalFile = new File(mContext.getExternalFilesDir(mFilepath), mFilename);
        try {
            FileOutputStream fos =  new FileOutputStream(myExternalFile);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.d("Save bitmap error", e.getMessage(), e);
        }
    }

    public void drawToBitmap(List<TouchPoint> list) {

        Canvas canvas = new Canvas(mBitmap);
        Rect limit = new Rect();
        Point offset = new Point();
        getGlobalVisibleRect(limit,offset);

        Path path = new Path();
//
//        for (TouchPointList pointList : pointLists) {
//            List<TouchPoint> list = pointList.getRenderPoints();


        PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
        path.moveTo(prePoint.x-offset.x, prePoint.y-offset.y);
        for (TouchPoint point : list) {
            path.quadTo(prePoint.x-offset.x, prePoint.y-offset.y, point.x-offset.x, point.y-offset.y);
            prePoint.x = point.x;
            prePoint.y = point.y;
        }
//        }

        canvas.drawPath(path, penPaint);

//        Drawable drawable = ResourcesCompat.getDrawable(getResources(), background, null);
//        drawable.setBounds(0, 0,surfaceView.getWidth(), surfaceView.getHeight());
//        drawable.draw(canvas);

    }

    public void eraseBitmap(List<TouchPoint> list) {

        Canvas canvas = new Canvas(mBitmap);
        Rect limit = new Rect();
        Point offset = new Point();
        getGlobalVisibleRect(limit,offset);

        Path path = new Path();
        PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
        path.moveTo(prePoint.x-offset.x, prePoint.y-offset.y);
        for (TouchPoint point : list) {
            path.quadTo(prePoint.x-offset.x, prePoint.y-offset.y, point.x-offset.x, point.y-offset.y);
            prePoint.x = point.x;
            prePoint.y = point.y;
        }

        canvas.drawPath(path, eraserPaint);

        Drawable drawable = ResourcesCompat.getDrawable(getResources(), mBackground, null);
        drawable.setBounds(0, 0,getWidth(), getHeight());
        drawable.draw(canvas);

    }

    private void initView(){
        Log.d(TAG, "initPaint");

        penPaint = new Paint();
        penPaint.setAntiAlias(true);
        penPaint.setDither(true);
        penPaint.setColor(Color.BLACK);
        penPaint.setStyle(Paint.Style.STROKE);
        penPaint.setStrokeJoin(Paint.Join.ROUND);
        penPaint.setStrokeCap(Paint.Cap.ROUND);
        penPaint.setStrokeWidth(mStrokeWidth);

        eraserPaint = new Paint();
        eraserPaint.setAntiAlias(true);
        eraserPaint.setDither(true);
        eraserPaint.setColor(Color.WHITE);
        eraserPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        eraserPaint.setStrokeJoin(Paint.Join.ROUND);
        eraserPaint.setStrokeCap(Paint.Cap.SQUARE);
        eraserPaint.setStrokeWidth(10*mStrokeWidth);

        setBackgroundColor(Color.WHITE);
        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSPARENT);

    }

}
