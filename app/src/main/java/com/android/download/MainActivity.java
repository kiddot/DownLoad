package com.android.download;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * Created by kiddo on 17-1-3.
 */

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

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
    public String fileName = "";
    //请求的文件下载地址(本地文件)
    public String path = "" ;

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
            public void run(){
                try {
                    URL url = new URL(path);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(8000);
                    if (connection.getResponseCode() == 200){
                        int length = connection.getContentLength();
                        File file = new File(Environment.getExternalStorageDirectory() , fileName);
                        RandomAccessFile randomAccessFile = new RandomAccessFile(file , "rwd");
                        randomAccessFile.setLength(length);
                        mProgress.setMax(length);//设置进度条的最大进度为文件的长度
                        randomAccessFile.close();
                        int size = length/THREAD_COUNT;
                        for (int i=0;i < THREAD_COUNT ; i++){
                            int startIndex = i * size;
                            int endIndex = (i + 1) * size -1;
                            if (i == THREAD_COUNT - 1){
                                endIndex = length -1 ;
                            }
                            Log.d(TAG , "第"+(i+1)+ "个线程下载区间为：" + startIndex + "--" + endIndex);
                            new DownLoadThread(startIndex , endIndex , path , i);
                        }
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    class DownLoadThread extends Thread{
        private int lastProgress ;
        private int startIndex , endIndex , threadId ;
        private String path ;

        public DownLoadThread(int startIndex , int endIndex , String path , int threadId){
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.path = path ;
            this.threadId = threadId ;
        }

        public void run(){
            try{
                //建立进度临时文件，其实这时还没有创建。当往文件里写东西的时候才创建。
                File progressFile = new File(Environment.getExternalStorageDirectory(), threadId+".txt");
                //判断临时文件是否存在，存在表示已下载过，没下完而已
                if (progressFile.exists()){
                    FileInputStream fis = new FileInputStream(progressFile);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis));
                    //从进度临时文件中读取出上一次下载的总进度，然后与原本的开始位置相加，得到新的开始位置
                    lastProgress = Integer.parseInt(bufferedReader.readLine());
                    startIndex += lastProgress;

                    //断点续传，更新上次下载的进度条
                    mCurrentProgress += lastProgress;
                    mProgress.setProgress(mCurrentProgress);
                    Message message = Message.obtain();
                    message.what = 1;
                    mHandler.sendMessage(message);

                    bufferedReader.close();
                    fis.close();
                }
                //真正请求数据
                URL url = new URL(path);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(8000);

                //设置本次http请求所请求的数据的区间(这是需要服务器那边支持断点)，格式需要这样写，不能写错
                connection.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);
                //请求部分数据，响应码是206(注意响应码是206)
                if (connection.getResponseCode() == 206) {
                    //此时流中只有1/3原数据
                    InputStream is = connection.getInputStream();
                    File file = new File(Environment.getExternalStorageDirectory(),fileName);
                    RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                    //把文件的写入位置移动至startIndex
                    raf.seek(startIndex);

                    byte[] b = new byte[1024];
                    int len = 0;
                    int total = lastProgress;
                    while ((len = is.read(b)) != -1) {
                        raf.write(b, 0, len);
                        total += len;
                        Log.d(TAG,"线程" + threadId + "下载了" + total);
                        //生成一个专门用来记录下载进度的临时文件
                        RandomAccessFile progressRaf = new RandomAccessFile(progressFile, "rwd");
                        //每次读取流里数据之后，同步把当前线程下载的总进度写入进度临时文件中
                        progressRaf.write((total + "").getBytes());
                        progressRaf.close();

                        //下载时更新进度条
                        mCurrentProgress += len;
                        mProgress.setProgress(mCurrentProgress);
                        Message msg = Message.obtain();
                        msg.what = 0x1;
                        mHandler.sendMessage(msg);
                    }
                    System.out.println("线程" + threadId + "下载完成");
                    raf.close();

                    //每完成一个线程就+1
                    mFinishedThread ++;
                    //等标志位等于线程数的时候就说明线程全部完成了
                    if (mFinishedThread == THREAD_COUNT) {
                        for (int i = 0; i < mFinishedThread; i++) {
                            //将生成的进度临时文件删除
                            File f = new File(Environment.getExternalStorageDirectory(),i + ".txt");
                            f.delete();
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
