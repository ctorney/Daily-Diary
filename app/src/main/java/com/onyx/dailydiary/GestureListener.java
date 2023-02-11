package com.onyx.dailydiary;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

class GestureListener extends GestureDetector.SimpleOnGestureListener {

    public static final int SWIPE_VELOCITY_THRESHOLD = 1000;
    public static final int SWIPE_THRESHOLD = 400;
    private static final String TAG = GestureListener.class.getSimpleName();

    @Override
    public boolean onDown(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {

        float diffY = event2.getY() - event1.getY();
        float diffX = event2.getX() - event1.getX();
        Log.d(TAG, "onFling: " + diffY + " " +diffX +  " " + velocityX + " " +velocityY);

        if (Math.abs(diffX) > Math.abs(diffY)){

            if (Math.abs(diffX)> SWIPE_THRESHOLD && Math.abs(velocityX)>SWIPE_VELOCITY_THRESHOLD){
                if (diffX>0){
                    onSwipeRight();
                }
                else{
                    onSwipeLeft();
                }
            }
        }
        else{
            if (Math.abs(diffY)> SWIPE_THRESHOLD && Math.abs(velocityY)>SWIPE_VELOCITY_THRESHOLD){
                if (diffY>0){
                    onSwipeBottom();
                }
                else{
                    onSwipeTop();
                }
            }
        }

        return true;
    }

    public void onSwipeLeft(){
        Log.d(TAG, "onSwipeLeft");

    }

    public void onSwipeRight(){
        Log.d(TAG, "onSwipeRight");
    }

    public void onSwipeBottom(){
        Log.d(TAG, "onSwipeBottom");
    }

    public void onSwipeTop(){
        Log.d(TAG, "onSwipeTop");
    }

    public boolean onDoubleTap(MotionEvent event) {
        Log.d(TAG, "onDoubleTap: " + event.toString());
        return true;
    }
}