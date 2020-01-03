package com.example.finalproject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;


public class Welcome extends AppCompatActivity
{
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor editor;
    Button btnRegister;
    Button btnLogin;
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
