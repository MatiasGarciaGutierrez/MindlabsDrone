package com.example.CombineProject;
import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.secneo.sdk.Helper;

import dji.sdk.media.MediaManager;

public class MApplication extends Application {
    private DemoApplication demoApplication;
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (demoApplication == null) {
            demoApplication = new DemoApplication();
            demoApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        demoApplication.onCreate();
    }

}