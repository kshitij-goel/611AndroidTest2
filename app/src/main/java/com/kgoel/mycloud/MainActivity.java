package com.kgoel.mycloud;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

public class MainActivity extends AppCompatActivity {

    private Devices[] devices = {
            new Devices (R.string.raspberry_pi_1, R.drawable.rpi1),
            new Devices (R.string.raspberry_pi_2, R.drawable.rpi1)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridView gridView = findViewById(R.id.gridview);
        final GridAdapter gridadapter = new GridAdapter(this, devices);
        gridView.setAdapter(gridadapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Devices device = devices[position];

                Log.d("kshitij","Clicked id: "+ id+" position: "+position);
                if(position==0){
                    Intent deviceIntent = new Intent(getApplicationContext(), RPi1.class);
                    startActivity(deviceIntent);
                }
                else if(position==1){
                    Intent deviceIntent2 = new Intent(getApplicationContext(), RPi2.class);
                    startActivity(deviceIntent2);
                }


            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
