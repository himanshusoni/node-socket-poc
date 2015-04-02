package eatgreedi.mobile.com.egm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RestartServiceReceiver extends BroadcastReceiver {
    public RestartServiceReceiver() {
    }

    private static final String TAG = "RestartServiceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive - service closed. trying to restart");
        context.startService(new Intent(context.getApplicationContext(), LocationService.class));
    }
}
