package com.example.finalproject;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
//import android.support.design.widget.Snackbar;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.widget.Button;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.nio.ByteBuffer;


public class MainActivity extends Activity {

    public static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    boolean mBounded;
    BlurringService mServer;
    private Button blurButton;
    private static final String TAG = "MainActivity";
    private CaptureScreen mCapture = null;
    private volatile int imageAvailable = 0;

    private FrameClassifier classifierObj = null;
    private Runnable runnableObj = null;

    Vector<BlurData> dataVector = null;

    public static final int defineDelayMilli = 200;
    public static final int definedRatio = 300;
    public static final int definedDensity = 420;
    public static final float definedThreshold = 0.5f;

    private final Lock StartLock = new ReentrantLock(true);
    private static Semaphore DrawSem = new Semaphore(1);
    private static Semaphore DrawUIToThread = new Semaphore(1);
    private static Semaphore DrawThreadToUI = new Semaphore(0);

    private static volatile boolean DrawOrRestore = true;

    private static ByteBuffer FrameBuffer;
    private static IntObj frameCounter;

    @Override
    protected void onStart()
    {
        super.onStart();
        Intent intent = new Intent(this, BlurringService.class);
        startService(intent);
        bindService(intent,mConnection,BIND_AUTO_CREATE);

        frameCounter = new IntObj();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        blurButton=(Button)findViewById(R.id.blur_Btn);
        blurButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(isLayoutOverlayPermissionGranted(MainActivity.this))
                {
                    Vector<BlurData> dataVector=new Vector<BlurData>();
                    //dataVector.add(new BlurData((int)(Math.random() * 150 + 100),(int)(Math.random() * 50 + 450),(int)(Math.random() * 50 + 250),(int)(Math.random() * 50 + 50)));
                    dataVector.add(new BlurData(100,0,500,500));
                    mServer.blur(dataVector);
                }
                else
                {
                    grantLayoutOverlayPermission(MainActivity.this);
                }
            }
        });
        // Init gui
        final Button StartBtn = (Button) findViewById(R.id.StartBtn);
        final Button StopBtn = (Button) findViewById(R.id.StopBtn);
        //final ImageView imgTrump = (ImageView) findViewById(R.id.imageView2);

        StartBtn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {

                Snackbar.make(v, "Capture screen started..", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                if(mCapture == null) {
                    mCapture = new CaptureScreen(MainActivity.this , defineDelayMilli, definedRatio, definedDensity);
                    mCapture.StartCaputre();

                    classifierObj = new FrameClassifier(MainActivity.this , definedRatio);

                    runnableObj = new Runnable() {
                        public void run() {

                            long startTimeMilli = -1;

                            try{
                                DrawSem.acquire();
                            }catch(Exception e){}

                            runUiThread();

                            try{
                                DrawSem.acquire();
                                DrawSem.release();
                            }catch(Exception e){}

                            while(null != mCapture) {
                                StartLock.lock();
                                try {
                                    if(startTimeMilli != -1)
                                        Log.i(TAG, "Cycle time:" + (System.currentTimeMillis()-startTimeMilli));
                                    startTimeMilli = System.currentTimeMillis();

                                    // Wait for restore to end and release
                                    DrawOrRestore = false;
                                    DrawUIToThread.acquire();
                                    DrawThreadToUI.release();
                                    DrawUIToThread.acquire();
                                    DrawUIToThread.release();

                                    if (null != FrameBuffer) {
                                        ArrayList<int[]> results;
                                        if (null != classifierObj && classifierObj.PredictFrame(FrameBuffer, definedThreshold) != -2) {
                                            results = classifierObj.GetCoordinates();

                                            // Add draw functions
                                            int i, x, y;
                                            dataVector = new Vector<BlurData>();

                                            for (i = 0; null != results && i < results.size(); i++) {
                                                x = results.get(i)[1] < 0 ? 0 : results.get(i)[1];
                                                y = results.get(i)[0] < 0 ? 0 : results.get(i)[0];
                                                if (results.get(i)[2] > 0 && results.get(i)[3] > 0) {
                                                    dataVector.add(new BlurData(x, y, results.get(i)[2] - x, results.get(i)[3] - y));
                                                }
                                            }

                                            if (null == results)
                                                dataVector = null;

                                            // Wait for draw to end and release
                                            DrawOrRestore = true;
                                            DrawUIToThread.acquire();
                                            DrawThreadToUI.release();
                                            DrawUIToThread.acquire();
                                            DrawUIToThread.release();


                                        } else
                                            classifierObj = new FrameClassifier(MainActivity.this, definedRatio);
                                    }

                                } catch (Exception e) {
                                    Log.i(TAG, e.toString() + " " + e.getStackTrace());
                                }

                                StartLock.unlock();

                                while ((System.currentTimeMillis() - startTimeMilli) < defineDelayMilli) {
                                    try {
                                        Thread.sleep(1);
                                    } catch (Exception e) {
                                        Log.i(TAG, "Failed to Sleep in Background thread");
                                    }
                                }
                            }

                        }
                    };

                    new Thread(runnableObj).start();
                }

            }
        });


        StopBtn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {

                Snackbar.make(v, "Capture screen stopped..", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                if(mCapture != null) {
                    StartLock.lock();
                    try {
                        mCapture.StopCaputre();
                        mCapture = null;
                    } catch (Exception e) {
                        // handle the exception
                    } finally {
                        StartLock.unlock();
                    }
                }

            }
        });

    }

    private void runUiThread(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                while (null != mCapture) {
                    try {
                        DrawSem.release();
                        DrawThreadToUI.acquire();

                        if(DrawOrRestore) {
                            mServer.blur(dataVector);
                        }
                        else{
                            mServer.clean(false);
                            FrameBuffer = mCapture.GetLatestFrame(frameCounter);
                            mServer.restore();
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    } finally {
                        DrawUIToThread.release();
                    }
                }
            }
        });
    }

    /*private void restoreUiThread(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /*try {
                    mServer.clean(false);
                    FrameBuffer = mCapture.GetLatestFrame(frameCounter);
                    mServer.restore();
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }
                ClearAndRestoreSem.release();
            }
        });
    }*/

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mCapture != null) {
            mCapture.StopCaputre();
            mCapture = null;
        }
        Intent intent = new Intent(this, BlurringService.class);
        if (mBounded) {
            mServer.onUnbind(intent);
            mBounded = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CaptureScreen.PERMISSION_CODE) {
            mCapture.CaptureScreenActivityResult(requestCode, resultCode, data);
        }
    }

    public class IntObj {
        public long value;
    }
    ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mBounded = false;
            mServer = null;
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mBounded = true;
            BlurringService.LocalBinder mLocalBinder = (BlurringService.LocalBinder)service;
            mServer = mLocalBinder.getServerInstance();
        }
    };
    private boolean isLayoutOverlayPermissionGranted(Activity activity)
    {
        Log.v(TAG,"Granting Layout Overlay Permission..");
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(activity))
        {
            Log.v(TAG,"Permission is denied");
            return false;
        }
        else
        {
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }//isLayoutOverlayPermissionGranted
    private void grantLayoutOverlayPermission(Activity activity)
    {
        if(Build.VERSION.SDK_INT>=23 &&!Settings.canDrawOverlays(activity))
        {
            Intent intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent,CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        }
    }
}






