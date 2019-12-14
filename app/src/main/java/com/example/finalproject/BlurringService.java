package com.example.finalproject;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Vector;

public class BlurringService extends Service
{
    IBinder mBinder = new LocalBinder();
    private static final String TAG = "BlurringService";
    private WindowManager windowManager;
    private ConstraintLayout blurringView;
    private Vector<ConstraintLayout> blurringViews=new Vector<ConstraintLayout>();
    private Vector<ConstraintLayout> alreadyblurredViews=new Vector<ConstraintLayout>();
    private LinearLayout bubbleLayout;
    private Point szWindow = new Point(0,0);
    private LayoutInflater layoutInflater;
    WindowManager.LayoutParams params;

    private long startTimeMilli;
    /**
     *Todo later
     */
    public BlurringService()
    {
    }
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG,"OnCreate()");
        handleStart();
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (blurringView!= null)
            windowManager.removeView(blurringView);
        if (bubbleLayout != null)
            windowManager.removeView(bubbleLayout);
    }
    private void handleStart(){
        windowManager=(WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(szWindow);
        layoutInflater=(LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        //create blurring view
        blurringView = (ConstraintLayout) layoutInflater.inflate(R.layout.layout_bubble_head, null);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
    }
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }
    public void blur(Vector<BlurData> dataVec)
    {
        int i=0;

        //clean();

        if(null == dataVec || dataVec.size()==0)
            return;

        startTimeMilli = System.currentTimeMillis();

        for(BlurData bd:dataVec)
        {
            ConstraintLayout curr;
            curr=(ConstraintLayout) layoutInflater.inflate(R.layout.layout_bubble_head, null);
            blurringViews.add(curr);
        }
        for(ConstraintLayout view:blurringViews)
        {
            BlurData curr=dataVec.get(i);
            params.x=curr.getX();
            params.y=curr.getY();
            params.gravity = Gravity.TOP|Gravity.LEFT;
            params.height=curr.getHeight();
            params.width=curr.getWidth();
            view.setLayoutParams(params);
            windowManager.addView(view,params);
            alreadyblurredViews.add(view);
            i++;
        }
        blurringViews.clear();//make the vector empty for next blurring

        Log.i(TAG, "blur time:" + (System.currentTimeMillis() - startTimeMilli));
    }//blur
    public void clean()
    {
        startTimeMilli = System.currentTimeMillis();

        for(ConstraintLayout view:alreadyblurredViews)
            windowManager.removeView(view);

        Log.i(TAG, "clean time:" + (System.currentTimeMillis() - startTimeMilli));
    }

    public void restore()
    {
        startTimeMilli = System.currentTimeMillis();

        for(ConstraintLayout view:alreadyblurredViews)
        {
            windowManager.addView(view , view.getLayoutParams());
        }

        Log.i(TAG, "restore time:" + (System.currentTimeMillis() - startTimeMilli));
    }

    public void remove_alreadyBlurred()
    {
        startTimeMilli = System.currentTimeMillis();

        alreadyblurredViews.clear();

        Log.i(TAG, "remove_alreadyBlurred time:" + (System.currentTimeMillis() - startTimeMilli));
    }

    public class LocalBinder extends Binder
    {
        public BlurringService getServerInstance()
        {
            return BlurringService.this;
        }
    }

}
