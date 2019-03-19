package edu.temple.beamencrypt;

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

import java.io.File;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

import static android.nfc.NdefRecord.createMime;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback {

    KeyService mService;
    boolean mBound = false;
    boolean textmode = false;
    Button keymodeButton, textmodeButton;

    FragmentManager fm;
    KeyFragment keyFragment;
    TextFragment textFragment;

    NfcAdapter nfcAdapter;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final View mainView = findViewById(R.id.mainLayout);

        keymodeButton = findViewById(R.id.keymodeButton);
        textmodeButton = findViewById(R.id.textmodeButton);

        fm = getSupportFragmentManager();
        keyFragment = new KeyFragment();
        textFragment = new TextFragment();

        fm.beginTransaction()
                .replace(R.id.container, keyFragment)
                .commit();

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

        // Android Beam stuff

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Register callback
        nfcAdapter.setNdefPushMessageCallback(this, this);

    }

    /**
     * Android Beam Stuff
     */

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String payload = "";

        if(textmode) {
            payload = "text mode!!";
        }
        else {
            payload = "key mode!!";
        }

        /*switch(mMode){
            case MESSAGE_RECIEVE_MODE:
                //Uh do nothing?
                Log.d("RECIEVE NDEF WRITE", "User tried to send NDEF in recieve mode.");
                break;

            case MESSAGE_SEND_MODE:
                //Send currently 'set' message
                Log.d("SEND NDEF WRITE", "User tried to send NDEF in write mode.");
                if(mMessage != null && mPartner != null){
                    String encryptedMessage = mKeyService.encrypt(mMessage, mPartner);
                    payload = "{\"to\":\"" + mPartner + "\",\"from\":\""+ username + "\",\"message\""+
                            ":\""+ encryptedMessage +"\"}";
                }

                break;
            case KEY_SEND_MODE:
                //Send currently 'set' message
                Log.d("KEY NDEF WRITE", "User tried to send NDEF in key mode.");
                String pubKey = mKeyService.getMyPublicKey();
                if(pubKey.equals("")){
                    Log.d("SEND EMPTY KEY", "KEY WAS EMPTY!");
                }
                else{
                    payload = "{\"user\":\""+ username +"\",\"key\":\""+ pubKey +"\"}";
                    Log.d("SENT KEY PAYLOAD", payload);
                }
                break;
        } */

        NdefRecord record = NdefRecord.createTextRecord(null, payload);
        NdefMessage msg = new NdefMessage(new NdefRecord[]{record});

        return msg;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        textView = findViewById(R.id.textView);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        textView.setText(new String(msg.getRecords()[0].getPayload()));
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

}

