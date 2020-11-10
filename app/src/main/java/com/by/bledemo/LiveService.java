package com.by.bledemo;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class LiveService extends Service {
    public LiveService() {
    }

    private final static String TAG = "PlayerMusicService";
    private MediaPlayer mMediaPlayer;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.novoice10);
        mMediaPlayer.setLooping(true);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                startPlayMusic();
            }
        }).start();
        return START_STICKY;//注释1
    }
    private void startPlayMusic(){
        if(mMediaPlayer != null){
            mMediaPlayer.start();
            Log.v(TAG, "开始无声播放音乐");
        }
    }
    private void stopPlayMusic(){
        if(mMediaPlayer != null){
            mMediaPlayer.stop();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "LiveService 销毁");
        stopPlayMusic();
        // 注释2
        Intent intent = new Intent(getApplicationContext(),LiveService.class);
        startService(intent);
    }
}
