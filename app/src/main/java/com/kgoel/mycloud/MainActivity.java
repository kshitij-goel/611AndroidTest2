package com.kgoel.mycloud;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNReconnectionPolicy;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import org.json.JSONObject;

import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final String subscriberKey = "sub-c-b3f1894c-b61b-11e8-9c8c-5aa277adf39c";
    static final String publisherKey = "pub-c-a48eea9b-bec6-437f-a198-c629b1d05c4c";
    static final String subscribeChannel = "Hub Channel";
    static final String publishChannel = "Mobile Channel";
    PubNub pubNub;

    private Devices[] devices = {
            new Devices (R.string.raspberry_pi_1, R.drawable.rpi1),
            new Devices (R.string.raspberry_pi_2, R.drawable.rpi1)
    };

    private PubNub pubNubInitialisation() {
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(subscriberKey);
        pnConfiguration.setPublishKey(publisherKey);
//        pnConfiguration.setSecure(true);
        pnConfiguration.setReconnectionPolicy(PNReconnectionPolicy.LINEAR);
        PubNub pub = new PubNub(pnConfiguration);
        pub.grant().channels(Arrays.asList(publishChannel,subscribeChannel)).authKeys(Arrays.asList(publisherKey,subscriberKey)).ttl(10000).read(true).write(true);
        return pub;
    }

    private void pubNubPublish(PubNub pubNub, TransmitObject obj){
        JSONObject jsonObject = obj.toJSON();
        pubNub.publish().message(jsonObject).channel(publishChannel)
                .async(new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        // handle publish result, status always present, result if successful
                        // status.isError() to see if error happened
                        if(!status.isError()) {
//                            System.out.println("pub timetoken: " + result.getTimetoken());
                            Log.d("kshitij","Publish success at time:" + result.getTimetoken());
                        }
                        else {
                            Log.d("kshitij", "Publish fail with code:" + status.getStatusCode());

//                        System.out.println("pub status code: " + status.getStatusCode());
                        }
                    }
                });
    }

    private void pubNubSubscribe(PubNub pubNub){
        pubNub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {

            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                TransmitObject transmitObject = new TransmitObject();
                Log.d("kshitij","Listener received message: "+ transmitObject.deviceType);
                transmitObject.deviceType = String.valueOf(message.getMessage().getAsJsonObject().get("deviceType"));
                Log.d("kshitij","Listener received message: "+ transmitObject.message);
                transmitObject.message = String.valueOf(message.getMessage().getAsJsonObject().get("message"));
                PassClass passClass = new PassClass();
                passClass.transmitObject = transmitObject;
                passClass.pubNub = pubnub;
                new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, passClass);
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {

            }
        });
        pubNub.subscribe().channels(Arrays.asList(subscribeChannel)).execute();
    }

    private class ServerTask extends AsyncTask<PassClass, Void, Void>{

        @Override
        protected Void doInBackground(PassClass... passClasses) {
            PassClass passClass = passClasses[0];
            if(isCancelled()){
                return null;
            }
            else{

                publishProgress();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    private class ClientTask extends AsyncTask<PassClass, String, Void>{

        @Override
        protected Void doInBackground(PassClass... passClasses) {
            String msgToSend = passClasses[0].transmitObject.message;
            PubNub pubNub = passClasses[0].pubNub;
            Log.d("kshitij","ClientTask msg to send: " + msgToSend);
            for(int i=0;i<5;i++){
                String msg="Testing "+i;
                Log.d("kshitij","Publishing: "+ msg);
                TransmitObject transmitObject = new TransmitObject();
                transmitObject.message=msg;
                transmitObject.deviceType="android";
                pubNubPublish(pubNub, transmitObject);
                Log.d("kshitij","After pubnub publish iteration: " + i);
            }
            return null;
        }
    }

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
//                if(device.getName())
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

//        Button RaspberryPi1 = findViewById(R.id.button1);
//        RaspberryPi1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent deviceIntent = new Intent(getApplicationContext(), rpi1.class);
//                startActivity(deviceIntent);
//            }
//        });

        Log.d("kshitij","Entering pi1 provider");
//        pubNub = pubNubInitialisation();
        Log.d("kshitij","After pubnub initialisation");

//        pubNubSubscribe(pubNub);

        Log.d("kshitij","After pubnub addListener");
        Log.d("kshitij","After pubnub subscribe");



//        String testSend = "Test send from android app to ClientTask";
//        PassClass passClass = new PassClass();
//        passClass.transmitObject.message = testSend;
//        passClass.pubNub = pubNub;
//        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        pubNub.unsubscribeAll();
//        new ServerTask().cancel(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        pubNubSubscribe(pubNub);
//        new ServerTask().cancel(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        pubNub.unsubscribeAll();
    }

    private class PassClass {
        PubNub pubNub;
        TransmitObject transmitObject;
    }
}
