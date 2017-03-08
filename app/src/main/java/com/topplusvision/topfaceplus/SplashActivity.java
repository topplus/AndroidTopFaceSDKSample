package com.topplusvision.topfaceplus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.topplusvision.topface.TopFace;

/**
 * time: 2017/1/19
 * description:
 *
 * @author fandong
 */
public class SplashActivity extends AppCompatActivity {

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Intent intent = new Intent(SplashActivity.this, SyncAcitivty.class);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_splash);
        //请在下面输入正确的clientId,clientSecret，否则无法使用FaceSDK
        TopFace.initLibrary(this, "", "");
        //进入预览界面
        launchMain();
//        launchAsync();
    }

    private void launchAsync() {
        mHandler.sendEmptyMessageDelayed(1, 2100);
    }

    private void launchMain() {
        mHandler.sendEmptyMessageDelayed(2, 2100);
    }

}
