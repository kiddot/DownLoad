package com.android.download;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 *
 * Created by kiddo on 17-1-3.
 */

public class MainActivity extends AppCompatActivity {

    public static final int THREAD_COUNT = 3 ;//

    //进度条
    private ProgressBar mProgress;
    //显示进度(百分比)
    private TextView mTvProgress;
    //记录当前进度条的下载进度
    private int mCurrentProgress;
    //下载完成的线程数量
    public int mFinishedThread = 0;
    //下载完成生成的文件名
    public String fileName = "test.mp3";
    //请求的文件下载地址(本地文件)
    public String path = "http://192.168.1.100:8089/" + fileName;

    private Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 0x1) {
                mTvProgress.setText(mProgress.getProgress()*100/mProgress.getMax() + "%");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    /**
     * 初始化组件
     */
    private void initView(){
        mTvProgress = (TextView) findViewById(R.id.tv);
        mProgress = (ProgressBar) findViewById(R.id.pb);
    }

    /**
    * 点击下载的事件
    * @param view
    */
    public void download(View view){
        new Thread(){

        }    }

}
