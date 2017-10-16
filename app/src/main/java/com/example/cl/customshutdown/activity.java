package com.example.cl.customshutdown;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.sun.mail.util.MailSSLSocketFactory;

import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * Created by CL on 7/13/17.
 */

public class activity extends AppCompatActivity {
    @BindView(R.id.et_password)
    EditText pwPassword;
    @BindView(R.id.tv_message)
    TextView tvMessage;
    @BindView(R.id.btn_resent)
    Button btnResent;
    @BindView(R.id.switch_launt_start)
    Switch switchLauntStart;
    @BindView(R.id.button_shut_down)
    Button buttonShutDown;
    private String randomPassWord = "";
    private String[] superPassword = {"22316", "19354", "14126"};
    private Runnable runnable;
    private Handler handler = new Handler();
    private boolean validTimeUp = true;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd " + "hh:mm:ss");
    private long validTime = 10;//min
    private int resentButtonCD = 3;//min
    //    private TimeTickReceiver mTickReceiver;
    private boolean regeristed = false;
    private AlertDialog.Builder alertDialogBuilder;
    private AlertDialog alertDialog;
    private boolean stopShutDown = false;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TimeService.activityhide = false;
        TimeService.canPass = false;
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);
        sp = getSharedPreferences("operationLog", MODE_MULTI_PROCESS);
        editor = sp.edit();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                creatPWIfPossible();
