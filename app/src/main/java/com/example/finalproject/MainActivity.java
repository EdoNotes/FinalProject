package com.example.finalproject;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.widget.Button;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.nio.ByteBuffer;


public class MainActivity extends Activity {

    public static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    private static final int BLURRING_OPCODE_DRAW = 1;
    private static final int BLURRING_OPCODE_CLEAN = 2;
    private static final int BLURRING_OPCODE_GETBUFFER = 3;
    private static final int BLURRING_OPCODE_RESTORE = 0;

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
    private static Semaphore DrawThreadToUI = new Semaphore(0);
    private static Semaphore DrawUIToThread = new Semaphore(1);

    private static volatile boolean DrawOrRestore = true;
    private volatile ByteBuffer FrameBuffer = null;

    //private static ByteBuffer FrameBuffer;
    private static IntObj frameCounter;

    @Override
    protected void onStart()
    {
        super.onStart();
        Intent intent = new Intent(this, BlurringService.class);
        startService(intent);
        bindService(intent,mConnection,BIND_AUTO_CREATE);

        frameCounter = new IntObj();

        mCapture = new CaptureScreen(MainActivity.this, defineDelayMilli, definedRatio, definedDensity);
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
                    try {
                        Vector vec=new Vector<ConstraintLayout>();
                        //dataVector.add(new BlurData((int)(Math.random() * 150 + 100),(int)(Math.random() * 50 + 450),(int)(Math.random() * 50 + 250),(int)(Math.random() * 50 + 50)));
                        vec.add(new BlurData(100, 0, 0, 0));
                        mServer.blur(vec);
                    } catch (Exception e) {
                    }

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

                if (mCapture == null) {
                    mCapture = new CaptureScreen(MainActivity.this, defineDelayMilli, definedRatio, definedDensity);
                    return;
                }

                if(!isLayoutOverlayPermissionGranted(MainActivity.this))
                {
                    grantLayoutOverlayPermission(MainActivity.this);
                }

                if(mCapture.PermissionGranted != 1 || !isLayoutOverlayPermissionGranted(MainActivity.this)) {
                    Snackbar.make(v, "Permissions not granted", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    return;
                }

                mCapture.StartCaputre();

                classifierObj = new FrameClassifier(MainActivity.this, definedRatio);

                runnableObj = new Runnable() {
                    public void run() {

                        long frameC = -1;
                        long startTimeMilli = -1;
                        Message completeMessage;

                        frameCounter.value = -1;

                        while (null != mCapture) {
                            StartLock.lock();
                            try {
                                //if (startTimeMilli != -1)
                                //    Log.i(TAG, "Cycle time:" + (System.currentTimeMillis() - startTimeMilli));
                                //startTimeMilli = System.currentTimeMillis();

                                if(frameCounter.value != -1) {
                                    startTimeMilli = System.currentTimeMillis();
                                    // Clean
                                    completeMessage = handler.obtainMessage(BLURRING_OPCODE_CLEAN, mServer);
                                    completeMessage.sendToTarget();
                                    DrawThreadToUI.acquire();
                                    //mCapture.CleanFrameQueue();
                                    //frameC = mCapture.GetFrameCounter();
                                    //Log.i(TAG, "GetFrameCounter == " + frameC);
                                    //frameC++;

                                    Log.i(TAG, "CleanHandler time:" + (System.currentTimeMillis() - startTimeMilli));

                                    // GetFrame
                                    startTimeMilli = System.currentTimeMillis();
                                    FrameBuffer = mCapture.GetLatestFrame(frameCounter);
                                    Log.i(TAG, "GetLatestFrame time:" + (System.currentTimeMillis() - startTimeMilli));

                                    // Restore
                                    startTimeMilli = System.currentTimeMillis();
                                    completeMessage = handler.obtainMessage(BLURRING_OPCODE_RESTORE, mServer);
                                    completeMessage.sendToTarget();
                                    DrawThreadToUI.acquire();
                                /*try {
                                    Thread.sleep(0);
                                } catch (Exception e) {
                                }*/
                                    Log.i(TAG, "RestoreHandler time:" + (System.currentTimeMillis() - startTimeMilli));
                                }
                                else
                                {
                                    // GetFrame
                                    startTimeMilli = System.currentTimeMillis();
                                    FrameBuffer = mCapture.GetLatestFrame(frameCounter);
                                    Log.i(TAG, "GetLatestFrame time:" + (System.currentTimeMillis() - startTimeMilli));

                                    //frameC = mCapture.GetFrameCounter();
                                }

                                /*if(mCapture.GetFrameCounter() == -1) {
                                    try {
                                        Thread.sleep(10);
                                    } catch (Exception e) {
                                    }
                                }*/

                                if (null != FrameBuffer) {
                                    ArrayList<int[]> results;

                                    startTimeMilli = System.currentTimeMillis();
                                    if (null != classifierObj && classifierObj.PredictFrame(FrameBuffer, definedThreshold) != -2) {
                                        results = classifierObj.GetCoordinates();

                                        Log.i(TAG, "PredictFrame and GetCoordinates time:" + (System.currentTimeMillis() - startTimeMilli));

                                        // Add draw functions
                                        startTimeMilli = System.currentTimeMillis();
                                        int i, x, y;
                                        dataVector = new Vector<BlurData>();

                                        for (i = 0; null != results && i < results.size(); i++) {
                                            x = results.get(i)[1] < 0 ? 0 : results.get(i)[1];
                                            y = results.get(i)[0] < 0 ? 0 : results.get(i)[0];
                                            if (results.get(i)[2] > 0 && results.get(i)[3] > 0) {
                                                dataVector.add(new BlurData(x, y, results.get(i)[2] - x, results.get(i)[3] - y));
                                            }
                                        }

                                        if (null == results) {
                                            Log.i(TAG, "Results == null");
                                            dataVector = null;
                                        } /*else {
                                            if (results.size() == 0 && frameCounter.value > frameC)
                                                dataVector = null;
                                            Log.i(TAG, "Results == " + results.size() + " FrameC == " + frameC);
                                        }*/

                                        Log.i(TAG, "Build Vector from Results time:" + (System.currentTimeMillis() - startTimeMilli));


                                        startTimeMilli = System.currentTimeMillis();

                                        /*try {
                                            Thread.sleep(3000);
                                        } catch (Exception e) {}*/
                                        // Clean
                                        /*completeMessage = handler.obtainMessage(BLURRING_OPCODE_CLEAN , mServer);
                                        completeMessage.sendToTarget();
                                        DrawThreadToUI.acquire();*/

                                        // Draw
                                        completeMessage = handler.obtainMessage(BLURRING_OPCODE_DRAW, mServer);
                                        completeMessage.sendToTarget();
                                        DrawThreadToUI.acquire();

                                        Log.i(TAG, "DrawHandler time:" + (System.currentTimeMillis() - startTimeMilli));

                                    } else
                                        classifierObj = new FrameClassifier(MainActivity.this, definedRatio);
                                }

                            } catch (Exception e) {
                                Log.i(TAG, e.toString() + " " + e.getStackTrace());
                            }

                            StartLock.unlock();

                            if (null != FrameBuffer || (null == FrameBuffer && frameCounter.value == -1)) {
                                while ((System.currentTimeMillis() - startTimeMilli) < defineDelayMilli) {
                                    try {
                                        Thread.sleep(1);
                                    } catch (Exception e) {
                                        Log.i(TAG, "Failed to Sleep in Background thread");
                                    }
                                }
                            }
                        }

                    }
                };

                new Thread(runnableObj).start();

            }
        });


        StopBtn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {

                Snackbar.make(v, "Capture screen stopped..", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                if(mCapture != null) {
                    CaptureScreen tempobj = mCapture;
                    mCapture = null;

                    StartLock.lock();
                    try {
                        tempobj.StopCaputre();
                    } catch (Exception e) {
                        // handle the exception
                    } finally {
                        StartLock.unlock();
                    }
                }

            }
        });

    }

    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {

            BlurringService mServer = (BlurringService) inputMessage.obj;

            try {
                switch (inputMessage.what) {
                    case BLURRING_OPCODE_DRAW:
                        //mServer.clean();
                        //mServer.remove_alreadyBlurred();
                        mServer.blur(dataVector);
                        break;
                    case BLURRING_OPCODE_CLEAN:
                        mServer.clean();
                        break;
                    case BLURRING_OPCODE_RESTORE:
                        mServer.restore();
                        break;
                    case BLURRING_OPCODE_GETBUFFER:
                        FrameBuffer = mCapture.GetLatestFrame(frameCounter);
                        break;
                }
            }catch (Exception e){
                e.printStackTrace();
            } finally {
                DrawThreadToUI.release();
            }

        }
    };


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
            if(null != mCapture) {
                mCapture.PermissionGranted = 1;
                mCapture.Per_requestCode = requestCode;
                mCapture.Per_resultCode = resultCode;
                mCapture.Per_data = data;
            }
        }
        else
        {
            if(null != mCapture) {
                mCapture.PermissionGranted = 0;
                mCapture.Per_requestCode = 0;
            }
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






