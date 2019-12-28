package com.example.finalproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import static com.example.finalproject.Welcome.sharedPreferences;

public class Login extends AppCompatActivity {
Button loginbtn;
EditText txtloginPass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_login);
        setTitle("Sign in");
        loginbtn=(Button)findViewById(R.id.btnLogin);
        txtloginPass=(EditText)findViewById(R.id.txtsignpass);

        loginbtn.setOnClickListener(new Button.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String SavedPass=sharedPreferences.getString("Password","");
                if(SavedPass.equals(txtloginPass.getText().toString()))
                {
                    Intent MainActivity=new Intent(getBaseContext(), ControlPanel.class);
                    startActivity(MainActivity);
                }
                else
                {
                    txtloginPass.setError("Wrong Password");
                }
            }
        });
    }
}
