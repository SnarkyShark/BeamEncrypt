package edu.temple.basic;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, NfcAdapter.CreateNdefMessageCallback {

    // on screen elements
    EditText setUserEditText, messageEditText;
    Button setUserButton, keyModeButton, textModeButton;
    Spinner friendSpinner;
    RecyclerView recyclerView;

    // service
    KeyService mService;
    boolean mBound = false;

    // important stored values
    String username, selectedFriend;
    Boolean textMode;


    // Testing variables
    //String payload;
    String message;
    ArrayList<String> friends, receivedMessages;
    ArrayAdapter<String> friendAdapter;
    RecyclerView.Adapter messageAdapter;
    RecyclerView.LayoutManager layoutManager;

    // NFC
    NfcAdapter nfcAdapter;
    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // hooking up on screen elements
        final View mainView = findViewById(R.id.mainLayout);
        setUserEditText = findViewById(R.id.setUserEditText);
        setUserButton = findViewById(R.id.setUserButton);
        keyModeButton = findViewById(R.id.keyModeButton);
        messageEditText = findViewById(R.id.messageEditText);
        friendSpinner = findViewById(R.id.friendSpinner);
        textModeButton = findViewById(R.id.textModeButton);
        recyclerView = findViewById(R.id.recyclerView);

        // setting default values
        textMode = false;
        friends = new ArrayList<>();
        receivedMessages = new ArrayList<>();
        selectedFriend = "";

        // set username
        setUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUsername();
            }
        });

        // set key mode
        keyModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textMode = false;
                mainView.setBackgroundColor(getColor(R.color.key_background_color));
            }
        });

        // set text mode & message to send
        textModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textMode = true;
                mainView.setBackgroundColor(getColor(R.color.text_background_color));
                setMessage();
            }
        });

        friendAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, friends);
        friendSpinner.setAdapter(friendAdapter);
        friendSpinner.setOnItemSelectedListener(this);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        messageAdapter = new MyAdapter(receivedMessages);
        recyclerView.setAdapter(messageAdapter);

        // Android Beam
        Intent intent = new Intent(this, MainActivity.class);
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.setNdefPushMessageCallback(this, this);
    }

    /**
     * Set username
     * Generate public & private keys
     */
    private void setUsername() {
        username = setUserEditText.getText().toString();
        if (username.compareTo("") == 0)
            username = "default";
        setTitle("username: " + username);
        mService.genMyKeyPair(username);
    }

    /**
     * Set Beam Payload
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String payload;
        if(textMode) {
            payload = setMessage();
        }
        else {
            payload = setKey();
        }

        NdefRecord record = NdefRecord.createTextRecord(null, payload);
        NdefMessage msg = new NdefMessage(new NdefRecord[]{record});

        return msg;
    }

    private String setKey() {
        String pubKey = mService.getMyPublicKey();
        if(pubKey.equals("")){
            Log.d("SEND EMPTY KEY", "KEY WAS EMPTY!");
            return "";
        }
        else{
            return "{\"user\":\""+ username +"\",\"key\":\""+ pubKey +"\"}";
            //Log.d("SENT KEY PAYLOAD", payload);
        }
    }

    private String setMessage() {
        // validate that inputs exist
        if (selectedFriend.compareTo("") == 0) {
            Toast.makeText(this, "get some friends", Toast.LENGTH_SHORT).show();
            return "";
        }

        message = messageEditText.getText().toString();
        if (message.compareTo("") == 0) {
            Toast.makeText(this, "enter a message first!", Toast.LENGTH_SHORT).show();
            return "";
        }

        // encrypt message using private key
        String encryptedMessage = mService.encrypt(selectedFriend, message);
        Log.e(" keytrack", username + " encrypted result: " + encryptedMessage);

        return "{\"to\":\"" + selectedFriend + "\",\"from\":\""+ username + "\",\"message\""+
                ":\""+ encryptedMessage +"\"}";
        //Toast.makeText(this, "sent " + encryptedMessage, Toast.LENGTH_SHORT).show();
    }

    // respond to friendSpinner selection
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedFriend = parent.getItemAtPosition(position).toString();
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * Accept Beam Payload
     */
    void processIntent(Intent intent) {
        String payload = new String(
                ((NdefMessage) intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)[0])
                        .getRecords()[0]
                        .getPayload());
        //Lop off the 'en' language code.
        String jsonString = payload.substring(3);
        if(jsonString.equals("")){
            Log.d("Message Recieved?", "Message was empty!");
        }
        else {
            //Determine which json payload we've got here.

            try {
                JSONObject json = new JSONObject(jsonString);
                if(json.has("message")){
                    Log.e(" beamtrack", "we know it's a message");
                    acceptMessage(json);
                }
                else if (json.has("key")){
                    Log.e(" beamtrack", "we know it's a key");
                    acceptUser(json);
                }
                // else, something's wrong
            } catch (JSONException e) {
                Log.e("JSON Exception", "Convert problem", e);
            }
        }
    }

    /**
     * Accept new user
     */
    private void acceptUser(JSONObject json) {
        // TODO: do this when intent received
        String friend = "default";

        try {
            String owner = json.getString("user");
            String pemKey = json.getString("key");
            friend = owner;
            //Toast.makeText(this, pemKey, Toast.LENGTH_SHORT).show();

            if(mBound)
                mService.storePublicKey(owner, pemKey);
        } catch (Exception e) {
            Log.e(" track", "json convert problem");
        }

        friends.add(friend);
        friendAdapter.notifyDataSetChanged();
    }

    /**
     * Accept message
     */
    private void acceptMessage(JSONObject json) {
        // TODO: do this when intent received
        try {
            String sender = json.getString("from");
            String encryptedMessage = json.getString("message");

            String decryptedMessage = mService.decrypt(encryptedMessage, sender);
            receivedMessages.add(sender + ": " + decryptedMessage);
            messageAdapter.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Beam Stuff
     */

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
