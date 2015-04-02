package eatgreedi.mobile.com.egm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {
    public NotificationReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(/* your notification */);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("New Request")
                        .setContentText("New delivery pick up request");
        /*Intent intent = new Intent(context, MainActivity.class);
        //PendingIntent pendingIntent = *//* your intent *//*;
        mBuilder.setContentIntent(resultPendingIntent);
        notificationManager.notify(1515, mBuilder);*/
    }
}
