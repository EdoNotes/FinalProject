package com.example.finalproject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;



public class Welcome extends Activity
{
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor editor;
    Button btnRegister;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        sharedPreferences=getApplication().getSharedPreferences("MyPref",0);
        editor= sharedPreferences.edit();
        if(sharedPreferences.getAll().size()==0)
        {
            setContentView(R.layout.welcome);
            btnRegister=(Button) findViewById(R.id.btnRegister);
            btnRegister.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    Intent RegisterActivity=new Intent(getBaseContext(),Registration.class);
                    startActivity(RegisterActivity);

//                    // TEMPORARY FOR DEVELOPMENT
//                    Intent MainActivity=new Intent(getBaseContext(),MainActivity.class);
//                    startActivity(MainActivity);

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
}
