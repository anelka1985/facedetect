/*License Agreement
 For Open Source Computer Vision Library
 (3-clause BSD License)


 Copyright (C) 2000-2018, Intel Corporation, all rights reserved.
 Copyright (C) 2009-2011, Willow Garage Inc., all rights reserved.
 Copyright (C) 2009-2016, NVIDIA Corporation, all rights reserved.
 Copyright (C) 2010-2013, Advanced Micro Devices, Inc., all rights reserved.
 Copyright (C) 2015-2016, OpenCV Foundation, all rights reserved.
 Copyright (C) 2015-2016, Itseez Inc., all rights reserved.
 Third party copyrights are property of their respective owners.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
•Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
•Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
•Neither the names of the copyright holders nor the names of the contributors may be used to endorse or promote products derived from this software without specific prior written permission.

This software is provided by the copyright holders and contributors "as is" and any express or implied warranties, including, but not limited to, the implied warranties of merchantability and fitness for
a particular purpose are disclaimed. In no event shall copyright holders or contributors be liable for any direct, indirect, incidental, special, exemplary, or consequential damages (including, but not limited to,
procurement of substitute goods or services; loss of use, data, or profits; or business interruption) however caused and on any theory of liability, whether in contract, strict liability, or tort (including negligence or otherwise)
arising in any way out of the use of this software, even if advised of the possibility of such damage
*/
package com.openailab.sdkdemo;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.VoiceInteractor;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.openailab.facelibrary.FaceAPP;
import com.openailab.facelibrary.FaceAttribute;
import com.openailab.facelibrary.FaceInfo;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

public class MainActivity extends Activity {

    // Used to load the 'native-lib' library on application startup.
    private static final String TAG = "Opencv::Activity";
    private Mat mRgb;
    private Mat mRgbaFrame;

    private boolean canCloseCam=false;
    private boolean isLiveness = false;
    private int liveRegisterStatus = -1;
    private int mWidth;
    private int mHeight;
    public final int camWidth = 640;
    public final int camHeight = 480;
    Thread mainLoop = null;
    private Lock lock = null;
    private Lock  lockcam=null;
    private Lock lockth = null;
    //  private CameraGLSurfaceView mOpenCvCameraView;
    private SurfaceView surfaceView;
    private VideoUtil videoUtil;
    private myDrawRectView drawRectView;
    private TextView tv_time;
    private final PermissionsDelegate permissionsDelegate = new PermissionsDelegate(this);
    private boolean hasCameraPermission;
    private boolean hasExtSDPermission;
    private boolean hasaudioPermission;
    private boolean drawMat=false;
    private int mState= mixController.STATE_IDLE;
    private static  int loop = 0;

    private byte[] posStr;

    private final static int SHOWTOAST=4;
    private final static int MAX_REGISTER= FileOperator.MAX_REGISTER; // set max number of faces ,could be modify
    private final static String STORE_PATH= "/sdcard/openailab/models/test.txt";
    private FaceAPP face= FaceAPP.GetInstance();//FaceAPP.getInstance();



    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private int stattID;
    private boolean noExit=true;
    private final MyHandler mHandler = new MyHandler(this);

    public float[] feature=new float[128];

    private static final String PROC_CPU_INFO_PATH = "/proc/cpuinfo";

    private final static long MIN_CLICK_DELAY_TIME = 1000;
    private long lastClickTime = 0;

    private RelativeLayout settingLayout;
    private Button settingSaveBtn, settingCancel;
    private RadioGroup radioGroupFlip;
    private Map<String, String> settingMap;
    private int flipInt=0;//后置摄像头画面翻转，0为不需要翻转，1为翻转
    private File settingFile;
    private String editTextString, showName;
    private List<FaceInfo> FaceInfos;


