package eatgreedi.mobile.com.egm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "LocationService";

    // use the websmithing defaultUploadWebsite for testing and then check your
    // location with your browser here: https://www.websmithing.com/gpstracker/displaymap.php
    private String defaultUploadWebsite;

    private boolean currentlyProcessingLocation = false;
    private LocationRequest mLocationRequest;

    private GoogleApiClient mLocationClient;

    private Socket mSocket;

    @Override
    public void onCreate() {
        super.onCreate();

        defaultUploadWebsite = getString(R.string.default_upload_website);

        buildGoogleApiClient();

        IO.Options opts = new IO.Options();
        opts.host = MainActivity.username;
        {
            try {
                mSocket = IO.socket(getString(R.string.default_upload_website));
            } catch (URISyntaxException e) {

            }
            mSocket.on(Socket.EVENT_CONNECT, onConnect);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mLocationClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // if we are currently trying to get a location and the alarm manager has called this again,
        // no need to start processing a new location.
        if (!currentlyProcessingLocation) {
            currentlyProcessingLocation = true;
            startTracking();
        }

        return START_NOT_STICKY;
    }

    private void startTracking() {
        Log.d(TAG, "startTracking");

        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
           // TODO recheck this
           // mLocationClient = new LocationClient(this,this,this);

            if (!mLocationClient.isConnected() || !mLocationClient.isConnecting()) {
                mLocationClient.connect();
            }
        } else {
            Log.e(TAG, "unable to connect to google play services.");
        }
    }

    protected void sendLocationDataToWebsite(Location location) {
        // formatted for mysql datetime format
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getDefault());
        Date date = new Date(location.getTime());

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.websmithing.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        float totalDistanceInMeters = sharedPreferences.getFloat("totalDistanceInMeters", 0f);

        boolean firstTimeGettingPosition = sharedPreferences.getBoolean("firstTimeGettingPosition", true);

        if (firstTimeGettingPosition) {
            editor.putBoolean("firstTimeGettingPosition", false);
        } else {
            Location previousLocation = new Location("");
            previousLocation.setLatitude(sharedPreferences.getFloat("previousLatitude", 0f));
            previousLocation.setLongitude(sharedPreferences.getFloat("previousLongitude", 0f));

            float distance = location.distanceTo(previousLocation);
            totalDistanceInMeters += distance;
            editor.putFloat("totalDistanceInMeters", totalDistanceInMeters);
        }

        editor.putFloat("previousLatitude", (float)location.getLatitude());
        editor.putFloat("previousLongitude", (float)location.getLongitude());
        editor.apply();


        Log.i(TAG,"sending location to server");
        mSocket.emit("updatelocation",Double.toString(location.getLatitude()));
        // TODO use the following requestParams to create a JSON payload
       /* final RequestParams requestParams = new RequestParams();
        requestParams.put("latitude", Double.toString(location.getLatitude()));
        requestParams.put("longitude", Double.toString(location.getLongitude()));

        Double speedInMilesPerHour = location.getSpeed()* 2.2369;
        requestParams.put("speed",  Integer.toString(speedInMilesPerHour.intValue()));

        try {
            requestParams.put("date", URLEncoder.encode(dateFormat.format(date), "UTF-8"));
        } catch (UnsupportedEncodingException e) {}

        requestParams.put("locationmethod", location.getProvider());

        if (totalDistanceInMeters > 0) {
            requestParams.put("distance", String.format("%.1f", totalDistanceInMeters / 1609)); // in miles,
        } else {
            requestParams.put("distance", "0.0"); // in miles
        }

        requestParams.put("username", sharedPreferences.getString("userName", ""));
        requestParams.put("phonenumber", sharedPreferences.getString("appID", "")); // uuid
        requestParams.put("sessionid", sharedPreferences.getString("sessionID", "")); // uuid

        Double accuracyInFeet = location.getAccuracy()* 3.28;
        requestParams.put("accuracy",  Integer.toString(accuracyInFeet.intValue()));

        Double altitudeInFeet = location.getAltitude() * 3.28;
        requestParams.put("extrainfo",  Integer.toString(altitudeInFeet.intValue()));

        requestParams.put("eventtype", "android");

        Float direction = location.getBearing();
        requestParams.put("direction",  Integer.toString(direction.intValue()));*/

        //final String uploadWebsite = sharedPreferences.getString("defaultUploadWebsite", defaultUploadWebsite);

        /* TODO create a JSON payload of username, locationdata
            Emit the Payload
         */

        /*LoopjHttpClient.get(uploadWebsite, requestParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, org.apache.http.Header[] headers, byte[] responseBody) {
                LoopjHttpClient.debugLoopJ(TAG, "sendLocationDataToWebsite - success", uploadWebsite, requestParams, responseBody, headers, statusCode, null);
                stopSelf();
            }
            @Override
            public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] errorResponse, Throwable e) {
                LoopjHttpClient.debugLoopJ(TAG, "sendLocationDataToWebsite - failure", uploadWebsite, requestParams, errorResponse, headers, statusCode, e);
                stopSelf();
            }
        });*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent("eatgreedi.mobile.com.egm.TryRestart"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.e(TAG, "position: " + location.getLatitude() + ", " + location.getLongitude() + " accuracy: " + location.getAccuracy());

            // we have our desired accuracy of 500 meters so lets quit this service,
            // onDestroy will be called and stop our location uodates
            if (location.getAccuracy() < 500.0f) {
                // TODO no need to stop location updates in our case
               // stopLocationUpdates();
                sendLocationDataToWebsite(location);
            }
        }
    }

    private void stopLocationUpdates() {
        if (mLocationClient != null && mLocationClient.isConnected()) {
            mLocationClient.disconnect();
            Log.i(TAG,"stopping location updates");
            // TODO Disconnect socket
        }
    }

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(1000); // milliseconds
        mLocationRequest.setFastestInterval(1000); // the fastest rate in milliseconds at which your app can handle location updates
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //mLocationClient.requestLocationUpdates(mLocationRequest, this);
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);

        // connect to socket for upload
        mSocket.connect();

    }

    /**
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */

    /*
     * TODO a good idea would be to try to restart/reconnect the service using alarm manager
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onDisconnected");

        stopLocationUpdates();
        stopSelf();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed");

        stopLocationUpdates();
        stopSelf();
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Connect to server in service", Toast.LENGTH_LONG).show();
                    mSocket.emit("setUserId", MainActivity.username);
                }
            });
        }
    };

    private Emitter.Listener notifyuser = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    /*
                    Intent notifyIntent = new Intent("eatgreedi.mobile.com.egm.NotifyUser");
                    notifyIntent.putExtra("message","some message");
                    sendBroadcast(notifyIntent);
                    */

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getApplicationContext())
                                    .setSmallIcon(R.drawable.ic_launcher)
                                    .setContentTitle("New Request")
                                    .setContentText("New delivery pick up request");

                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    Intent myIntent = new Intent(getApplicationContext(),MainActivity.class);
                    myIntent.putExtra("message","deliveryrequest");

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                    // Adds the back stack for the Intent (but not the Intent itself)
                    stackBuilder.addParentStack(MainActivity.class);
                    // Adds the Intent that starts the Activity to the top of the stack
                    stackBuilder.addNextIntent(myIntent);
                    PendingIntent pendingIntent =  stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    mBuilder.setContentIntent(pendingIntent);
                    notificationManager.notify(1515, mBuilder.build());
                }
            });
        }
    };


}