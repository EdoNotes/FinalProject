package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
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
                    txtError.setText("Please fill all required fields");
                    txtError.setTextColor(getColor(android.R.color.holo_red_dark));
                }//empty field
                else
                {//all required fields ok
                    if(rePassword.getText().toString().length()!=6||password.getText().toString().length()!=6)
                    {
                        txtError.setText("Password must be 6 digits");
                        txtError.setTextColor(getColor(android.R.color.holo_red_dark));
                    }
                    else
                    {
                        if(rePassword.getText().toString().equals(password.getText().toString())==false)
                        {
                            txtError.setText("Passwords difference\ntry Again!");
                            txtError.setTextColor(getColor(android.R.color.holo_red_dark));
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
                                editor.commit();//save data
                                Intent LoginActivity=new Intent(getBaseContext(),Login.class);
                                startActivity(LoginActivity);
                            }
                    }

                }
            }
        });
    }
}
