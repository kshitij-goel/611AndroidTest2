package com.kgoel.mycloud;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

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

public class RPi1 extends AppCompatActivity {

    static final String subscriberKey = "sub-c-b3f1894c-b61b-11e8-9c8c-5aa277adf39c";
    static final String publisherKey = "pub-c-a48eea9b-bec6-437f-a198-c629b1d05c4c";
    static final String subscribeChannel = "Hub Channel";
    static final String publishChannel = "Mobile Channel";
    PubNub pubNub;

    static boolean overrideStatus = false;
    static boolean redStatus = false;
    static boolean yellowStatus = false;
    static boolean greenStatus = false;
    static boolean overrideDis = false;
    static boolean redDis = false;
    static boolean yellowDis = false;
    static boolean greenDis = false;

    static String macAddress;

    public static String getStatus(String string){
        if(string.compareTo("override")==0){
            if(overrideStatus)
                return "1";
            else
                return "0";
        }
        else if(string.compareTo("red")==0){
            if(redStatus)
                return "1";
            else
                return "0";
        }
        else if(string.compareTo("yellow")==0){
            if(yellowStatus)
                return "1";
            else
                return "0";
        }
        else if(string.compareTo("green")==0){
            if(greenStatus)
                return "1";
            else
                return "0";
        }
        return null;
    }
    public static String setStatus(String over, String red, String yellow, String green){
        if(over.compareTo("dis")==0)
            overrideDis = true;
        else if(over.compareTo("en")==0)
            overrideDis = false;
        if(red.compareTo("dis")==0)
            redDis = true;
        else if(red.compareTo("en")==0)
            redDis = false;
        if(yellow.compareTo("dis")==0)
            yellowDis = true;
        else if(yellow.compareTo("en")==0)
            yellowDis = false;
        if(green.compareTo("dis")==0)
            greenDis = true;
        else if(green.compareTo("en")==0)
            greenDis = false;
        return String.valueOf(overrideDis)+"#"+String.valueOf(redDis)+"#"+String.valueOf(yellowDis)+"#"+String.valueOf(greenDis);
    }

