package edu.temple.beamencrypt;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback, KeyFragment.KeyInterface {

    // View elements
    Button keymodeButton, textmodeButton;
    TextView textView;

    // Fragment Navigation
    FragmentManager fm;
    KeyFragment keyFragment;
    TextFragment textFragment;
    boolean textmode = false;
    private String username;

    // Service
    KeyService mService;
    boolean mBound = false;

    // NFC
    NfcAdapter nfcAdapter;
    private PendingIntent mPendingIntent;
    private String payload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View Elements
        setContentView(R.layout.activity_main);
        final View mainView = findViewById(R.id.mainLayout);
        keymodeButton = findViewById(R.id.keymodeButton);
        textmodeButton = findViewById(R.id.textmodeButton);
        textView = findViewById(R.id.testTextView);

        // Fragment Nav
        fm = getSupportFragmentManager();
        keyFragment = new KeyFragment();
        textFragment = new TextFragment();

        fm.beginTransaction()
                .replace(R.id.container, keyFragment)
                .commit();

        username = "default";

        keymodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textmode = false;
                mainView.setBackgroundColor(getColor(R.color.key_background));
                fm.beginTransaction()
                        .replace(R.id.container, keyFragment)
                        .commit();
            }
        });

        textmodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textmode = true;
                mainView.setBackgroundColor(getColor(R.color.text_background));
                fm.beginTransaction()
                        .replace(R.id.container, textFragment)
                        .commit();
            }
        });

        // Android Beam
        Intent intent = new Intent(this, MainActivity.class);
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.setNdefPushMessageCallback(this, this);

    }

    public void testValue(String test) {
        textView.setText(test);
    }

    /**
     * Android Beam
     */

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        if(textmode) {
            payload = "text mode!!";
        }
        else {
            payload = "key mode!!";
            String pubKey = mService.getMyPublicKey();
            if(pubKey.equals("")){
                Log.d("SEND EMPTY KEY", "KEY WAS EMPTY!");
            }
            else{
                payload = "{\"user\":\""+ username +"\",\"key\":\""+ pubKey +"\"}";
                Log.d("SENT KEY PAYLOAD", payload);
            }
        }

        NdefRecord record = NdefRecord.createTextRecord(null, payload);
        NdefMessage msg = new NdefMessage(new NdefRecord[]{record});

        return msg;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e( " beamtrack", "We resumed");

        // Get the intent from Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Log.e( " beamtrack", "We discovered an NDEF");
            processIntent(getIntent());
        }
        nfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();

        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);  // look for new intents
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        String payload = new String(
                ((NdefMessage) intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)[0])
                        .getRecords()[0]
                        .getPayload());
        Toast.makeText(this, "Recieved NFC tag", Toast.LENGTH_LONG).show();

        //Lop off the 'en' language code.
        String jsonString = payload.substring(3);
        Log.d("Tag debug", jsonString);
        if(jsonString.equals("")){
            Log.d("Message Recieved?", "Message was empty!");
        }
        else {
            //Determine which json payload we've got here.

            try {
                JSONObject json = new JSONObject(jsonString);
                /*if(json.has("message")){
                    manageMessageJSON(json);
                }
                else */ if (json.has("key")){
                    Log.e(" beamtrack", "we know it's a key");
                    manageKeyJson(json);
                }
                //else do nothing bc json is messed up.
            } catch (JSONException e) {
                Log.e("JSON Exception", "Convert problem", e);
            }
        }
    }

    private void manageKeyJson(JSONObject json){
        try {
            String owner = json.getString("user");
            String pemKey = json.getString("key");
            if(mBound)
                mService.storePublicKey(owner, pemKey);/*else{
                mStoreKeyWhenReady = true;
                mTempOwner = owner;
                mTempPemKey = pemKey;
            } */
            Toast.makeText(this, "User: " + owner, Toast.LENGTH_SHORT).show();
        }
        catch (JSONException e){
            Log.e("JSON Exception", "Key Problem", e);
        }

    }

    /**
     * Service Stuff
     */

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, KeyService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.e(" keytrack", "we tried to bind");

    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        mBound = false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            KeyService.LocalBinder binder = (KeyService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.e(" keytrack", "connected to the service");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    // Fragment Communication

    public void setUsername() {
        username = keyFragment.getUsername();
        mService.genMyKeyPair();
        //testValue(username);
    }

}

