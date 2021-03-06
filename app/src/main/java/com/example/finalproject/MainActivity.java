package com.example.finalproject;

import android.content.ComponentName;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.nio.ByteBuffer;

import static com.example.finalproject.Welcome.sharedPreferences;


public class MainActivity extends AppCompatActivity {

    Button StartBtn;
    Button StopBtn;
    Spinner dropdown;
    Button btnChangePassword;
    Button btnShowDataLog;
    EditText input;
    AlertDialog ad;
    String[] drop_items;

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

    //private final Lock StartLock = new ReentrantLock(true);
    private static Semaphore StartLock = new Semaphore(1);
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

        if(!isLayoutOverlayPermissionGranted(MainActivity.this))
            grantLayoutOverlayPermission(MainActivity.this);

        while(!isLayoutOverlayPermissionGranted(MainActivity.this)) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {;}
        }

        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE);

        frameCounter = new IntObj();
        mCapture = new CaptureScreen(MainActivity.this, defineDelayMilli, definedRatio, definedDensity);

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_control_panel);
        // Init gui
        StartBtn = (Button) findViewById(R.id.StartBtn);
        StopBtn = (Button) findViewById(R.id.StopBtn);
        //final ImageView imgTrump = (ImageView) findViewById(R.id.imageView2);

        StartBtn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {

                //Snackbar.make(v, "Capture screen started..", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                Toast.makeText(getApplicationContext(), "Capture screen started..", Toast.LENGTH_LONG).show();

                if (mCapture == null) {
                    mCapture = new CaptureScreen(MainActivity.this, defineDelayMilli, definedRatio, definedDensity);
                }

                mCapture.StartCaputre();

                if(!isLayoutOverlayPermissionGranted(MainActivity.this))
                {
                    grantLayoutOverlayPermission(MainActivity.this);
                    Toast.makeText(getApplicationContext(), "Permissions not granted", Toast.LENGTH_LONG).show();
                    mCapture.StopCaputre();
                    return;
                }

                if(mCapture.PermissionGranted != 1) {
                    Toast.makeText(getApplicationContext(), "Permissions not granted", Toast.LENGTH_LONG).show();
                    mCapture.StopCaputre();
                    return;
                }

                classifierObj = new FrameClassifier(MainActivity.this, definedRatio);

                runnableObj = new Runnable() {
                    public void run() {

                        long frameC = -1;
                        long startTimeMilli = -1;
                        Message completeMessage;

                        frameCounter.value = -1;
                        /*try{
                        StartLock.acquire();
                        }catch (Exception e){}*/

                        while (null != mCapture) {
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
                                    Log.i(TAG, "GetLatestFrame1 time:" + (System.currentTimeMillis() - startTimeMilli));

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
                                    Log.i(TAG, "GetLatestFrame2 time:" + (System.currentTimeMillis() - startTimeMilli));

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

                                        if (null == results || (results.size() == 0 && frameCounter.value < frameC+3)) {
                                            Log.i(TAG, "Results == null");
                                            dataVector = null;
                                            frameCounter.value = -1;
                                        } /*else {
                                            if (results.size() == 0 && frameCounter.value > frameC)
                                                dataVector = null;
                                            Log.i(TAG, "Results == " + results.size() + " FrameC == " + frameC);
                                        }*/

                                        if(frameCounter.value != -1)
                                            frameC = frameCounter.value;

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
                            }finally {
                                //StartLock.release();
                            }

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

                        //StartLock.release();

                    }
                };

                new Thread(runnableObj).start();

            }
        });

        dropdown=(Spinner) findViewById(R.id.spinner);
        btnChangePassword=(Button) findViewById(R.id.btnChangePassword);
        btnShowDataLog=(Button) findViewById(R.id.BtnShowDataLog);
        drop_items=new String[]{getString(R.string.NetConPerson),getString(R.string.NetConPorn),getString(R.string.NetConBlood),getString(R.string.NetConShoppingAds)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, drop_items);
        dropdown.setAdapter(adapter);
        AlertDialog.Builder builder =new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.TitleChangePassword));
        builder.setMessage(getString(R.string.MessageEnterNewPassword));
        input=new EditText(this);
        TextView lbl=new TextView(this);
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.PositiveButtonOK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String txtNewPass=input.getText().toString();
                Toast.makeText(getApplicationContext(), R.string.MessagePasswordChanged,Toast.LENGTH_SHORT).show();
                sharedPreferences.edit().putString(getString(R.string.Password),txtNewPass).apply();
            }
        });
        builder.setNegativeButton(getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Do Nothing
            }
        });
        ad=builder.create();
        btnChangePassword.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                ad.show();
            }
        });
        btnShowDataLog.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dataLogActivity=new Intent(getBaseContext(),DataLog.class);
                startActivity(dataLogActivity);
            }
        });
        StopBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Capture screen stopped..", Toast.LENGTH_LONG).show();

                if(mCapture != null) {
                    CaptureScreen tempobj = mCapture;
                    mCapture = null;

                    try {
                        //StartLock.acquire();
                        tempobj.StopCaputre();
                        mServer.clean();

                    } catch (Exception e) {
                        // handle the exception
                    } finally {
                        //StartLock.release();
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

        super.onActivityResult(requestCode, resultCode, data);

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






