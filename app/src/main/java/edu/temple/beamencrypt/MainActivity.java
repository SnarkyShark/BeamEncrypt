package edu.temple.beamencrypt;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
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

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity {

    KeyService mService;
    boolean mBound = false;
    boolean textmode = false;
    Button keymodeButton, textmodeButton;

    FragmentManager fm;
    KeyFragment keyFragment;
    TextFragment textFragment;

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
    }

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

