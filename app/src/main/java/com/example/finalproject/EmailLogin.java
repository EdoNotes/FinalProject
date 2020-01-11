package com.example.finalproject;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import static com.example.finalproject.Welcome.sharedPreferences;

public class EmailLogin extends AppCompatActivity {

    private EditText input1;
    private EditText input2;
    private AlertDialog ad;
    private FirebaseAuth auth = null;
    private Context context = null;
    private Handler handler = null;
    private Button btnLogin = null;

    public EmailLogin(Context con , Button btn , Handler hand){
        context = con;
        btnLogin = btn;
        handler = hand;
    }

    public int EmailLoginDialog() {

        if(null == context || null == btnLogin || null == handler) {
            Log.d("EmailLoginDialog", "One of the global parameters is null");
            return -1;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Login");
        builder.setMessage("Enter valid email and password:");

        input1 = new EditText(context);
        input2 = new EditText(context);
        input1.setHint("Email");
        input2.setHint("Password");
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(input1);
        ll.addView(input2);
        builder.setView(ll);

        builder.setPositiveButton("Login", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String txtEmail = input1.getText().toString();
                final String txtPassword = input2.getText().toString();

                if(txtEmail.isEmpty() || txtPassword.isEmpty()) {
                    Toast.makeText(context, "Invalid Email/Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                input1.setText("");
                input2.setText("");
                //sharedPreferences.edit().putString("Password",txtNewPass).apply();

                auth = FirebaseAuth.getInstance();

                auth.signInWithEmailAndPassword(txtEmail, txtPassword)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Message completeMessage;
                                    sharedPreferences.edit().putString("Password",txtPassword).apply();
                                    sharedPreferences.edit().putString("Email",txtEmail).apply();

                                    Toast.makeText(context, "Login successful!", Toast.LENGTH_LONG).show();

                                    completeMessage = handler.obtainMessage(1, 0);
                                    completeMessage.sendToTarget();
                                }
                                else {
                                    Toast.makeText(context, "Login failed! " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                input1.setText("");
                input2.setText("");
            }
        });
        ad = builder.create();
        btnLogin.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.show();
            }
        });

        return 0;
    }
}