    public static String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.getLocalizedMessage();
        }
        return "";
    }

    private PubNub pubNubInitialisation() {
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(subscriberKey);
        pnConfiguration.setPublishKey(publisherKey);
//        pnConfiguration.setSecure(true);
        pnConfiguration.setReconnectionPolicy(PNReconnectionPolicy.LINEAR);
        PubNub pub = new PubNub(pnConfiguration);
        pub.grant().channels(Arrays.asList(publishChannel,subscribeChannel)).authKeys(Arrays.asList(publisherKey,subscriberKey)).ttl(5).read(true).write(true);
        return pub;
    }

    private void pubNubPublish(PubNub pubNub, TransmitObject obj){
        Log.d("kshitij","Pubnub Publishing message: "+obj.message);
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

    private class ServerTask extends AsyncTask<PassClass, String, Void>{

        @Override
        protected Void doInBackground(PassClass... passClasses) {
            PassClass passClass = passClasses[0];
            if(isCancelled()){
                return null;
            }
            else{
                String rec = passClass.transmitObject.message;
                String[] recs = rec.split("#");
                if(passClass.transmitObject.deviceType.compareTo("android")==0) {
                    if (recs[3].compareTo("1") == 0 && recs[1].compareTo(macAddress) != 0) {
                        String send = setStatus("dis", "dis", "dis", "dis");
                        publishProgress(send);
                    } else if (recs[3].compareTo("0") == 0 && recs[1].compareTo(macAddress) != 0){
                        String send = setStatus("en","dis","dis","dis");
                        publishProgress(send);
                    }
                }
                else if(recs[0].compareTo("hub")==0){

                }

            }
            return null;
        }

        protected void onProgressUpdate(String... strings) {
            super.onProgressUpdate(strings);
            Switch overRide = findViewById(R.id.OverRide_Switch);
            Switch red = findViewById(R.id.switchRed);
            Switch yellow = findViewById(R.id.switchYellow);
            Switch green = findViewById(R.id.switchGreen);

            TextView redText = findViewById(R.id.textViewRed);
            TextView yellowText = findViewById(R.id.textViewYellow);
            TextView greenText = findViewById(R.id.textViewGreen);

            String[] recs = strings[0].split("#");

            overRide.setEnabled(Boolean.parseBoolean(recs[0]));
            red.setEnabled(Boolean.parseBoolean(recs[1]));
            yellow.setEnabled(Boolean.parseBoolean(recs[2]));
            green.setEnabled(Boolean.parseBoolean(recs[3]));
        }
    }

    private class ClientTask extends AsyncTask<PassClass, String, Void>{

        @Override
        protected Void doInBackground(PassClass... passClasses) {
            TransmitObject transmitObject = new TransmitObject();
            String msgToSend = passClasses[0].transmitObject.message;
            transmitObject.deviceType=passClasses[0].transmitObject.deviceType;
            PubNub pubNub = passClasses[0].pubNub;
            Log.d("kshitij","Pi1 ClientTask msg to send: " + msgToSend);
            Log.d("kshitij","Pi1 ClientTask deviceType: " + transmitObject.deviceType);
            transmitObject.message=msgToSend;
            pubNubPublish(pubNub, transmitObject);
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rpi1);
        setTitle("Raspberry Pi 1");

        macAddress = getMacAddress();
        Log.d("kshitij","MAC Address: "+ macAddress);

        Log.d("kshitij","Entering pi1 provider");
        pubNub = pubNubInitialisation();
        Log.d("kshitij","After pubnub initialisation");

//        String testSend = "Test send from android app to ClientTask";
//        passClass.transmitObject.message = testSend;
        final PassClass passClass = new PassClass();
        TransmitObject transmitObject = new TransmitObject();
        passClass.transmitObject = transmitObject;
        passClass.pubNub = pubNub;
        passClass.transmitObject.deviceType = "android";



        pubNubSubscribe(pubNub);

        Log.d("kshitij","After pubnub addListener");
        Log.d("kshitij","After pubnub subscribe");

        final Switch overRide = findViewById(R.id.OverRide_Switch);
        final Switch red = findViewById(R.id.switchRed);
        final Switch yellow = findViewById(R.id.switchYellow);
        final Switch green = findViewById(R.id.switchGreen);

        red.setEnabled(false);
        yellow.setEnabled(false);
        green.setEnabled(false);

        overRide.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    red.setEnabled(true);
                    yellow.setEnabled(true);
                    green.setEnabled(true);
                    overrideStatus = true;
                    passClass.transmitObject.message = passClass.transmitObject.deviceType+"#"+macAddress+"#override#"+getStatus("override")+"#red#"+getStatus("red")+"#yellow#"+getStatus("yellow")+"#green#"+getStatus("green");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
                else{
                    overrideStatus = false;
                    red.setEnabled(false);
                    yellow.setEnabled(false);
                    green.setEnabled(false);
                    red.setChecked(false);
                    yellow.setChecked(false);
                    green.setChecked(false);
                    passClass.transmitObject.message = passClass.transmitObject.deviceType+"#"+macAddress+"#override#"+getStatus("override")+"#red#"+getStatus("red")+"#yellow#"+getStatus("yellow")+"#green#"+getStatus("green");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
            }
        });


        red.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if(isChecked) {
                    Log.d("kshitij", "Setting red to true");
                    redStatus = true;
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#"+getStatus("override")+"#red#" + getStatus("red") + "#yellow#" + getStatus("yellow") + "#green#" + getStatus("green");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
                else{
                    Log.d("kshitij", "Setting red to false");
                    redStatus = false;
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#"+getStatus("override")+"#red#" + getStatus("red") + "#yellow#" + getStatus("yellow") + "#green#" + getStatus("green");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
            }
        });

        yellow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if(isChecked) {
                    Log.d("kshitij", "Setting yellow to true");
                    yellowStatus = true;
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#"+getStatus("override")+"#red#" + getStatus("red") + "#yellow#" + getStatus("yellow") + "#green#" + getStatus("green");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
                else{
                    Log.d("kshitij", "Setting yellow to false");
                    yellowStatus = false;
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#"+getStatus("override")+"#red#" + getStatus("red") + "#yellow#" + getStatus("yellow") + "#green#" + getStatus("green");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
            }
        });

        green.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if(isChecked) {
                    Log.d("kshitij", "Setting green to true");
                    greenStatus = true;
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#"+getStatus("override")+"#red#" + getStatus("red") + "#yellow#" + getStatus("yellow") + "#green#" + getStatus("green");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
                else{
                    Log.d("kshitij", "Setting green to false");
                    greenStatus = false;
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#"+getStatus("override")+"#red#" + getStatus("red") + "#yellow#" + getStatus("yellow") + "#green#" + getStatus("green");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
            }
        });
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
