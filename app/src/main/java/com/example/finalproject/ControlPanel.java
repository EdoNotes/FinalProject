package com.example.finalproject;
import static com.example.finalproject.Welcome.editor;
import static com.example.finalproject.Welcome.sharedPreferences;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ControlPanel extends AppCompatActivity
{
    Spinner dropdown;
    Button btnChangePassword;
    Button btnShowDataLog;
    EditText input;
    AlertDialog ad;
    String[] drop_items=new String[]{getString(R.string.NetConPerson),getString(R.string.NetConPorn),getString(R.string.NetConBlood),getString(R.string.NetConShoppingAds)};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);
        dropdown=(Spinner) findViewById(R.id.spinner);
        btnChangePassword=(Button) findViewById(R.id.btnChangePassword);
        btnShowDataLog=(Button) findViewById(R.id.BtnShowDataLog);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, drop_items);
        dropdown.setAdapter(adapter);
        AlertDialog.Builder builder =new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.TitleChangePassword));
        builder.setMessage(getString(R.string.MessageEnterNewPassword));
        input=new EditText(this);
        TextView lbl=new TextView(this);
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.PositiveButtonOk), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String txtNewPass=input.getText().toString();
                Toast.makeText(getApplicationContext(),getString(R.string.PasswordSavedMessage),Toast.LENGTH_SHORT).show();
                sharedPreferences.edit().putString(getString(R.string.PasswordKey),txtNewPass).apply();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Do Nothing
            }
        });
        ad=builder.create();
        btnChangePassword.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                ad.show();
            }
        });
        btnShowDataLog.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dataLogActivity=new Intent(getBaseContext(),DataLog.class);
                startActivity(dataLogActivity);
            }
        });

    }

}
