package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Random;

import static com.example.finalproject.Welcome.editor;
import static com.example.finalproject.Welcome.sharedPreferences;


public class Registration extends AppCompatActivity
{
    Button btnSubmit;
    EditText password;
    EditText rePassword;
    EditText Email;
    TextView txtError;
    RadioGroup radioGroup;

    private String randomString = null;

    public static Registration regAct = null;
    private FirebaseAuth auth = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        regAct = Registration.this;

        setContentView(R.layout.registration);
        btnSubmit=(Button)findViewById(R.id.btnSubmit);
        password=findViewById(R.id.editTxt_password);
        rePassword=findViewById(R.id.editTxt_Repassword);
        Email=findViewById(R.id.editTextEmail);
        txtError=findViewById(R.id.txtError);
        radioGroup=(RadioGroup)findViewById(R.id.radioGroup);
        btnSubmit.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v)
            {

                //first check that all fields are filled
                if(password.getText().toString().isEmpty()||
                        rePassword.getText().toString().isEmpty()||
                       Email.getText().toString().isEmpty())
                {
                    //txtError.setText("Please fill all required fields");
                    //txtError.setTextColor(getColor(android.R.color.holo_red_dark));
                    Toast.makeText(getApplicationContext(), "Please fill all required fields.", Toast.LENGTH_LONG).show();
                }//empty field
                else
                {//all required fields ok
                    if(rePassword.getText().toString().length()!=6||password.getText().toString().length()!=6)
                    {
                        //txtError.setText("Password must be 6 digits");
                        //txtError.setTextColor(getColor(android.R.color.holo_red_dark));
                        Toast.makeText(getApplicationContext(), "Password must be 6 digits.", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        if(rePassword.getText().toString().equals(password.getText().toString())==false)
                        {
                            //txtError.setText("Passwords difference\ntry Again!");
                            //txtError.setTextColor(getColor(android.R.color.holo_red_dark));
                            Toast.makeText(getApplicationContext(), "Passwords difference\ntry Again!", Toast.LENGTH_LONG).show();
                        }
                        else if(Email.getText().toString().contains("@")== false||Email.getText().toString().contains(".")==false)
                        {
                            {
                                Email.setError("Wrong Email Format");
                            }
                        }
                        else
                            {//all ok
                                editor.putString("Password",password.getText().toString());
                                editor.putString("Email",Email.getText().toString());
                                //net configuration check

                                if (radioGroup.getCheckedRadioButtonId()==-1)
                                {
                                    editor.putString("NetCon","Flowers");
                                }
                                else{
                                    int id=radioGroup.getCheckedRadioButtonId();
                                    RadioButton checked=findViewById(id);
                                    editor.putString("NetCon",checked.getText().toString());
                                }

                                EmailVerification ev = new EmailVerification();
                                sendSignInLink(/*"misha3792v@gmail.com"*/Email.getText().toString() , /*"123456"*/password.getText().toString());

                            }
                    }

                }
            }
        });
    }

    public void sendSignInLink(String email , String password) {
        // [START auth_send_sign_in_link]
        auth = FirebaseAuth.getInstance();

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
                            if(task.getException() instanceof FirebaseAuthUserCollisionException) {
                                //txtError.setText("The email is already registered");
                                //txtError.setTextColor(getColor(android.R.color.holo_red_dark));
                                Toast.makeText(getApplicationContext(), "The email is already registered", Toast.LENGTH_LONG).show();
                            }
                            else
                                Toast.makeText(getApplicationContext(), "Unknown error.", Toast.LENGTH_LONG).show();
                        }

                    }
                });
        // [END auth_send_sign_in_link]
    }

    private static String generateVereficationCode()
    {
        Random ran = new Random();
        char data;
        String dat = "";

        for (int i=0; i<6; i++) {
            data = (char)(ran.nextInt(25)+97);
            dat = dat + data;
        }

        return dat;
    }

    public boolean VerificationCodeIsValid(String verificationCode) {
        if(null != verificationCode && verificationCode.equals(randomString))
            return true;

        return false;
    }

    public boolean EmailIsVerified(){
        auth.getCurrentUser().reload();
        return auth.getCurrentUser().isEmailVerified();
    }

    public boolean SaveVerificationData(){
        return editor.commit();//save data
    }
}
