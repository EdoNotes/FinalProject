package com.example.createoverlay;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Vector;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    boolean mBounded;
    BlurringService mServer;
    private Button startButton;

    @Override
   protected void onStart()
   {
       super.onStart();
       Intent intent = new Intent(this, BlurringService.class);
       startService(intent);
       bindService(intent,mConnection,BIND_AUTO_CREATE);
   }
   @Override protected void onDestroy()
   {
       super.onDestroy();
       Intent intent = new Intent(this, BlurringService.class);
       if (mBounded) {
           mServer.onUnbind(intent);
           mBounded = false;
       }
   }
    @Override
    protected void onStop() {
        Intent intent = new Intent(this, BlurringService.class);
        super.onStop();
        if (mBounded) {
            mServer.onUnbind(intent);
            mBounded = false;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton=(Button)findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(isLayoutOverlayPermissionGranted(MainActivity.this))
                {
                    Vector<BlurData> dataVector=new Vector<BlurData>();
                    dataVector.add(new BlurData(200,500,200,200));
                    dataVector.add(new BlurData(400,0,500,200));
                    mServer.blur(dataVector);
                }
                else
                {
                    grantLayoutOverlayPermission(MainActivity.this);
                }
            }
        });
    }//onCreate
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
}
