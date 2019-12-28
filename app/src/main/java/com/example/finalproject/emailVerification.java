package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class EmailVerification extends AppCompatActivity {

    Button btnSubmit;
    EditText vereficationCodeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);
        btnSubmit=(Button)findViewById(R.id.btnSubmitVerification);
        vereficationCodeText = findViewById(R.id.editText);
        btnSubmit.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v)
            {
               if(null != Registration.regAct) {
                   if(Registration.regAct.VerificationCodeIsValid(vereficationCodeText.getText().toString())) {

                       if(Registration.regAct.EmailIsVerified()) {

                           boolean saved = Registration.regAct.SaveVerificationData();
                           Intent LoginActivity = new Intent(getBaseContext(), Login.class);
                           startActivity(LoginActivity);
                       }
                       else
                           Toast.makeText(getApplicationContext(), "Email not verified. \nClick on the link inside the mail.", Toast.LENGTH_LONG).show();
                   }
                   else {
                       Toast.makeText(getApplicationContext(), "Verification code is wrong. try again.", Toast.LENGTH_LONG).show();
                   }
               }
               else {
                   Toast.makeText(getApplicationContext(), "General error occurred, go back and try again", Toast.LENGTH_LONG).show();
               }

            }
        });
    }


}
