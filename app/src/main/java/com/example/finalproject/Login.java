package com.example.finalproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import static com.example.finalproject.Welcome.sharedPreferences;

public class Login extends AppCompatActivity {

    private Button loginbtn;
    private EditText txtloginPass;
    private Button btnLogin = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_login);
        setTitle(R.string.TitleSignIn);
        loginbtn=(Button)findViewById(R.id.btnLogin);
        txtloginPass=(EditText)findViewById(R.id.txtsignpass);

        btnLogin = (Button) findViewById(R.id.btnLoginWithDifferentEmail);

        EmailLogin El = new EmailLogin(this , btnLogin , handler);
        El.EmailLoginDialog();

        loginbtn.setOnClickListener(new Button.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String SavedPass=sharedPreferences.getString(getString(R.string.PasswordKey),"");
                if(SavedPass.equals(txtloginPass.getText().toString()))
                {
                    Intent MainActivity=new Intent(getBaseContext(), MainActivity.class);
                    startActivity(MainActivity);
                }
                else
                {
                    txtloginPass.setError(getString(R.string.WrongPassword));
                }
            }
        });
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
