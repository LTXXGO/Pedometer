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
    private Button gravitySensorStartButton;
    private Button accelerometerStartButton;
    private Button stopRecordButton;
    private boolean recording = false;
    private static int stepSensorType = -1;
    private int nowStepCount;
    private Thread thread;
    private StepCount stepCount;
    private SensorManager pedometerSensorManager;
    private SensorManager directionSensorManager;
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
    private TextView startTimeTextView;
    private TextView finishTimeTextView;
    private TextView timeTextView;
    private TextView directionTextView;
    private TextView caloriesTextView;
    private Button recountButton;
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldSensorValues = new float[3];
    private float calories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.counts);
        timeTextView = findViewById(R.id.time);
        timeTextView.setText(0 + "天" + 0 + "小时" + 0 + "分钟" + 0 + "秒");
        directionTextView = findViewById(R.id.direction);
        caloriesTextView = findViewById(R.id.calories);
        startTimeTextView = findViewById(R.id.start_time);
        finishTimeTextView = findViewById(R.id.finish_time);
        gravitySensorStartButton = findViewById(R.id.bt_startservice);
        accelerometerStartButton = findViewById(R.id.bt_startbya);
        stopRecordButton = findViewById(R.id.bt_stop);
        pedometerSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        nowStepCount = Integer.parseInt(textView.getText().toString());
        // 未开始计步时禁用'停止计步'按钮
        stopRecordButton.setEnabled(false);
        // 设置按钮字体颜色
        stopRecordButton.setTextColor(Color.rgb(128, 128, 128));
        gravitySensorStartButton.setTextColor(Color.rgb(0, 133, 119));
        accelerometerStartButton.setTextColor(Color.rgb(0, 133, 119));
        recountButton = findViewById(R.id.bt_zero);
        recountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nowStepCount = 0;
                textView.setText("0");
                timeTextView.setText("暂无数据");
                startTimeTextView.setText("暂无数据");
                finishTimeTextView.setText("暂无数据");
            }
        });

        directionSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = directionSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magneticFieldSensor = directionSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        directionSensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        directionSensorManager.registerListener(listener, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);

        calculateOrientation();

        accelerometerStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUI();
                // 更新按钮显示
                accelerometerStartButton.setText("使用加速度传感器\n计步中");
                // 获取当前时间
                getCurrentTime();
                startTimeTextView.setText(startDay + "日" + startHour + "时" + startMin + "分" + startSec + "秒");
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
                updateUI();
                // 更新按钮显示
                gravitySensorStartButton.setText("使用重力传感器\n计步中");
                // 获取当前时间
                getCurrentTime();
                startTimeTextView.setText(startDay + "日" + startHour + "时" + startMin + "分" + startSec + "秒");
                //取得SDK版本
                final int versionCodes = Build.VERSION.SDK_INT;
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
                stopRecordButton.setTextColor(Color.rgb(128, 128, 128));
                // 启用开始计步按钮
                gravitySensorStartButton.setEnabled(true);
                gravitySensorStartButton.setText("使用重力传感器\n开始计步");
                gravitySensorStartButton.setTextColor(Color.rgb(0, 133, 119));
                accelerometerStartButton.setEnabled(true);
                accelerometerStartButton.setText("使用加速度传感器\n开始计步");
                accelerometerStartButton.setTextColor(Color.rgb(0, 133, 119));
                // 启用'重新计数'按钮
                recountButton.setEnabled(true);
                recountButton.setTextColor(Color.rgb(0, 0, 0));

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
                finishTimeTextView.setText(finishDay + "日" + finishHour + "时" + finishMin + "分" + finishSec + "秒");
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
                timeTextView.setText(timeDay + "天" + timeHour + "小时" + timeMin + "分钟" + timeSec + "秒");
                calculateCalories();
            }
        });
    }

    public void onPause(){
        directionSensorManager.unregisterListener(listener);
        super.onPause();
    }

    final SensorEventListener listener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldSensorValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
            calculateOrientation();
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldSensorValues);
        SensorManager.getOrientation(R, values);
        // 要经过一次数据格式的转换，转换为度
        values[0] = (float) Math.toDegrees(values[0]);
        Log.i("000", values[0] + "");
        if (values[0] >= -5 && values[0] < 5) {
            directionTextView.setText("正北");
        } else if (values[0] >= 5 && values[0] < 85) {
            directionTextView.setText("东北");
        } else if (values[0] >= 85 && values[0] <=95) {
            directionTextView.setText("正东");
        } else if (values[0] >= 95 && values[0] <175) {
            directionTextView.setText("东南");
        } else if ((values[0] >= 175 && values[0] <= 180) || (values[0]) >= -180 && values[0] < -175) {
            directionTextView.setText("正南");
        } else if (values[0] >= -175 && values[0] <-95) {
            directionTextView.setText("西南");
        } else if (values[0] >= -95 && values[0] < -85) {
            directionTextView.setText("正西");
        } else if (values[0] >= -85 && values[0] <-5) {
            directionTextView.setText("西北");
        } else {
            directionTextView.setText("暂无数据");
        }
    }

    private void calculateCalories() {
        calories = (float)nowStepCount / (float)20;
        caloriesTextView.setText(calories + " cal");
    }

    private void updateUI() {
        // 更新'记录'状态
        recording = true;
        // 启用'停止计步'按钮
        stopRecordButton.setEnabled(true);
        stopRecordButton.setTextColor(Color.rgb(255, 0, 0));
        // 禁用'开始计步'按钮
        gravitySensorStartButton.setEnabled(false);
        gravitySensorStartButton.setTextColor(Color.rgb(0, 133, 119));
        accelerometerStartButton.setEnabled(false);
        accelerometerStartButton.setTextColor(Color.rgb(128, 128, 128));
        // 禁用'重新计数'按钮
        recountButton.setEnabled(false);
        recountButton.setTextColor(Color.rgb(128, 128, 128));
        // 更新耗时文本
        timeTextView.setText("正在计算...");
        // 更新结束时间
        finishTimeTextView.setText("暂无数据");
        // 更新卡路里
        caloriesTextView.setText("正在计算...");
    }

    private void getCurrentTime() {
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
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void addCountStepListener() {
        Sensor countSensor = pedometerSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detectorSensor = pedometerSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (countSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_COUNTER;
            pedometerSensorManager.registerListener((SensorEventListener) this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.i("计步传感器类型", "Sensor.TYPE_STEP_COUNTER");
        } else if (detectorSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_DETECTOR;
            pedometerSensorManager.registerListener((SensorEventListener) this, detectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            addBasePedometerListener();
        }
    }

    public void addBasePedometerListener() {
        Log.i("BindService", "加速度传感器");
        stepCount = new StepCount();
        stepCount.setSteps(nowStepCount);
        //获取传感器类型 获得加速度传感器
        Sensor sensor = pedometerSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        boolean isAvailable = pedometerSensorManager.registerListener(stepCount.getStepDetector(), sensor, SensorManager.SENSOR_DELAY_UI);
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
                    //获取APP打开到现在的总步数 = 本次系统回调的总步数-APP打开之前已有的步数
                    int thisStepCount = tempStep - numberOfStepsRecorded;
                    //本次有效步数 =（APP打开后所记录的总步数-上一次APP打开后所记录的总步数）
                    int thisStep = thisStepCount - previousStepCount;
                    //总步数 = 现有的步数 + 本次有效步数
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
