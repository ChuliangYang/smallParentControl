package com.example.cl.customshutdown;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by CL on 7/13/17.
 */

public class LaunchAndShutDownReceiver extends BroadcastReceiver {

    private Context context;

    static Process createSuProcess(String cmd) throws IOException {

        DataOutputStream os = null;
        Process process = createSuProcess();

        try {
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit $?\n");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }

        return process;
    }

    static Process createSuProcess() throws IOException {
        File rootUser = new File("/system/xbin/ru");
        if (rootUser.exists()) {
            return Runtime.getRuntime().exec(rootUser.getAbsolutePath());
        } else {
            return Runtime.getRuntime().exec("su");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        if (intent.getAction().equals("com.example.cl.customshutdown.timeup") && intent.getBooleanExtra("timeup", false)) {
            try {
                createSuProcess("reboot -p").waitFor(); //关机
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            final ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
            String cmpNameTemp = null;
            if (runningTaskInfos != null) {
                cmpNameTemp = runningTaskInfos.get(0).topActivity.toString();
            }
            Log.d("onreceive", cmpNameTemp);
            if (cmpNameTemp != null && cmpNameTemp.contains("com.example.cl.customshutdown.activity")) {
                Log.d("onreceive", "在前台");
            } else {
                Log.e("onreceive", "在后台，启动");
                Intent i = new Intent(context, TimeService.class);
                i.putExtra("action", intent.getAction());
                context.startService(i);
            }
        }

    }
}
