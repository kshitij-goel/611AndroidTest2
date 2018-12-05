package com.kgoel.mycloud;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
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

    static final String subscriberKey = "sub-x-key";
    static final String publisherKey = "pub-x-key";
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
    static String sensorDist_global;
    static String flagtime = "";

    public static String getOnStatus(String string){
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

    public static void setOnStatus(@Nullable String red, @Nullable String yellow, @Nullable String green, @Nullable String sensorDist){
        if(red!=null && red.compareTo("0")==0)
            redStatus = false;
        else if(red!=null && red.compareTo("1")==0)
            redStatus = true;
        if(yellow!=null && yellow.compareTo("0")==0)
            yellowStatus = false;
        else if(yellow!=null && yellow.compareTo("1")==0)
            yellowStatus = true;
        if(green!=null && green.compareTo("0")==0)
            greenStatus = false;
        else if(green!=null && green.compareTo("1")==0)
            greenStatus = true;
        sensorDist_global = sensorDist;
    }

    public static void setDisStatus(@Nullable String over, @Nullable String red, @Nullable String yellow, @Nullable String green){
        if(over!=null && over.compareTo("dis")==0)
            overrideDis = true;
        else if(over!=null && over.compareTo("en")==0)
            overrideDis = false;
        if(red!=null && red.compareTo("dis")==0)
            redDis = true;
        else if(red!=null && red.compareTo("en")==0)
            redDis = false;
        if(yellow!=null && yellow.compareTo("dis")==0)
            yellowDis = true;
        else if(yellow!=null && yellow.compareTo("en")==0)
            yellowDis = false;
        if(green!=null && green.compareTo("dis")==0)
            greenDis = true;
        else if(green!=null && green.compareTo("en")==0)
            greenDis = false;
//        return String.valueOf(overrideDis)+"#"+String.valueOf(redDis)+"#"+String.valueOf(yellowDis)+"#"+String.valueOf(greenDis);
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
        pub.grant().channels(Arrays.asList(publishChannel,subscribeChannel)).authKeys(Arrays.asList(publisherKey,subscriberKey)).read(true).write(true);
        return pub;
    }

    private void pubNubPublish(PubNub pubNub, TransmitObject obj){
        if(!overrideDis) {
            Log.d("kshitij", "Pubnub Publishing message: " + obj.message);
            Log.d("kshitij", "Pubnub Publishing deviceType: " + obj.deviceType);
            JSONObject jsonObject = obj.toJSON();
            pubNub.publish().message(jsonObject).channel(publishChannel)
                    .async(new PNCallback<PNPublishResult>() {
                        @Override
                        public void onResponse(PNPublishResult result, PNStatus status) {
                            // handle publish result, status always present, result if successful
                            // status.isError() to see if error happened
                            if (!status.isError()) {
//                            System.out.println("pub timetoken: " + result.getTimetoken());
//                            Log.d("kshitij","Publish success at time:" + result.getTimetoken());
                            } else {
//                            Log.d("kshitij", "Publish fail with code:" + status.getStatusCode());

//                        System.out.println("pub status code: " + status.getStatusCode());
                            }
                        }
                    });
        }
    }

    private void pubNubSubscribe(PubNub pubNub){
        pubNub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
//                Log.d("kshitij","StatusCode number: " + status.getStatusCode());
//                Log.d("kshitij","StatusCode string: " + status.toString());
//                Log.d("kshitij","StatusCode error: " + status.isError());
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                TransmitObject transmitObject = new TransmitObject();
                Log.d("kshitij","////////////////////////////////Received message: "+ message.getMessage());
                String time = message.getTimetoken().toString();
                if(flagtime.compareTo(time)!=0) {
                    transmitObject.deviceType = String.valueOf(message.getMessage().getAsJsonObject().get("map").getAsJsonObject().get("deviceType").toString().replace("\"", ""));
                    Log.d("kshitij", "Listener received message: " + transmitObject.deviceType);
                    transmitObject.message = String.valueOf(message.getMessage().getAsJsonObject().get("map").getAsJsonObject().get("message").toString().replace("\"", ""));
                    Log.d("kshitij", "Listener received message: " + transmitObject.message);
                    PassClass passClass = new PassClass();
                    passClass.transmitObject = transmitObject;
                    passClass.pubNub = pubnub;
                    flagtime = time;
                    new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, passClass);
                }
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {

            }
        });
        pubNub.subscribe().channels(Arrays.asList(subscribeChannel)).execute();
    }

    private class ServerTask extends AsyncTask<PassClass, Void, Void>{
//    private class ServerTask{

        String flag = "";

        @Override
        protected Void doInBackground(PassClass... passClasses) {
            PassClass passClass = passClasses[0];
            if(isCancelled()){
                return null;
            }
            else{
                if(passClass.transmitObject.message.compareTo(flag)==0){
                    Log.d("kshitij","continue");
                }
                else {
                    String rec = passClass.transmitObject.message;
                    String[] recs = rec.split("#");
//                Log.d("kshitij","Updating UI with message: "+rec);
                    if (passClass.transmitObject.deviceType.compareTo("android") == 0 || passClass.transmitObject.deviceType.compareTo("webapp") == 0) {
                        if (recs[3].compareTo("1") == 0 && recs[1].compareTo(macAddress) != 0) {
                            setDisStatus("dis", "dis", "dis", "dis");
                            publishProgress();
                        } else if (recs[3].compareTo("0") == 0 && recs[1].compareTo(macAddress) != 0) {
                            setDisStatus("en", "dis", "dis", "dis");
                            publishProgress();
                        }
                    } else if (passClass.transmitObject.deviceType.compareTo("hub") == 0) {
                        Log.d("kshitij", "all on status================== " + getOnStatus("override") + getOnStatus("red") + getOnStatus("yellow") + getOnStatus("green") + " " + macAddress);
                        Log.d("kshitij", "all dis status================== " + overrideDis + " " + redDis + " " + yellowDis + " " + greenDis + " " + macAddress);
                        if (!overrideDis) {
                            setOnStatus(recs[3], recs[5], recs[7], recs[9]);
//                            Log.d("kshitij","here");
                            publishProgress();
                        } else {
                            if (overrideStatus) {
                                Log.d("kshitij", "Ignoring UI update");
                            } else {
                                setOnStatus(recs[3], recs[5], recs[7], recs[9]);
//                                Log.d("kshitij","here2");
                                Log.d("kshitij", "all on status================== " + getOnStatus("override") + getOnStatus("red") + getOnStatus("yellow") + getOnStatus("green") + " " + macAddress);
                                Log.d("kshitij", "all dis status================== " + overrideDis + " " + redDis + " " + yellowDis + " " + greenDis + " " + macAddress);
                                publishProgress();
                            }
                        }
                    }
                    flag=passClass.transmitObject.message;
                }
            }
            return null;
        }

        protected void onProgressUpdate(Void... voids) {
            super.onProgressUpdate(voids);
            Switch overRide = findViewById(R.id.OverRide_Switch);
            Switch red = findViewById(R.id.switchRed);
            Switch yellow = findViewById(R.id.switchYellow);
            Switch green = findViewById(R.id.switchGreen);

            TextView redText = findViewById(R.id.textViewRed);
            TextView yellowText = findViewById(R.id.textViewYellow);
            TextView greenText = findViewById(R.id.textViewGreen);

            TextView sensor = findViewById(R.id.textViewSensor);
            sensor.setText(sensorDist_global);

            overRide.setEnabled(!overrideDis);
//            Log.d("kshitij","OverRide disable status: "+ overrideDis);
//            Log.d("kshitij","Red disable status: "+ redDis);
//            Log.d("kshitij","Yellow disable status: "+ yellowDis);
//            Log.d("kshitij","Green disable status: "+ greenDis);
            red.setChecked(redStatus);
            yellow.setChecked(yellowStatus);
            green.setChecked(greenStatus);
            if(redStatus)
                redText.setBackgroundColor(Color.parseColor("#fc2828"));
            else if(!redStatus)
                redText.setBackgroundColor(Color.parseColor("#ffffff"));
            if(yellowStatus)
                yellowText.setBackgroundColor(Color.parseColor("#ffee00"));
            else if(!yellowStatus)
                yellowText.setBackgroundColor(Color.parseColor("#ffffff"));
            if(greenStatus)
                greenText.setBackgroundColor(Color.parseColor("#0ad100"));
            else if(!greenStatus)
                greenText.setBackgroundColor(Color.parseColor("#ffffff"));
        }
    }

    private class ClientTask extends AsyncTask<PassClass, String, Void>{

        @Override
        protected Void doInBackground(PassClass... passClasses) {
            pubNubPublish(passClasses[0].pubNub, passClasses[0].transmitObject);
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rpi1);
        setTitle("Raspberry Pi 1");

        macAddress = getMacAddress();

        pubNub = pubNubInitialisation();

        final PassClass passClass = new PassClass();
        TransmitObject transmitObject = new TransmitObject();
        passClass.transmitObject = transmitObject;
        passClass.pubNub = pubNub;
        passClass.transmitObject.deviceType = "android";

        pubNubSubscribe(pubNub);

        final Switch overRide = findViewById(R.id.OverRide_Switch);
        final Switch red = findViewById(R.id.switchRed);
        final Switch yellow = findViewById(R.id.switchYellow);
        final Switch green = findViewById(R.id.switchGreen);

        final TextView redText = findViewById(R.id.textViewRed);
        final TextView yellowText = findViewById(R.id.textViewYellow);
        final TextView greenText = findViewById(R.id.textViewGreen);

        red.setEnabled(redDis);
        yellow.setEnabled(yellowDis);
        green.setEnabled(greenDis);
        overRide.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(!overrideDis) {
                    if (isChecked) {
                        redDis = true;
                        yellowDis = true;
                        greenDis = true;
                        red.setEnabled(redDis);
                        yellow.setEnabled(yellowDis);
                        green.setEnabled(greenDis);
                        overrideStatus = true;
                        passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#" + getOnStatus("override") + "#red#" + getOnStatus("red") + "#yellow#" + getOnStatus("yellow") + "#green#" + getOnStatus("green");
//                    Log.d("kshitij","Calling clientTask override if");
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                    } else {
                        redDis = false;
                        yellowDis = false;
                        greenDis = false;
                        overrideStatus = false;
                        red.setEnabled(redDis);
                        yellow.setEnabled(yellowDis);
                        green.setEnabled(greenDis);
                        red.setChecked(false);
                        yellow.setChecked(false);
                        green.setChecked(false);
                        passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#" + getOnStatus("override") + "#red#" + getOnStatus("red") + "#yellow#" + getOnStatus("yellow") + "#green#" + getOnStatus("green");
//                    Log.d("kshitij","Calling clientTask override else");
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                    }
                }
            }
        });


        red.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && redDis == true) {
//                   Log.d("kshitij", "Setting red to true");
                    redStatus = true;
                    redText.setBackgroundColor(Color.parseColor("#fc2828"));
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#" + getOnStatus("override") + "#red#" + getOnStatus("red") + "#yellow#" + getOnStatus("yellow") + "#green#" + getOnStatus("green");
//                  Log.d("kshitij","Calling clientTask red if");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                } else if (redDis == true) {
//                  Log.d("kshitij", "Setting red to false");
                    redStatus = false;
                    redText.setBackgroundColor(Color.parseColor("#ffffff"));
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#" + getOnStatus("override") + "#red#" + getOnStatus("red") + "#yellow#" + getOnStatus("yellow") + "#green#" + getOnStatus("green");
//                Log.d("kshitij","Calling clientTask red else");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
            }
        });

        yellow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked && yellowDis == true) {
//                Log.d("kshitij", "Setting yellow to true");
                    yellowStatus = true;
                    yellowText.setBackgroundColor(Color.parseColor("#ffee00"));
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#" + getOnStatus("override") + "#red#" + getOnStatus("red") + "#yellow#" + getOnStatus("yellow") + "#green#" + getOnStatus("green");
//                Log.d("kshitij","Calling clientTask yellow if");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                } else if (yellowDis == true) {
//                Log.d("kshitij", "Setting yellow to false");
                    yellowStatus = false;
                    yellowText.setBackgroundColor(Color.parseColor("#ffffff"));
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#" + getOnStatus("override") + "#red#" + getOnStatus("red") + "#yellow#" + getOnStatus("yellow") + "#green#" + getOnStatus("green");
//                   Log.d("kshitij","Calling clientTask yellow else");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
            }
        });

        green.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && greenDis == true) {
//                  Log.d("kshitij", "Setting green to true");
                    greenStatus = true;
                    greenText.setBackgroundColor(Color.parseColor("#0ad100"));
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#" + getOnStatus("override") + "#red#" + getOnStatus("red") + "#yellow#" + getOnStatus("yellow") + "#green#" + getOnStatus("green");
//                   Log.d("kshitij","Calling clientTask green if");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                } else if (greenDis == true) {
//                  Log.d("kshitij", "Setting green to false");
                    greenStatus = false;
                    greenText.setBackgroundColor(Color.parseColor("#ffffff"));
                    passClass.transmitObject.message = passClass.transmitObject.deviceType + "#" + macAddress + "#override#" + getOnStatus("override") + "#red#" + getOnStatus("red") + "#yellow#" + getOnStatus("yellow") + "#green#" + getOnStatus("green");
//                   Log.d("kshitij","Calling clientTask green else");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passClass);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        pubNub.unsubscribeAll();
        new ServerTask().cancel(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pubNubSubscribe(pubNub);
        new ServerTask().cancel(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pubNub.unsubscribeAll();
    }

    private class PassClass {
        PubNub pubNub;
        TransmitObject transmitObject;
    }
}
