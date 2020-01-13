package com.example.finalproject;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class Welcome extends AppCompatActivity
{
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor editor;
    Button btnRegister;
    Button btnLogin;
    ImageView imgView;

    private AlertDialog ad;
    private Context context = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        sharedPreferences=getApplication().getSharedPreferences("MyPref",0);
        editor= sharedPreferences.edit();
        if(sharedPreferences.getAll().size()==0)
        {
            setContentView(R.layout.welcome);
            btnLogin = (Button) findViewById(R.id.btnExistingLogin);
            btnRegister=(Button) findViewById(R.id.btnRegister);
            imgView = (ImageView) findViewById(R.id.HelpImage);

            EmailLogin El = new EmailLogin(this , btnLogin , handler);
            El.EmailLoginDialog();

            btnRegister.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    Intent RegisterActivity=new Intent(getBaseContext(),Registration.class);
                    startActivity(RegisterActivity);

                    // TEMPORARY FOR DEVELOPMENT
                    //Intent MainActivity=new Intent(getBaseContext(),MainActivity.class);
                    //startActivity(MainActivity);
                }
            });

            context = Welcome.this;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Help");
            builder.setMessage("Welcome to ParentControl application. For first time you must to register with email and password. " +
                    "For next time you will be able to log in with your password. For logging with different email and password, " +
                    "you must be connected to the Internet.\n\nBy clicking Start or Stop the censoring engine will start or stop in " +
                    "background. You are able to see the log censoring objects, and also change the password if you wish to.");


            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);
            builder.setView(ll);

            builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ;
                }
            });
            ad = builder.create();
            imgView.setOnClickListener(new ImageView.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    ad.show();
                }
            });
        }
        //data already stored --->already registered
        else
        {
            Intent LoginActivity=new Intent(getBaseContext(), Login.class);
            startActivity(LoginActivity);
        }
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {

            try {
                switch (inputMessage.what) {
                    case 1:
                        Intent MainActivity=new Intent(getBaseContext(), MainActivity.class);
                        startActivity(MainActivity);
                        break;
                }
            }catch (Exception e){
                e.printStackTrace();
            } finally {
            }

        }
    };
}
