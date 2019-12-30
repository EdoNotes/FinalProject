package com.example.finalproject;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Vector;

import static androidx.constraintlayout.solver.widgets.Optimizer.OPTIMIZATION_NONE;

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
    WindowManager.LayoutParams RedParams;
    int[] prev_heights = new int[10];

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
        //super.onCreate();
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
        int i=0;

        windowManager=(WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(szWindow);
        layoutInflater=(LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        //create blurring view
        blurringView = (ConstraintLayout) layoutInflater.inflate(R.layout.layout_bubble_head, null);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT>=28 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        RedParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT>=28 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        ConstraintLayout curr;

        for(i=0; i<10; i++)
        {
            curr=(ConstraintLayout) layoutInflater.inflate(R.layout.layout_bubble_head, null);
            blurringViews.add(curr);
            prev_heights[i] = 0;
        }
        i=1;
        for(ConstraintLayout view:blurringViews)
        {
            params.x=i;
            params.y=1;
            params.gravity = Gravity.TOP|Gravity.LEFT;
            params.height=0;
            params.width=1;
            view.setLayoutParams(params);
            windowManager.addView(view,params);
            i++;

            //view.setOptimizationLevel(OPTIMIZATION_NONE);
        }

        curr=(ConstraintLayout) layoutInflater.inflate(R.layout.layout_bubble_head_red, null);
        blurringViews.add(curr);

        ConstraintLayout view = blurringViews.get(10);
        RedParams.x=0;
        RedParams.y=0;
        RedParams.gravity = Gravity.TOP|Gravity.LEFT;
        RedParams.height=0;
        RedParams.width=20;
        view.setLayoutParams(RedParams);
        windowManager.addView(view,RedParams);

    }
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }
    public void blur(Vector<BlurData> dataVec)
    {
        //clean();
        int i = 0;
        ConstraintLayout view;

        if(null == dataVec)
            return;

        if(dataVec.size()!=0) {

            startTimeMilli = System.currentTimeMillis();

        /*for(BlurData bd:dataVec)
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
        */
            RedParams.height = 20;
            windowManager.updateViewLayout(blurringViews.get(10), RedParams);
            
            BlurData curr;

            for (BlurData bd : dataVec) {
                view = blurringViews.get(i);
                curr = dataVec.get(i);
                params.x = curr.getX();
                params.y = curr.getY();
                //params.gravity = Gravity.TOP|Gravity.LEFT;
                params.height = curr.getHeight();
                params.width = curr.getWidth();
                //view.setLayoutParams(params);
                windowManager.updateViewLayout(view, params);
                //windowManager.addView(view,params);
                //alreadyblurredViews.add(view);
                i++;
            }

        }
        else {
            RedParams.height = 0;
            windowManager.updateViewLayout(blurringViews.get(10), RedParams);
        }

        params.height = 0;
        for (i = i; i < 10; i++) {
            if (prev_heights[i] == 0)
                continue;
            prev_heights[i]=0;
            view = blurringViews.get(i);
            windowManager.updateViewLayout(view, params);
        }


        Log.i(TAG, "blur time:" + (System.currentTimeMillis() - startTimeMilli));
    }//blur
    public void clean()
    {
        int i=0;

        startTimeMilli = System.currentTimeMillis();

        ConstraintLayout view;
        params.height = 0;
        for(i=0; i<10; i++)
        {
            view = blurringViews.get(i);
            //windowManager.removeViewImmediate(view);
            prev_heights[i] = view.getHeight();
            if(prev_heights[i] == 0)
                continue;
            //view.setLayoutParams(params);
            windowManager.updateViewLayout(view, params);
        }

        RedParams.height = 0;
        windowManager.updateViewLayout(blurringViews.get(10), RedParams);

        Log.i(TAG, "clean time:" + (System.currentTimeMillis() - startTimeMilli));
    }

    public void restore()
    {
        startTimeMilli = System.currentTimeMillis();

        int i = 0;

        for(ConstraintLayout view:blurringViews)
        {
            //params.height = prev_heights[i];
            if(prev_heights[i] == 0)
                continue;
            view.getLayoutParams().height = prev_heights[i];
            windowManager.updateViewLayout(view , view.getLayoutParams());
            i++;
        }

        RedParams.height = 20;
        windowManager.updateViewLayout(blurringViews.get(10), RedParams);

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
