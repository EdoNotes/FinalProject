package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Random;


public class EmailVerification extends AppCompatActivity {

    private String randomString = null;
    private Registration regAct = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);
    }


    public void sendSignInLink(Registration regActivity , String email , String password) {
        // [START auth_send_sign_in_link]
        regAct = regActivity;
        final FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            randomString = generateVereficationCode();

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(randomString).build();

                            auth.getCurrentUser().updateProfile(profileUpdates);
                            auth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        Log.d("EmailVerification", "Email sent.");

                                        Intent EmailVerificationActivity=new Intent(getBaseContext(),EmailVerification.class);
                                        startActivity(EmailVerificationActivity);
                                    }
                                    else {
                                        Log.d("EmailVerification", "Email sent FAILED. " + task.getException().getMessage());
                                        randomString = null;
                                    }
                                }
                            });
                        }
                        else {
                            Log.d("EmailVerification", "Create user failed. " + task.getException().getMessage());
                        }

                    }
                });
        // [END auth_send_sign_in_link]
    }

    private static String generateVereficationCode()
    {
        Random ran = new Random();
        char data = ' ';
        String dat = "";

        for (int i=0; i<6; i++) {
            data = (char)(ran.nextInt(25)+97);
            dat = dat + data;
        }

        return dat;
    }

}
