package com.example.finalproject;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DataLog extends AppCompatActivity {

 private static ListView dataLog;
 public static ArrayAdapter<String> arrayAdapter;
 public static List<String> dataLogItems =new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_log);
        dataLog=(ListView)findViewById(R.id.listdataLog);

        //dataLogItems.add("Gotcha,item has been capture on"+ currentTime);

        arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                dataLogItems );

        dataLog.setAdapter(arrayAdapter);
    }

    /*@Override
    protected void onPause() {
        super.onPause();
        setContentView(R.layout.activity_data_log);
        dataLog=(ListView)findViewById(R.id.listdataLog);
        dataLogItems=new ArrayList<String>();
        Date currentTime = Calendar.getInstance().getTime();
        arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                dataLogItems );

        dataLog.setAdapter(arrayAdapter);
    }*/

}
