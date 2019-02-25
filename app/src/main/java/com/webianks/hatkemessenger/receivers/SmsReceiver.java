package com.webianks.hatkemessenger.receivers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.util.Log;

import com.webianks.hatkemessenger.R;
import com.webianks.hatkemessenger.activities.SmsDetailedView;
import com.webianks.hatkemessenger.constants.Constants;
import com.webianks.hatkemessenger.services.SaveSmsService;

import android.os.Vibrator;
/**
 * Created by R Ankit on 24-12-2016.
 */

public class SmsReceiver extends BroadcastReceiver {


    private String TAG = SmsReceiver.class.getSimpleName();
    private Bundle bundle;
    private SmsMessage currentSMS;
    private int mNotificationId = 101;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {

            Log.e(TAG, "smsReceiver");

            bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdu_Objects = (Object[]) bundle.get("pdus");
                if (pdu_Objects != null) {

                    for (Object aObject : pdu_Objects) {

                        currentSMS = getIncomingMessage(aObject, bundle);

                        String senderNo = currentSMS.getDisplayOriginatingAddress();
                        String message = currentSMS.getDisplayMessageBody();
                        //Log.d(TAG, "senderNum: " + senderNo + " :\n message: " + message);

                        issueNotification(context, senderNo, message);
                        saveSmsInInbox(context,currentSMS);


                    }
                    this.abortBroadcast();
                    // End of loop
                }
            }
        } // bundle null
    }

    private void saveSmsInInbox(Context context, SmsMessage sms) {

        Intent serviceIntent = new Intent(context, SaveSmsService.class);
        serviceIntent.putExtra("sender_no", sms.getDisplayOriginatingAddress());
        serviceIntent.putExtra("message", sms.getDisplayMessageBody());
        serviceIntent.putExtra("date", sms.getTimestampMillis());
        context.startService(serviceIntent);

    }

    private void issueNotification(Context context, String senderNo, String message) {

        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.mipmap.ic_launcher);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setLargeIcon(icon)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(senderNo)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setAutoCancel(true)
                        .setContentText(message);

        System.out.println(message);
        String s = convertToMorse(message);
        vibrateMessage(context, s);
        System.out.println(s);


        Intent resultIntent = new Intent(context, SmsDetailedView.class);
        resultIntent.putExtra(Constants.CONTACT_NAME,senderNo);
        resultIntent.putExtra(Constants.FROM_SMS_RECIEVER,true);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

    }

    private SmsMessage getIncomingMessage(Object aObject, Bundle bundle) {
        SmsMessage currentSMS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String format = bundle.getString("format");
            currentSMS = SmsMessage.createFromPdu((byte[]) aObject, format);
        } else {
            currentSMS = SmsMessage.createFromPdu((byte[]) aObject);
        }
        return currentSMS;
    }

    private String convertToMorse(String arg)
    {
        String[] english = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
                "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x",
                "y", "z", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
                ",", ".", "?" };

        String[] morse = { ".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..",
                ".---", "-.-", ".-..", "--", "-.", "---", ".---.", "--.-", ".-.",
                "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".----",
                "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.",
                "-----", "--..--", ".-.-.-", "..--.." };

        char[] chars = arg.toLowerCase().toCharArray();

        String str = "";
        for (int i = 0; i < chars.length; i++){
            for (int j = 0; j < english.length; j++){

                if (english[j].charAt(0) == chars[i]){
                    str = str + morse[j] + " ";
                }
            }
        }
        return str;
    }

    private void vibrateMessage(Context context, String str)
    {
        long DOT = 300;
        long DASH = DOT * 4;
        long SPACE = DOT / 2;
        long CHAR = SPACE * 4;
        long WORD = SPACE * 8;
        long STOP = WORD + CHAR;

        int count=1;
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] longs = new long[1000];
        longs[0] = 0;
        for(int i = 0, n = str.length() ; i < n ; i++) {
            char c = str.charAt(i);
            if(c=='.')
            {
                longs[count] = DOT;
                longs[count+1] = SPACE;
            }
            else if(c=='-')
            {
                longs[count] = DASH;
                longs[count+1] = SPACE;
            }
            else
            {
                longs[count] = 1;
                longs[count+1] = SPACE;
            }
            count=count+2;
        }
        System.out.println(longs);
        v.vibrate(longs, -1);
    }
}
