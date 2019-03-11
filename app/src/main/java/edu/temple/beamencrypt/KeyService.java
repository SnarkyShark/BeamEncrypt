package edu.temple.beamencrypt;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

public class KeyService extends Service {

    PublicKey storedPublicKey;
    PrivateKey storedPrivateKey;
    Map <String, String> storedKeys;
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        KeyService getService() {
            // Return this instance of KeyService so clients can call public methods
            return KeyService.this;
        }
    }

    public KeyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        storedKeys = new HashMap<String, String>();

        return mBinder;
    }

    public KeyPair getMyKeyPair() throws NoSuchAlgorithmException {

        KeyPair kp;

        if(storedPublicKey == null || storedPrivateKey == null) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            kp = kpg.generateKeyPair();
            storedPublicKey = kp.getPublic();
            storedPrivateKey = kp.getPrivate();
        }
        else
            kp = new KeyPair(storedPublicKey, storedPrivateKey);

        return kp;
    }

    public void storePublicKey (String partnerName, String publicKey) {
        storedKeys.put(partnerName, publicKey);
    }

    public RSAPublicKey getPublicKey(String partnerName) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String publicKey = storedKeys.get(partnerName);
        byte[] publicBytes = Base64.decode(publicKey, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    public void resetMyKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        storedPublicKey = kp.getPublic();
        storedPrivateKey = kp.getPrivate();
    }

    public void resetPublicKey(String partnerName) {
        storedKeys.remove(partnerName);
    }
}
