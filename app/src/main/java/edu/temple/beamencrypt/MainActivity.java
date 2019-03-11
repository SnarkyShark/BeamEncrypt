package edu.temple.beamencrypt;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
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
    boolean finishedEncrypt = false;
    Button getKeysButton, encryptButton, decryptButton;
    EditText messageText;
    TextView encryptResult, decryptResult, privateText, publicText;
    byte [] encryptedBytes, decryptedBytes;
    String encrypted, decrypted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getKeysButton = findViewById(R.id.button);
        encryptButton = findViewById(R.id.encryptButton);
        decryptButton = findViewById(R.id.decryptButton);
        messageText = findViewById(R.id.encryptMessageEditText);
        encryptResult = findViewById(R.id.encryptedMessageText);
        decryptResult = findViewById(R.id.decryptedMessageText);
        privateText = findViewById(R.id.privateKeyText);
        publicText = findViewById(R.id.pubicKeyText);


        getKeysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(" keytrack", "get keys button");
                if(mBound) {
                    Log.e(" keytrack", "service bound");

                    try {
                        mService.getMyKeyPair();
                        privateText.setText(mService.getMyKeyPair().getPrivate().toString());
                        publicText.setText(mService.getMyKeyPair().getPublic().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                else
                    Log.e(" keytrack", "service not bound");

            }
        });

        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // encrypt the text and print on screen
                String message = messageText.getText().toString();
                if(mBound && message.compareTo("") != 0) {

                    try {
                        PrivateKey privateKey = mService.getMyKeyPair().getPrivate();
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
                        encryptedBytes = cipher.doFinal(message.getBytes());
                        encrypted = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
                        //encrypted = new String(encryptedBytes);
                        encryptResult.setText(encrypted);
                        finishedEncrypt = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else
                    Toast.makeText(MainActivity.this, "we have a problem", Toast.LENGTH_SHORT).show();
            }
        });

        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // decrypt the text and print on screen
                if(mBound && finishedEncrypt) {

                    try {
                        PublicKey publicKey = mService.getMyKeyPair().getPublic();
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.DECRYPT_MODE, publicKey);
                        decryptedBytes = cipher.doFinal(encryptedBytes);
                        decrypted = new String(decryptedBytes);
                        decryptResult.setText(decrypted);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else
                    Toast.makeText(MainActivity.this, "we have a problem", Toast.LENGTH_SHORT).show();
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

