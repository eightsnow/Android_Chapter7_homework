package com.bytedance.videoplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.Locale;

public class VideoActivity extends AppCompatActivity {
    private Button buttonPlay;
    private Button buttonPause;
    private MyVideoView videoView;
    private TextView timeView;
    private SeekBar seekBar;

    private boolean flag = false;
    private int currentPositionForPause = 0, durationForPause = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_video);
        setTitle("VideoPlayer");

        Intent intent = getIntent();
        String path;
        Uri uri = intent.getData();
        if (uri != null)
            path = getRealFilePath(this, uri);
        else
            path = intent.getStringExtra("Path");

        videoView = findViewById(R.id.videoView);
        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向
        if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {
            //横屏
            getSupportActionBar().hide();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            videoView.setOrientation(true);
        } else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
            //竖屏
            videoView.setOrientation(false);
        }
        videoView.setVideoPath(path);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //设置 MediaPlayer 的 OnSeekComplete 监听
                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        // seekTo 方法完成时的回调
                        if(flag){
                            videoView.start();
                        }
                    }
                });
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mediaPlayer){
                flag = false;
            }
        });

        buttonPause = findViewById(R.id.buttonPause);
        buttonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoView.pause();
                flag = false;
            }
        });

        buttonPlay = findViewById(R.id.buttonPlay);
        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!flag){
                    videoView.start();
                    flag = true;

                    int currentPosition = videoView.getCurrentPosition();
                    int duration = videoView.getDuration();
                    changeTimeView(currentPosition, duration);

                    update();
                }
            }
        });

        seekBar = findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    int position = (int)((float)progress/100*videoView.getDuration());
                    videoView.seekTo(position);
                    int currentPosition = videoView.getCurrentPosition();
                    int duration = videoView.getDuration();
                    changeTimeView(currentPosition, duration);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        timeView = findViewById(R.id.timeView);
        if (savedInstanceState != null) {
            flag = true;
            final int currentPosition = savedInstanceState.getInt("currentPosition");
            final int duration = savedInstanceState.getInt("duration");
            videoView.seekTo(currentPosition);
            changeTimeView(currentPosition, duration);
            seekBar.setProgress((int)((float)currentPositionForPause/durationForPause*100));

            update();
        }
        else{
            timeView.setText(new StringBuilder("00:00/00:00"));
        }

    }

    private void changeTimeView(int currentPosition, int duration)
    {
        String time = String.format(Locale.CHINA, "%02d", currentPosition / 1000 / 60) + ":" +
                String.format(Locale.CHINA, "%02d", currentPosition / 1000 % 60) + "/" +
                String.format(Locale.CHINA, "%02d", duration / 1000 / 60) + ":" +
                String.format(Locale.CHINA, "%02d", duration / 1000 % 60);
        timeView.setText(time);
    }

    //更新进度条和时间
    private void update(){
        if(flag){
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    int currentPosition = videoView.getCurrentPosition();
                    int duration = videoView.getDuration();

                    float interval = (float)currentPosition/duration - (float)seekBar.getProgress()/100;
                    if(interval >= 0.01 || interval <= -0.01)
                        seekBar.setProgress(seekBar.getProgress() + (int)(interval*100));

                    changeTimeView(currentPosition, duration);

                    update();
                }
            }, 1000);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("currentPosition", videoView.getCurrentPosition());
        outState.putInt("duration", videoView.getDuration());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause(){
        currentPositionForPause = videoView.getCurrentPosition();
        durationForPause = videoView.getDuration();
        super.onPause();
    }

    @Override
    protected void onRestart(){
        super.onRestart();

        timeView = findViewById(R.id.timeView);
        seekBar = findViewById(R.id.seekbar);
        videoView.seekTo(currentPositionForPause);
        changeTimeView(currentPositionForPause, durationForPause);
        seekBar.setProgress((int)((float)currentPositionForPause/durationForPause*100));

        update();
    }

    public static String getRealFilePath(final Context context, final Uri uri ) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

}

