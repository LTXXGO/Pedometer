package com.network.hqu.myapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView textView;
    private SensorEventListener sensorEventListener;
    private boolean isBind;
    private Button gravitySensorStartButton;
    private Button accelerometerStartButton;
    private Button stopRecordButton;
    private boolean recording = false;
    private static int stepSensorType = -1;
    private int nowStepCount;
    private Intent intent;
    private Thread thread;
    private StepCount stepCount;
    SensorManager sensorManager;
    private boolean recorded = false;
    private int numberOfStepsRecorded = 0;
    private int previousStepCount = 0;
    private String startDay;
    private String finishDay;
    private String startHour;
    private String finishHour;
    private String startMin;
    private String finishMin;
    private String startSec;
    private String finishSec;
    private Calendar calendar;
    private TextView mTextTime;
    private Button mBtnZero;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.counts);
        gravitySensorStartButton = findViewById(R.id.bt_startservice);
        accelerometerStartButton = findViewById(R.id.bt_startbya);
        stopRecordButton = findViewById(R.id.bt_stop);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        nowStepCount = Integer.parseInt(textView.getText().toString());
        // 未开始计步时禁用'停止计步'按钮
        stopRecordButton.setEnabled(false);
        // 设置按钮字体颜色
        stopRecordButton.setTextColor(Color.rgb(128, 128, 128));
        gravitySensorStartButton.setTextColor(Color.rgb(0, 133, 119));
        accelerometerStartButton.setTextColor(Color.rgb(0, 133, 119));
        mBtnZero = findViewById(R.id.bt_zero);
        mBtnZero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nowStepCount = 0;
                textView.setText("0");
            }
        });

        accelerometerStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 更新'记录'状态
                recording = true;
                // 更新按钮显示
                accelerometerStartButton.setText("使用加速度传感器\n计步中");
                // 启用'停止计步'按钮
                stopRecordButton.setEnabled(true);
                stopRecordButton.setTextColor(Color.rgb(255, 0, 0));
                // 禁用'开始计步'按钮
                gravitySensorStartButton.setEnabled(false);
                gravitySensorStartButton.setTextColor(Color.rgb(128, 128, 128));
                accelerometerStartButton.setEnabled(false);
                accelerometerStartButton.setTextColor(Color.rgb(0, 133, 119));

                calendar = Calendar.getInstance();
                calendar.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
                startDay = String.valueOf(calendar.get(Calendar.DATE));
                if (calendar.get(Calendar.AM_PM) == 0) {
                    startHour = String.valueOf(calendar.get(Calendar.HOUR));
                } else {
                    startHour = String.valueOf(calendar.get(Calendar.HOUR) + 12);
                }
                startMin = String.valueOf(calendar.get(Calendar.MINUTE));
                startSec = String.valueOf(calendar.get(Calendar.SECOND));
                Log.d("888", "startTime :" + startDay + "-" + startHour + ":" + startMin + ":" + startSec);
                thread = new Thread() {
                    @Override
                    public void run() {
                        addBasePedometerListener();
                    }
                };
                thread.start();
            }
        });

        gravitySensorStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 更新'记录'状态
                recording = true;
                // 更新按钮显示
                gravitySensorStartButton.setText("使用重力传感器\n计步中");
                // 启用'停止计步'按钮
                stopRecordButton.setEnabled(true);
                stopRecordButton.setTextColor(Color.rgb(255, 0, 0));
                // 禁用'开始计步'按钮
                gravitySensorStartButton.setEnabled(false);
                gravitySensorStartButton.setTextColor(Color.rgb(0, 133, 119));
                accelerometerStartButton.setEnabled(false);
                accelerometerStartButton.setTextColor(Color.rgb(128, 128, 128));

                calendar = Calendar.getInstance();
                calendar.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
                startDay = String.valueOf(calendar.get(Calendar.DATE));
                if (calendar.get(Calendar.AM_PM) == 0) {
                    startHour = String.valueOf(calendar.get(Calendar.HOUR));
                } else {
                    startHour = String.valueOf(calendar.get(Calendar.HOUR) + 12);
                }
                startMin = String.valueOf(calendar.get(Calendar.MINUTE));
                startSec = String.valueOf(calendar.get(Calendar.SECOND));
                Log.d("888", "startTime :" + startDay + "-" + startHour + ":" + startMin + ":" + startSec);
                final int versionCodes = Build.VERSION.SDK_INT;//取得SDK版本
                final Thread t = new Thread() {
                    @Override
                    public void run() {
                        if (versionCodes > 19) {
                            previousStepCount = 0;
                            addCountStepListener();
                        }
                    }
                };
                t.start();
            }
        });

        stopRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 更新'记录'状态
                recording = false;
                // 禁用'停止计步'按钮
                stopRecordButton.setEnabled(false);
                stopRecordButton.setTextColor(Color.rgb(255, 0, 0));
                // 启用开始计步按钮
                gravitySensorStartButton.setEnabled(true);
                gravitySensorStartButton.setText("使用重力传感器\n开始计步");
                gravitySensorStartButton.setTextColor(Color.rgb(0, 133, 119));
                accelerometerStartButton.setEnabled(true);
                accelerometerStartButton.setText("使用加速度传感器\n开始计步");
                accelerometerStartButton.setTextColor(Color.rgb(0, 133, 119));

                calendar = Calendar.getInstance();
                calendar.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
                finishDay = String.valueOf(calendar.get(Calendar.DATE));
                if (calendar.get(Calendar.AM_PM) == 0) {
                    finishHour = String.valueOf(calendar.get(Calendar.HOUR));
                } else {
                    finishHour = String.valueOf(calendar.get(Calendar.HOUR) + 12);
                }
                finishMin = String.valueOf(calendar.get(Calendar.MINUTE));
                finishSec = String.valueOf(calendar.get(Calendar.SECOND));
                //设置用时
                mTextTime = findViewById(R.id.tx_time);
                int fd = Integer.valueOf(finishDay);
                int sd = Integer.valueOf(startDay);
                int fh = Integer.valueOf(finishHour);
                int sh = Integer.valueOf(startHour);
                int fm = Integer.valueOf(finishMin);
                int sm = Integer.valueOf(startMin);
                int fs = Integer.valueOf(finishSec);
                int ss = Integer.valueOf(startSec);
                String timeDay = Integer.toString((fd - sd));
                String timeHour;
                String timeMin;
                String timeSec;
                if (fh >= sh) {
                    timeHour = Integer.toString((fh - sh));
                } else {
                    timeHour = Integer.toString((fh + 24 - sh));
                    timeDay = Integer.toString((fd - sd - 1));
                }
                if (fm >= sm) {
                    timeMin = Integer.toString((fm - sm));
                } else {
                    timeMin = Integer.toString((fm + 60 - sm));
                    timeHour = Integer.toString((Integer.valueOf(timeHour) - 1));
                }
                if (fs >= ss) {
                    timeSec = Integer.toString((fs - ss));
                    Log.d("888", "fs: " + fs);
                    Log.d("888", "ss: " + ss);
                    Log.d("888", "timesec: " + timeSec);
                } else {
                    timeSec = Integer.toString((fs + 60 - ss));
                    timeMin = Integer.toString((Integer.valueOf(timeMin) - 1));
                }
                Log.d("888", "finishTime :" + finishDay + "-" + finishHour + ":" + finishMin + ":" + finishSec);
                Log.d("888", "time :"+timeDay + "-" + timeHour + ":" + timeMin + ":" + timeSec);
                mTextTime.setText(timeDay + "天" + timeHour + "小时" + timeMin + "分钟" + timeSec + "秒");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void addCountStepListener() {
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (countSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_COUNTER;
            sensorManager.registerListener((SensorEventListener) this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.i("计步传感器类型", "Sensor.TYPE_STEP_COUNTER");
        } else if (detectorSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_DETECTOR;
            sensorManager.registerListener((SensorEventListener) this, detectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            addBasePedometerListener();
        }
    }

    public void addBasePedometerListener() {
        Log.i("BindService", "加速度传感器");
        stepCount = new StepCount();
        stepCount.setSteps(nowStepCount);
        //获取传感器类型 获得加速度传感器
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        boolean isAvailable = sensorManager.registerListener(stepCount.getStepDetector(), sensor, SensorManager.SENSOR_DELAY_UI);
        stepCount.initListener(new StepValuePassListener() {
            @Override
            public void stepChanged(int steps) {
                if (recording == true) {
                    nowStepCount = steps;//通过接口回调获得当前步数
                    String nowStep = Integer.toString(nowStepCount);
                    Log.d("888", "stepChanged: " + nowStepCount);
                    Log.d("888", "flag: " + recording);
                    textView.setText(nowStep);
                }
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (recording == true) {
            //这种类型的传感器返回步骤的数量由用户自上次重新启动时激活。返回的值是作为浮动(小数部分设置为0),
            // 只在系统重启复位为0。事件的时间戳将该事件的第一步的时候。这个传感器是在硬件中实现,预计低功率。
            if (stepSensorType == Sensor.TYPE_STEP_COUNTER) {
                //获取当前传感器返回的临时步数
                int tempStep = (int) event.values[0];
                //首次如果没有获取手机系统中已有的步数则获取一次系统中APP还未开始记步的步数
                if (!recorded) {
                    recorded = true;
                    numberOfStepsRecorded = tempStep;
                } else {
                    //获取APP打开到现在的总步数=本次系统回调的总步数-APP打开之前已有的步数
                    int thisStepCount = tempStep - numberOfStepsRecorded;
                    //本次有效步数=（APP打开后所记录的总步数-上一次APP打开后所记录的总步数）
                    int thisStep = thisStepCount - previousStepCount;
                    //总步数=现有的步数+本次有效步数
                    nowStepCount += (thisStep);
                    //记录最后一次APP打开到现在的总步数
                    previousStepCount = thisStepCount;
                }
            }
            // 这种类型的传感器触发一个事件每次采取的步骤是用户。只允许返回值是1.0,为每个步骤生成一个事件。
            // 像任何其他事件,时间戳表明当事件发生(这一步),这对应于脚撞到地面时,生成一个高加速度的变化。
            else if (stepSensorType == Sensor.TYPE_STEP_DETECTOR) {
                if (event.values[0] == 1.0) {
                    nowStepCount++;
                }
            }
            Log.d("888", "nowStepCount: " + nowStepCount);
            Log.d("888", "onSensorChanged: " + recording);
            textView.setText(Integer.toString(nowStepCount));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
