package com.h4112.androidparking;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;

public class AntiTheftService extends Service implements FetchParkingSpotsTask.Callback {
    private static final int UPDATE_INTERVAL = 10000;

    private int id;

    private Handler handler;
    private Runnable runnable;
    private FetchParkingSpotsTask task;

    public AntiTheftService() {
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            id = intent.getIntExtra("id", 0);
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            id = prefs.getInt("parkid", 0);
        }

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.anti_theft_title))
                .setContentText(getString(R.string.anti_theft_text))
                .setOngoing(true)
                .build();

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotifyMgr.notify(100, notification);

        Log.d("AntiTheftService", "It should notify.");

        handler = new Handler();
        startTimer();

        return 0;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startTimer() {
        runnable = new Runnable() {
            @Override
            public void run() {
                runUpdatePlaces();
                startTimer();
            }
        };

        handler.postDelayed(runnable, UPDATE_INTERVAL);
    }

    @Override
    public void onDestroy() {
        Log.d("AntiTheftService", "Stopping service!");

        if(runnable != null) handler.removeCallbacks(runnable);
        if(task != null) task.cancel(true);

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotifyMgr.cancel(100);

        Log.d("AntiTheftService", "It should cancel.");
    }

    private void runUpdatePlaces() {
        if(task != null) {
            Log.d("AntiTheftService", "Launch request -> Do not want");
            return;
        }

        task = new FetchParkingSpotsTask(this, this);
        task.execute(new FetchParkingSpotsTask.Params(FetchParkingSpotsTask.Params.Mode.BY_ID,
                0, 0, 0, id));
    }

    @Override
    public void listePlacesFailure(FetchParkingSpotsTask.Params params) {
        Log.w("AntiTheftService", "Could not get spot list!");

        task = null;
    }

    @Override
    public void setListePlaces(ArrayList<PlaceParking> parks, FetchParkingSpotsTask.Params params) {
        PlaceParking theOne = null;
        for(PlaceParking p : parks) {
            if(p.getId() == id) {
                theOne = p;
                break;
            }
        }

        if(theOne == null) {
            Log.e("AntiTheftService", "Spot does not exist!?");
        } else {
            if(theOne.getEtat() != PlaceParking.Etat.OCCUPEE
                    && theOne.getEtat() != PlaceParking.Etat.INCONNU) {
                Log.i("AntiTheftService", "It moved!");

                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                Notification notification = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(R.string.car_moving_title))
                        .setContentText(getString(R.string.car_moving_text))
                        .setSound(alarmSound)
                        .build();

                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                mNotifyMgr.notify(101, notification);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor edit = prefs.edit();
                edit.remove("positionlat");
                edit.remove("positionlong");
                edit.remove("parkid");
                edit.apply();

                stopSelf();
            } else {
                Log.d("AntiTheftService", "It didn't move.");
            }
        }

        task = null;
    }
}
