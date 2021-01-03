package com.dev.callrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



/*
* MyService runs in background, which receives the broadcast
* when the intents are delivered.
*
* The Audio is captured through MIC or VOICE_COMMUNICATION
*
* StartForeground() - Shows BackgroundNotification
* createAndRecord() - Create a file and prepare the media-recorder
*
*
*
* */

public class CallService extends Service {
    private MediaRecorder recorder;
    private AudioManager audioManager;
    private boolean recordStarted = false;
    File file;
    public static CallService instance = null;
    public static final String NOTIFICATION_CHANNEL_ID = "Channel 1";

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        StartForeground();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("StopService");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        this.registerReceiver(new PhonecallReceiver(), intentFilter);


        return START_STICKY;

    }



    public class PhonecallReceiver extends BroadcastReceiver{

        Bundle bundle;
        String state;
        String Incoming, Outgoing;
        boolean wasRinging;

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && intent.getAction().equals("android.intent.action.PHONE_STATE")) {

                if ((bundle = intent.getExtras()) != null) {
                    state = bundle.getString(TelephonyManager.EXTRA_STATE);
                        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                            Incoming = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                            wasRinging = true;
                            Toast.makeText(context, "IN : " + Incoming, Toast.LENGTH_LONG).show();
                        }

                        else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                            Toast.makeText(context, "ANSWERED", Toast.LENGTH_LONG).show();
                            createAndRecord();
                        }

                        else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                            wasRinging = false;
                            Toast.makeText(context, "COMPLETED", Toast.LENGTH_LONG).show();
                            if(recordStarted){
                                recorder.stop();
                                recorder.reset();
                                recorder.release();
                                recordStarted = false;
                            }
                        }
                    }
                }

            else if(intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")){
                Outgoing = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                wasRinging = true;
                Toast.makeText(context, "OutGoing " + Outgoing, Toast.LENGTH_SHORT).show();
            }
        }
    }





    private void StartForeground() {
        String ChannelID = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ChannelID = createNotificationChannel("My_service", NOTIFICATION_CHANNEL_ID);
        } else {
            ChannelID = NOTIFICATION_CHANNEL_ID;
        };


        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ChannelID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("CALL RECORDER ")
                .setContentText("App is running in Background")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(101, notification);
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel(String ChannelName, String Channel_ID) {
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, ChannelName, NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);
        return NOTIFICATION_CHANNEL_ID;

    }




    //Create a File and Record the info

    private void createAndRecord() {
        AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
        }
            File sampleDir;

            sampleDir = new File(Environment.getExternalStorageDirectory(), "/AutomaticCallRecorder1");
            if (!sampleDir.exists()) {
                sampleDir.mkdirs();
            }


            String time = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
            String file_name = "Record-" +time + "-";
            try {
                file = File.createTempFile(file_name, ".3gp", sampleDir);
            } catch (IOException e) {
                e.printStackTrace();
            }


//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                try {
//                    while(true) {
//                        sleep(1000);
//                        audioManager.setMode(AudioManager.MODE_IN_CALL);
//                        if (!audioManager.isSpeakerphoneOn())
//
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//
//        thread.start();




//            audioManager.setMode(AudioManager.MODE_IN_CALL);
//            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
//            audioManager.setMode(AudioManager.MODE_NORMAL);
//            recorder.setAudioSamplingRate(44100);


//            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
//                //callType = "VOICE_CALL";
//            } else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                //callType = "MIC";
//            } else {
//                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
//                // callType = "VOICE_COMMUNICATION";
//            }

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(file.getAbsolutePath());
        try {
            recorder.prepare();
            recorder.start();
            recordStarted = true;
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }


}
