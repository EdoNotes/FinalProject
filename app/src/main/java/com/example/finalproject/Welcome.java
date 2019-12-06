package com.example.finalproject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class Welcome extends Activity
{
    Button btnRegister;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        btnRegister=findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent mainActivity=new Intent(getBaseContext(),MainActivity.class);
                startActivity(mainActivity);
            }
        });
    }
}
