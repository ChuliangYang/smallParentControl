package com.example.cl.customshutdown;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by CL on 7/13/17.
 */

public class TimeService extends Service {
    static Boolean canPass = false;
    static Boolean activityhide = true;
    SharedPreferences sp;
    Timer timer;
    TimerTask timerTask;
    //监听时间变化的 这个receiver只能动态创建
//    private TimeTickReceiver mTickReceiver;
    private IntentFilter mFilter;
    private boolean fromBoot = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        timer = new Timer();

        sp = getSharedPreferences("operationLog", MODE_MULTI_PROCESS);
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.d("canpass&activityhide", canPass + " " + activityhide);
                if (canPass) {
                    timer.cancel();
//                    timerTask.cancel();
                    stopSelf();
//                    stopForeground(true);

                }
                if (!canPass && activityhide) {
                    startActivity(new Intent(getBaseContext(), activity.class));

                    Intent it1 = new Intent(new Intent("com.dbjtech.dsz.destroy"));
                    it1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//it.putExtra("key","value"); //传一些参数
                    startActivity(it1);

                    Intent it = new Intent();
                    it.setComponent(new ComponentName("com.example.cl.customshutdown", "com.example.cl.customshutdown.activity")); //包名和类名
//it.putExtra("key","value"); //传一些参数
                    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(it);

                    Intent it2 = new Intent();
                    it2.setClassName("com.example.cl.customshutdown", "com.example.cl.customshutdown.activity"); //包名和类名
//it.putExtra("key","value"); //传一些参数
                    it2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(it2);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("启动了服务");
//        Notification notification = new Notification.Builder(this.getApplicationContext()).setContentText("这是一个前台服务").setSmallIcon(R.mipmap.ic_launcher).setWhen(System.currentTimeMillis()).build();
        if (intent.getStringExtra("action") != null) {
            fromBoot = intent.getStringExtra("action").equals("android.intent.action.BOOT_COMPLETED");
        } else {
            fromBoot = false;
        }
        Log.d("onStartCommand", String.valueOf(fromBoot));

//        startForeground(1232,notification);

        if (fromBoot && !sp.getBoolean("launch_when_device_start", true)) {
            Log.d("开机启动被取消", "开机启动被取消");
            canPass = true;
            stopSelf();
        } else {
            Log.d("开机启动有效", "尝试启动activity");
            timer.schedule(timerTask, 0, 1000);
        }
        return Service.START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
//        stopForeground(true);
//        unregisterReceiver(mTickReceiver);
        if (!canPass) {
            Intent intent = new Intent("com.dbjtech.waiqin.destroy");
            sendBroadcast(intent);
        }
        System.out.println("销毁了服务");

    }
}