//                if (validTimeUp) {
//                    sendMailToQQ(randomPassWord);
//                }
//            }
//        }).start();


        creatPWIfPossible();
        if (validTimeUp) {
            sendMailToQQ(randomPassWord);
        }

        pwPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Boolean rightPW = false;
                TimeService.canPass = false;
                String inputText = s.toString();
                if (inputText.length() == 5) {
                    pwPassword.setCursorVisible(false);
                    pwPassword.setFocusableInTouchMode(false);
                    pwPassword.clearFocus();
                    if (inputText.equals(randomPassWord)) {
                        rightPW = true;
                        if (stopShutDown) {
                            pass();
                        } else {
                            alertDialog = alertDialogBuilder.show();
                        }
                    }
                    for (String pd : superPassword) {
                        if (inputText.equals(pd)) {
                            rightPW = true;
                            pass();
                        }
                    }
                    if (!rightPW) {
                        tvMessage.setText("密码错误");
                        pwPassword.setText("");
                        pwPassword.setCursorVisible(true);
                        pwPassword.setFocusableInTouchMode(true);
                        pwPassword.requestFocus();
                    }

                }


            }
        });

        runnable = new Runnable() {
            @Override
            public void run() {
                btnResent.setVisibility(View.VISIBLE);
            }
        };

        if (!isServiceRunning(this, "com.example.cl.customshutdown.TimeService")) {
            startService(new Intent(this, TimeService.class));
            Log.e("isServiceRunning", "false");
        } else {
            Log.e("isServiceRunning", "true");
        }

        if (sp.getBoolean("launch_when_device_start", true)) {
            switchLauntStart.setChecked(true);
        } else {
            switchLauntStart.setChecked(false);
        }


        alertDialogBuilder = configDialog();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                // Do stuff here
                Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD, 0);

            } else {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } else {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD, 0);
        }

        pwPassword.requestFocus();
    }





    @NonNull
    private AlertDialog.Builder configDialog() {
        LayoutInflater inflater = getLayoutInflater();
        final View dialogCustomLayout = inflater.inflate(R.layout.dialog_time, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("请设定观看时间");
        alertDialogBuilder.setView(dialogCustomLayout);
        final EditText et_number = (EditText) dialogCustomLayout.findViewById(R.id.edv_num);
        alertDialogBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!TextUtils.isEmpty(et_number.getText().toString())) {
                    autoCloseAfterTimeUp(Integer.valueOf(et_number.getText().toString()));
                } else {
                    alertDialog.dismiss();
                }
                pass();
            }
        });
        alertDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
                pass();
            }
        });
        return alertDialogBuilder;
    }

    private void pass() {
        tvMessage.setText("解锁成功");
        TimeService.canPass = true;
        if (switchLauntStart.isChecked()) {
            editor.putBoolean("launch_when_device_start", true);
        } else {
            editor.putBoolean("launch_when_device_start", false);
        }
        editor.commit();
        if (stopShutDown) {
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent("com.example.cl.customshutdown.timeup");
            intent.putExtra("timeup", true);
            int requestCode = 0;

            /**
             * 绑定了闹钟的执行动作，比如发送广播、给出提示等；
             *      PendingIntent.getService(Context c, int i, Intent intentm int j) 通过启动服务来实现闹钟提示
             *      PendingIntent.getBroadcase(...) 通过启动广播来实现
             *      PendingIntent.getActivity(...) 通过启动Activity来实现
             */
            pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                    requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            alarmManager.cancel(pendingIntent);

        }

        finish();
    }

    private final void creatPWIfPossible() {
        String lastestPWCreat = sp.getString("LastestPWCreat", "");
        if (!lastestPWCreat.equals("")) {
            try {
                Date now = new Date();
                Date lastestDatePWCreat = sdf.parse(lastestPWCreat);
                long after = now.getTime() - lastestDatePWCreat.getTime();
                long day = after / (24 * 60 * 60 * 1000);
                long hour = (after / (60 * 60 * 1000) - day * 24);
                long min = ((after / (60 * 1000)) - day * 24 * 60 - hour * 60);
                Log.e("分钟差", String.valueOf(min));
                if (min >= validTime) {
                    validTimeUp = true;
                } else {
                    validTimeUp = false;
                }
            } catch (ParseException e) {
                e.printStackTrace();
                validTimeUp = true;
            }
        }

        if (validTimeUp) {
            randomPassWord = creatRandomPW(5);
        } else {
            randomPassWord = sp.getString("LastestPW", "");
        }
        if (TextUtils.isEmpty(randomPassWord)) {
            randomPassWord = creatRandomPW(5);
            sendMailToQQ(randomPassWord);
        }
    }

    private String creatRandomPW(int k) {
        String randomPW = "";
        for (int i = 0; i < k; i++) {
            Random r = new Random();
            randomPW += String.valueOf(r.nextInt(10));
        }
        editor.putString("LastestPWCreat", sdf.format(new Date()));
        editor.putString("LastestPW", randomPW);
        editor.commit();
        return randomPW;
    }

    private void sendMailToQQ(String pw) {
        //0.5，props和authenticator参数
        Properties props = new Properties();
        props.setProperty("mail.host", "smtp.qq.com");
        props.setProperty("mail.smtp.auth", "true");

        //QQ邮箱的SSL加密。
        MailSSLSocketFactory sf = null;
        try {
            sf = new MailSSLSocketFactory();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        sf.setTrustAllHosts(true);
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.socketFactory", sf);

        //authenticator参数，登录自己的邮箱帐号密码，
        Authenticator authenticator = new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                /**
                 * 注意，QQ邮箱的规则是如果不是由腾讯的网页或者客户端打开登录的话，在其他任何地方
                 *登录邮箱，密码必须使用授权码，授权码下面会讲解，vlyvawibbsribgee
                 *xxxxxxx:自己的QQ邮箱登录帐号，也就是qq号
                 *yyyyyyy:密码，使用授权码登录，而不能使用原始的QQ密码
                 */
                return new PasswordAuthentication("410880617", "uiqbylnfvtnubhgc");
            }
        };
        //1、连接
        /**
         * props
         *         连接配置信息，邮件服务器的地址，是否进行权限验证
         * authenticator
         *         权限验证，也就是帐号密码验证
         * 所以需要先配置这两个参数
         */
        Session session = Session.getDefaultInstance(props, authenticator);


        // 2.2  , to:收件人 ; cc:抄送 ; bcc :暗送.
        /**
         * 收件人是谁？
         *         第一个参数：
         *             RecipientType.TO    代表收件人
         *             RecipientType.CC    抄送
         *             RecipientType.BCC    暗送
         *         比如A要给B发邮件，但是A觉得有必要给要让C也看看其内容，就在给B发邮件时，
         *         将邮件内容抄送给C，那么C也能看到其内容了，但是B也能知道A给C抄送过该封邮件
         *         而如果是暗送(密送)给C的话，那么B就不知道A给C发送过该封邮件。
         *     第二个参数
         *         收件人的地址，或者是一个Address[]，用来装抄送或者暗送人的名单。或者用来群发。
         */

        try {
            //2、发送的内容对象Mesage
            final Message message = new MimeMessage(session);
            //2.1、发件人是谁
            message.setFrom(new InternetAddress("410880617@qq.com"));
            message.setRecipient(RecipientType.TO, new InternetAddress("2376606275@qq.com"));
            // 2.3 主题（标题）
            message.setSubject("解锁密码_CarlosYang");
            // 2.4 正文
            String str = "解锁密码为：" + pw + " (" + validTime + "分钟有效)";
            message.setContent(str, "text/html;charset=UTF-8");


            final Message message2 = new MimeMessage(session);
            //2.1、发件人是谁
            message2.setFrom(new InternetAddress("410880617@qq.com"));
            message2.setRecipient(RecipientType.TO, new InternetAddress("272218160@qq.com"));
            // 2.3 主题（标题）
            message2.setSubject("解锁密码_CarlosYang");
            // 2.4 正文

            message2.setContent(str, "text/html;charset=UTF-8");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Transport.send(message);
//                        Transport.send(message2);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
                return true;
            case KeyEvent.KEYCODE_MENU:
                return true;
            case KeyEvent.KEYCODE_BACK:
                return true;
        }
        return super.onKeyDown(keyCode, event);

    }

    @OnClick({R.id.tv_message, R.id.btn_resent, R.id.button_shut_down})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_message:
                break;
            case R.id.btn_resent:
                creatPWIfPossible();
                sendMailToQQ(randomPassWord);
                tvMessage.setText("邮件已发送");
                btnResent.setVisibility(View.GONE);
                handler.postDelayed(runnable, resentButtonCD * 60 * 1000);
                break;
            case R.id.button_shut_down:
                if (!stopShutDown) {
                    stopShutDown = true;
                    buttonShutDown.setText("自动关机已取消");
                } else {
                    buttonShutDown.setText("关机任务有效");
                    stopShutDown = false;
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        TimeService.activityhide = true;
    }

    public void autoCloseAfterTimeUp(int minute) {
        stopShutDown = false;
//        IntentFilter mFilter = new IntentFilter();
//        mFilter.addAction("com.example.cl.customshutdown.timeup"); //每分钟变化的action
////        mFilter.addAction(Intent.ACTION_TIME_CHANGED); //设置了系统时间的action
//        mTickReceiver = new TimeTickReceiver();
//        registerReceiver(mTickReceiver, mFilter);
//        regeristed = true;
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("com.example.cl.customshutdown.timeup");
        intent.putExtra("timeup", true);
        int requestCode = 0;

        /**
         * 绑定了闹钟的执行动作，比如发送广播、给出提示等；
         *      PendingIntent.getService(Context c, int i, Intent intentm int j) 通过启动服务来实现闹钟提示
         *      PendingIntent.getBroadcase(...) 通过启动广播来实现
         *      PendingIntent.getActivity(...) 通过启动Activity来实现
         */
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

//        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
//                requestCode, new Intent(this, ViewAnimationActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        // 5秒后发送广播
        long triggerAtTime = SystemClock.elapsedRealtime() + minute * 60 * 1000;
//        long triggerAtTime = SystemClock.elapsedRealtime() + 5*1000;

        /**
         * set(int type，long startTime，PendingIntent pi)；
         * 设置一次性闹钟
         *      第一个参数：闹钟类型
         *      第二个参数：闹钟执行的时间
         *      第三个参数：闹钟响应动作
         *
         *    5秒后发送广播
         */
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, triggerAtTime, pendingIntent);
        editor.putBoolean("shutDownTask", true);
        editor.commit();
    }



    /*
         * 判断服务是否启动,context上下文对象 ，className服务的name
         */
    public static boolean isServiceRunning(Context mContext, String className) {

        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(30);

        if (!(serviceList.size() > 0)) {
            return false;
        }

        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().contains(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }
}
