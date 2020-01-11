package com.example.finalproject;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class DataLog extends AppCompatActivity {
 ListView dataLog;
 List<String> dataLogItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_log);
        dataLog=(ListView)findViewById(R.id.listdataLog);
        dataLogItems=new ArrayList<String>();
        dataLogItems.add("Item test");
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                dataLogItems );

        dataLog.setAdapter(arrayAdapter);
    }
}
