package com.example.prototype1;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.widget.Button;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.nio.ByteBuffer;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private CaptureScreen mCapture = null;
    private volatile int imageAvailable = 0;

    private FrameClassifier classifierObj = null;
    private Runnable runnableObj = null;

    public static final int defineDelayMilli = 200;
    public static final int definedRatio = 300;
    public static final int definedDensity = 420;
    public static final float definedThreshold = (float)0.55;

    private final Lock lock = new ReentrantLock(true);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                            IntObj frameCounter = new IntObj();
                            ByteBuffer buff;

                            while(null != mCapture) {
                                lock.lock();
                                try {
                                    startTimeMilli = System.currentTimeMillis();

                                    buff = mCapture.GetLatestFrame(frameCounter);

                                    if (null != buff) {
                                        ArrayList<int[]> results;
                                        if (null != classifierObj) {
                                            classifierObj.PredictFrame(buff, definedThreshold);
                                            results = classifierObj.GetCoordinates();

                                            results = null;
                                            // Add draw functions
                                        } else
                                            classifierObj = new FrameClassifier(MainActivity.this, definedRatio);
                                    }

                                } catch (Exception e) {
                                    Log.i(TAG, e.toString());
                                } finally {
                                    lock.unlock();
                                }

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

                //((TextView)findViewById(R.id.textView)).setText("Running...");


                if(mCapture != null) {
                    lock.lock();
                    try {
                        mCapture.StopCaputre();
                        mCapture = null;
                    } catch (Exception e) {
                        // handle the exception
                    } finally {
                        lock.unlock();
                    }
                }

            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mCapture != null) {
            mCapture.StopCaputre();
            mCapture = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CaptureScreen.PERMISSION_CODE) {
            mCapture.CaptureScreenActivityResult(requestCode, resultCode, data);
        }
    }

    public class IntObj {
        public long value;
    }

}






