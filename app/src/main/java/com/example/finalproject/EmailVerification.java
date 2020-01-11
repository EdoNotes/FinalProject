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
                           Toast.makeText(getApplicationContext(), R.string.EmailNotVerified, Toast.LENGTH_LONG).show();
                   }
                   else {
                       Toast.makeText(getApplicationContext(), R.string.VerificationCodeWrong, Toast.LENGTH_LONG).show();
                   }
               }
               else {
                   Toast.makeText(getApplicationContext(), R.string.GeneralError, Toast.LENGTH_LONG).show();
               }

            }
        });
    }


}