    private final static int FACENUM = 30;//1为单框显示，3为3框显示
    private final static int LIVENESS = 0;//0为非活体检测，1为活体检测
    //public byte[] tmpPos = new byte[1024*FACENUM];
    String[] faceparams={"a","b","c","d", "factor","min_size","clarity","perfoptimize","livenessdetect","gray2colorScale","frame_num","quality_thresh","mode","facenum"};
    double[] VALUE = {0.75, 0.8, 0.9, 0.6, 0.65, 40, 200, 0, LIVENESS, 0.5, 1, 0.8, 1, FACENUM};


    /**
     * 声明一个静态的Handler内部类，并持有外部类的弱引用
     */
    private class MyHandler extends Handler {

        private final WeakReference<MainActivity> mActivty;

        private MyHandler(MainActivity mActivty) {
            this.mActivty = new WeakReference<MainActivity>(mActivty);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

                case SHOWTOAST:{
                    if(msg.arg1==1){
                        //Toast.makeText(mActivty.get(), "注册成功", Toast.LENGTH_LONG).show();

                        tv_time.setText("注册成功");
                    }
                    else if(msg.arg1==0){
                        liveRegisterStatus = 2;
                        tv_time.setText("你是:" + showName);
                        Log.d("zheng", "toast:" + showName);
                        showName = "点击人脸注册";

                    }else if(msg.arg1==2){

                        tv_time.setText("已经注册超过"+MAX_REGISTER+"人");

                    } else if(msg.arg1==3){
                        if (isLiveness) {
                            liveRegisterStatus = 1;
                        }
                        tv_time.setText("点击人脸注册");

                    }else if (msg.arg1 == 5) {

                        tv_time.setText("注册请输入名称");

                    }
                }
                break;
                }

            }
        }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override

        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //    surfaceView.setVisibility(View.VISIBLE);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    protected void hideBottomUIMenu() {
        int flags;
        int curApiVersion = Build.VERSION.SDK_INT;
        // This work only for android 4.4+
        if(curApiVersion >= Build.VERSION_CODES.KITKAT){
            // This work only for android 4.4+
            // hide navigation bar permanently in android activity
            // touch the screen, the navigation bar will not show
            flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }else{
            // touch the screen, the navigation bar will show
            flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }

        // must be executed in main thread :)
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }
  boolean drawRect(List<com.openailab.facelibrary.FaceInfo> faceInfos,Mat mat){
      //  Log.d("zheng","drawRectView????");
        for (int icount = 0; icount < faceInfos.size(); icount++) {
            FaceInfo info = faceInfos.get(icount);
            drawRectView.updateRect(info.mRect.left * drawRectView.getWidth() / camWidth, info.mRect.top * drawRectView.getHeight() / camHeight, info.mRect.right * drawRectView.getWidth() / camWidth, info.mRect.bottom * drawRectView.getHeight() / camHeight,icount);
        }
         Log.d("zheng","faceInfos.size():"+faceInfos.size());
        for (int icount = faceInfos.size(); icount < FACENUM; icount++) {
            drawRectView.updateRect(0, 0, 0, 0, icount);
        }
        return true;
  }
        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //copyFilesFassets(this,"openailab","/mnt/sdcard/openailab");
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        getWindow().setAttributes(params);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            surfaceView = (SurfaceView) findViewById(R.id.java_camera_view);
            drawRectView = (myDrawRectView) findViewById(R.id.mipi_preview_content);
            tv_time = (TextView) findViewById(R.id.tv_time);
            drawRectView.getHolder().setFormat(PixelFormat.TRANSPARENT);
            permissionsDelegate.checkRequiredPermission();

            hasCameraPermission = permissionsDelegate.hasCameraPermission();
            while (!hasCameraPermission) {
                permissionsDelegate.requestCameraPermission();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                hasCameraPermission = permissionsDelegate.hasCameraPermission();
            }

            hasExtSDPermission = permissionsDelegate.hasExtSDPermission();
            while (!hasExtSDPermission) {
                permissionsDelegate.requestExtSDPermission();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                hasExtSDPermission = permissionsDelegate.hasExtSDPermission();
            }

            surfaceView.setVisibility(SurfaceView.VISIBLE);
            drawRectView.setVisibility(SurfaceView.VISIBLE);
            drawRectView.setZOrderOnTop(true);
            posStr = new byte[1024];


            FaceInfos = new ArrayList<>();
            //  FaceInfos.add(new FaceInfo());

            copyFilesFassets(this, "openailab", "/sdcard/openailab");
            sharedPref = getSharedPreferences(getString(R.string.pref_start_id),Context.MODE_PRIVATE);
            editor= sharedPref.edit();
            stattID=sharedPref.getInt(getString(R.string.pref_start_id), 0);
            face.SetParameter(faceparams, VALUE);


            setSettingView();

      //  mOpenCvCameraView.setCvCameraViewListener(this);
            DisplayMetrics mDisplayMetrics = new DisplayMetrics();//屏幕分辨率容器
            getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
            mWidth = mDisplayMetrics.widthPixels;
            mHeight = mDisplayMetrics.heightPixels;
            videoUtil = new VideoUtil(surfaceView.getHolder(), 720, 640,CAMERA_FACING_BACK ,this);
            mRgb = new Mat(480, 640, CvType.CV_8UC1);
            mRgbaFrame = new Mat(480, 640, CvType.CV_8UC1);
            surfaceView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                    lastClickTime = currentTime;
                    drawMat = false;
                    //switch to sleep mode
                    mixController.setState(mixController.STATE_FACE_RECOGNIZE, mixController.STATE_FACE_REGISTER);
                    // Toast.makeText(MainActivity.this,"注册成功",Toast.LENGTH_LONG).show();
                    final View pwdEntryView = MainActivity.this.getLayoutInflater().inflate(
                            R.layout.dialog_exit_pwd, null);

                    //图片导入
                    Button file_browser = (Button) pwdEntryView.findViewById(R.id.file_browser);
                    file_browser.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent openFileBrowser = new Intent(MainActivity.this, GetSDTreeActivity.class);
                            videoUtil.stopPreview();
                            startActivity(openFileBrowser);
                            //    MainActivity.this.finish();
                        }
                    });

                    final EditText register_edittext = (EditText) pwdEntryView.findViewById(R.id.register_edittext);
                    register_edittext.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});//10 char

                    TextView sdk_version_text = (TextView) pwdEntryView.findViewById(R.id.sdk_version_text);
                    sdk_version_text.setText("SDK Version: " + face.GetVersion() + "    " + "Lib Version: " + face.GetFacelibVersion());


                    new AlertDialog.Builder(MainActivity.this).setTitle("确认注册吗？")
                            .setIcon(android.R.drawable.ic_input_add)
                            .setView(pwdEntryView)
                            .setCancelable(false)
                            .setPositiveButton("点此采集图片注册", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // 点击“确认”后的采集图片，用于注册
                                    drawMat = false;// 默认关闭true;
                                    editTextString = register_edittext.getText().toString();
                                    editTextString = editTextString.replaceAll(" ", "");
                                    editTextString = editTextString.replaceAll("\r", "");
                                    editTextString = editTextString.replaceAll("\n", "");


                                    if (editTextString == null || "".equals(editTextString)|| liveRegisterStatus == 1) {
                                        Message tempMsg = mHandler.obtainMessage();
                                        tempMsg.arg1 = 5;
                                        tempMsg.what = SHOWTOAST;
                                        mHandler.sendMessage(tempMsg);

                                        drawMat = false;//true;
                                        mixController.setState(mixController.STATE_IDLE, mixController.STATE_FACE_RECOGNIZE);
                                        return;
                                    }
                                    mixController.setState(mixController.STATE_FACE_REGISTER, mixController.STATE_FACE_RECOGNIZE);
                                    //    FileOperator.namelist[FileOperator.getfIndex()] = editTextString;
                                    Log.d("morrisdebug", "name is " + register_edittext.getText().toString());

                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    drawMat = false;//true;
                                    mixController.setState(mixController.STATE_IDLE, mixController.STATE_FACE_RECOGNIZE);
                                    // 点击“返回”后的操作,这里不设置没有任何操作
                                    //Toast.makeText(MainActivity.this, "你点击了返回键", Toast.LENGTH_LONG).show();
                                }
                            }).show();

                }
            }
        });
        lock = new ReentrantLock();

        lockcam = new ReentrantLock();
        lockth = new ReentrantLock();

        face.OpenDB();
        mainLoop = new Thread() {
            public void run() {
                int j;
                int[] i=new int[1];
                float[] high = new float[1];
                int recogTimes = 0;
                long now;
                long pas = 0;

//                  String[] params={"perfoptimize","mode"};
                // boolean fileExist= FileOperator.setStorePath(STORE_PATH);

                while (true) {
                    now = System.currentTimeMillis();
                    float fps = 1000/((float)(now - pas));
                    pas = System.currentTimeMillis();

                    if(!noExit)
                    {
                        canCloseCam=true;
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("cpdebugquit", "sleeping");
                        continue;
                    }
                    if (!videoUtil.isSyncFlag()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("cpdebugquit", "syncFlag");
                        continue;
                    }
                    lock.lock();
                    try{

                        Imgproc.cvtColor(videoUtil.getmRgba(), mRgbaFrame, Imgproc.COLOR_YUV2RGBA_NV12, 4);

                    }catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }finally {
                        lock.unlock();
                    }

                    FaceAPP.Image image= FaceAPP.GetInstance().new Image();
                    if (flipInt==1) {
                        Core.flip(mRgbaFrame, mRgbaFrame, 1);// Core.transpose(mRgbaFrame,mRgbaFrame);   //
                    }

                    String nameStr;
                    int Result;
                    switch (mixController.curState){
                        case mixController.STATE_FACE_RECOGNIZE:
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            break;
                        case mixController.STATE_FACE_REGISTER:
                            Log.d("cpdebugregister","register");

                            if (editTextString == null || "".equals(editTextString)) {
                                break;
                            }
                            image.matAddrframe=mRgbaFrame.getNativeObjAddr();
                            j=face.GetFeature(image,feature,i);

                            if(j==face.SUCCESS)
                            {
                                Log.d("zheng", "register editTextString:" + editTextString);
                                int res = face.AddDB(feature, editTextString);
                                Log.d("zheng", "register res:" + res);

                                Message tempMsg = mHandler.obtainMessage();
                                if (res == FaceAPP.SUCCESS) {
                                    tempMsg.arg1 = 1;
                                } else {
                                    tempMsg.arg1 = 2;
                                }
                                tempMsg.what = SHOWTOAST;
                                mHandler.sendMessage(tempMsg);

                                mixController.setState(mixController.STATE_IDLE, mixController.STATE_FACE_REGISTER);

                            }else if(j==face.ERROR_FAILURE){
                                mixController.setState(mixController.STATE_IDLE, mixController.STATE_FACE_REGISTER);
                            }
                            editTextString = "";

                            break;


                        case mixController.STATE_IDLE:

                            if(mRgbaFrame.empty()||mRgbaFrame.channels()!=4){
                                break;
                            }
                            image.matAddrframe=mRgbaFrame.getNativeObjAddr();

                            j = face.Detect(image,FaceInfos,i);
                            if(j == face.SUCCESS) {
                                drawRect(FaceInfos, mRgbaFrame);
                                drawRectView.updateDrawFlag(false, "未注册", fps, 0);

                                for (int icount = 0; icount < FaceInfos.size(); icount++) {
                                    String gender;
                                    String emotion;
                                    FaceAttribute faceAttr = new FaceAttribute();
                                    face.GetFaceAttr(image,FaceInfos.get(icount),faceAttr,i);

                                    if(FaceAttribute.GENDER_MALE == faceAttr.mGender)
                                        gender = "男";
                                    else
                                        gender = "女";
                                    if(FaceAttribute.EMOTION_CALM == faceAttr.mEmotion)
                                        emotion = "平静";
                                    else
                                        emotion = "开心";

                                    drawRectView.updateDrawFlag(true, showName+" "+faceAttr.mAge+" "+gender+" "+emotion, fps, icount);
                                }

                            }

//                            if(j==face.SUCCESS){
//                                //FaceAttribute faceAttr = new FaceAttribute();
//                                //face.GetFaceAttr(image,FaceInfos.get(0),faceAttr,i);
//                                drawRect(FaceInfos, mRgbaFrame);
//                                /*
//                                String gender;
//                                String emotion;
//                                if(FaceAttribute.GENDER_MALE == faceAttr.mGender)
//                                    gender = "男";
//                                else
//                                    gender = "女";
//                                if(FaceAttribute.EMOTION_CALM == faceAttr.mEmotion)
//                                    emotion = "calm";
//                                else
//                                    emotion = "开心";
//                                    */
//                                Message tempMsg = mHandler.obtainMessage();
//                                tempMsg.what = SHOWTOAST;
//                                for (int icount = 0; icount < FaceInfos.size(); icount++) {
//                                    face.GetFeature(image,FaceInfos.get(icount),feature,i);
//                                    float[] score = {0};
//
//                                    showName = face.QueryDB(feature, score);
//
//                                    Log.i("zheng", "name " + showName + " score " + score[0]);
//
//                                    if (score[0] > VALUE[3]) {
//                                        //tempMsg.arg1 = 0;
//                                        //drawRectView.updateDrawFlag(true, showName+" "+faceAttr.mAge+" "+gender+" "+emotion, fps);
//                                        drawRectView.updateDrawFlag(true, showName, fps, icount);
//                                    } else {
//                                        //drawRectView.updateDrawFlag(true, "未注册"+" "+faceAttr.mAge+" "+gender+" "+emotion, fps);
//                                        drawRectView.updateDrawFlag(true, "未注册", fps, icount);
//                                        //tempMsg.arg1 = 3;
//                                    }
//                                }
//
//                                 tempMsg.what = SHOWTOAST;
//                                mHandler.sendMessage(tempMsg);
//
//                                recogTimes++;
//
//                            }else if(j==face.ERROR_FAILURE){
//                                Log.d("morrisdebug","cant recoginze you");
//                             //   Log.d("zheng", "!!!!!!!!!!!!!!!!");
//                                if(i[0]==face.ERROR_NOT_EXIST){
//                                    drawRect(FaceInfos,mRgbaFrame);
//                                }else if(i[0]==face.ERROR_INVALID_PARAM){
//                                    for (int icount = 0; icount < FACENUM; icount++) {
//                                        drawRectView.updateRect(0, 0, 0, 0, icount);
//                                        drawRectView.updateDrawFlag(false, null, fps, icount);
//                                    }
//                                }
//                                Message tempMsg = mHandler.obtainMessage();
//                                tempMsg.arg1 = 3;
//                                tempMsg.what = SHOWTOAST;
//                                mHandler.sendMessage(tempMsg);
//
//                            }

                            break;
                        default:
                            try {
                                currentThread().sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                    FaceInfos.clear();
                    mRgbaFrame.release();
                    mRgb.release();

                }
            }
        };
        mainLoop.start();

    }
    @Override
    public void onStart() {
        super.onStart();
       
    }
    @Override
    public void onStop() {
        super.onStop();
        videoUtil.stopPreview();
    }
    @Override

    public void onPause()

    {  super.onPause();
        // mView.onPause();
        lockth.lock();
        try{
            canCloseCam=false;
            noExit=false;
            while(!canCloseCam){
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lockth.unlock();
        }

    }
    @Override

    public void onResume()
    {
        super.onResume();

        mixController.getInstance(this);
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        hideBottomUIMenu();
        lockth.lock();
        try{
            noExit=true;
        }finally {
            lockth.unlock();
        }

    }
    @Override
    public void onDestroy() {

        super.onDestroy();
        Log.d("morrisdebug", "destroy you ");
        mixController.setState(mixController.STATE_FACE_RECOGNIZE, mixController.STATE_FACE_REGISTER);
        noExit=false;
        face.CloseDB();
        face.Destroy();
        face=null;
        if (videoUtil != null)
            videoUtil.stopPreview();
        mainLoop = null;
        System.exit(0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsDelegate.resultGranted(requestCode, permissions, grantResults)) {

            surfaceView.setVisibility(SurfaceView.VISIBLE);
        }
    }

    public void copyFilesFassets(Context context, String oldPath, String newPath) {
        try {
            String fileNames[] = context.getAssets().list(oldPath);//获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {//如果是目录
                File file = new File(newPath);
                file.mkdirs();//如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFilesFassets(context,oldPath + "/" + fileName,newPath+"/"+fileName);
                }
            } else {//如果是文件
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount=0;
                while((byteCount=is.read(buffer))!=-1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                }
                fos.flush();//刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //如果捕捉到错误则通知UI线程

        }
    }


    private int isCPUInfoARMv7() {
        File cpuInfo = new File(PROC_CPU_INFO_PATH);
        if (cpuInfo != null && cpuInfo.exists()) {
            InputStream inputStream = null;
            BufferedReader bufferedReader = null;
            try {
                inputStream = new FileInputStream(cpuInfo);
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 512);
                String line = bufferedReader.readLine();
                //arch64
                Log.d("zheng", "line:" + line);
                if (line != null && line.length() > 0 && line.toLowerCase(Locale.US).contains("armv7")) {

                    return 1;
                } else {
                    if (line != null && line.length() > 0 && line.toLowerCase(Locale.US).contains("arch64"))
                        return 0;
                }
            } catch (Throwable t) {
                return -1;
            } finally {
                try {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }

    //设置
    private void setSettingView() {
        getSettngFileFromPath();
        Button flipBtn = (Button) this.findViewById(R.id.flipBtn);
        settingLayout = (RelativeLayout) this.findViewById(R.id.settingLayout);
        settingSaveBtn = (Button) this.findViewById(R.id.settingSaveBtn);
//        settingCancel = (Button) this.findViewById(R.id.settingCancel);
        radioGroupFlip = (RadioGroup) this.findViewById(R.id.radioGroupFlip);
        RadioButton flipSettingsDisable = (RadioButton) findViewById(R.id.flipSettingsDisable);
        RadioButton flipSettingsEnable = (RadioButton) findViewById(R.id.flipSettingsEnable);

        String flipStr = settingMap.get("flip");
        if ("1".equals(flipStr)) {
            flipSettingsEnable.setChecked(true);
            flipSettingsDisable.setChecked(false);
            flipInt = 1;
        } else {
            flipSettingsEnable.setChecked(false);
            flipSettingsDisable.setChecked(true);
            flipInt = 0;
        }


        flipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingLayout.setVisibility(View.VISIBLE);
            }
        });

        settingLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        settingSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileOperator.saveSettingIntoTxt(settingMap, settingFile);
                settingLayout.setVisibility(View.GONE);
            }
        });

        radioGroupFlip.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    public void onCheckedChanged(RadioGroup group, int checkedId) {

                        if (checkedId == R.id.flipSettingsEnable) {
                            flipInt = 1;
                            settingMap.put("flip", "1");
                        }
                        //else if(String.valueOf(radioButton.getText()).equalsIgnoreCase("启用"))
                        else if (checkedId == R.id.flipSettingsDisable) {
                            flipInt = 0;
                            settingMap.put("flip", "0");
                        }
                    }
                });


    }

    private boolean getSettngFileFromPath() {
        String path = "/sdcard/openailab/setting.conf";

        settingFile = new File(path);
        if (!settingFile.exists()) {
            settingMap = new HashMap<String, String>();
            try {
                settingFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        } else {
            settingMap = FileOperator.getSettingMapFromFile(settingFile);
            return true;
        }

    }




}
