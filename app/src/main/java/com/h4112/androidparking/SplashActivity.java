package com.h4112.androidparking;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

public class SplashActivity extends AppCompatActivity {

    private Handler handler;
    private Runnable delayedOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Log.i("SplashActivity", "In splash screen!");

        handler = new Handler();
        delayedOpen = new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();

                Log.i("SplashActivity", "Out splash screen!");
            }
        };

        handler.postDelayed(delayedOpen, 1000);

        if(getSupportActionBar() != null) getSupportActionBar().hide();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacks(delayedOpen);
    }
}
