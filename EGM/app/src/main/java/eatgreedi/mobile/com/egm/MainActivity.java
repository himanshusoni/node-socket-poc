package eatgreedi.mobile.com.egm;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends ActionBarActivity {

    String userKey = "storedName";
    static String username;

    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // if not logged in, register
        // show dialog to register username
        if(!isLoggedIn()){
            promptUserLogIn();
        }

        IO.Options opts = new IO.Options();
        opts.host = getUserName();
        {
            try {
                mSocket = IO.socket(getString(R.string.default_upload_website),opts);
            } catch (URISyntaxException e) {

            }
        }

        String message = getIntent().getStringExtra("message");
        if(message!= null && message.equals("deliveryrequest"));
        {
           // showRequest();
        }
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("join response", onJoinMessage);
        mSocket.on("notifyuser", notifyuser);
        // connect to the socket
        mSocket.connect();

        Log.i("MAIN"," Setting title " + getUserName() + " : " + username);
        // Set username as actionbar title
        setTitle(getUserName());


        /*//Register out broadcast receiver
        IntentFilter intentFilter = new IntentFilter("eatgreedi.mobile.com.egm.TryRestart");
        registerReceiver( RestartServiceReceiver , intentFilter);*/

        // start the service
        Log.i("MAIN","attempting to start the service");
        startService(new Intent(this, LocationService.class));

    }

    private void showRequest() {

        View linearLayout =  findViewById(R.id.pager);
        //LinearLayout layout = (LinearLayout) findViewById(R.id.info);

        TextView tv = new TextView(this);
        SimpleDateFormat dt = new SimpleDateFormat("yyyyy-mm-dd hh:mm:ss");
        tv.setText("Deliver Request" + dt.format(new Date()));
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        ((LinearLayout) linearLayout).addView(tv);

    }

    private void promptUserLogIn() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // Set up the input
        final EditText userInput = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        userInput.setInputType(InputType.TYPE_CLASS_TEXT);
        alertDialogBuilder.setView(userInput);

        // set dialog message
        alertDialogBuilder
                .setTitle("Enter username")
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                String userName = userInput.getText().toString();
                                userName = userName.trim();

                                // validate
                                if (userName.length() != 0 && userName.split(" ").length == 1) {
                                    saveUser(userName);
                                } else
                                    userInput.setError("Try again. 1 word only");
                            }
                        });
                /*.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });*/

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void showToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }
    private void saveUser(String username) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(userKey, username);
        editor.commit();

        setUserName(username);
    }

    private String getUserName() {
        return username;
    }

    private void setUserName(String username) {
        this.username = username;
        Log.i("MAIN","Logged in as " + username);
    }

    private boolean isLoggedIn() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String name = sharedPreferences.getString(userKey, "notfound");
        if(name.equals("notfound")){
            Log.i("MAIN","username not found " + username);
         return false;
        }
        setUserName(name);
        Log.i("MAIN","username has already logged in " + username);
        return true;
    }

    /**
     * socket listener for join response from server
     */
    private Emitter.Listener onJoinMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    String message;
                    try {
                        //username = data.getString("username");
                        message = data.getString("message");
                    } catch (JSONException e) {
                        return;
                    }

                    // TODO Display successfull handshake
                    Toast.makeText(getApplicationContext(),
                            "Success in set user id", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Connect to server", Toast.LENGTH_LONG).show();
                    mSocket.emit("setUserId", username);
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Error in connecting", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getApplicationContext(),
                            "Some Message Received in main", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
