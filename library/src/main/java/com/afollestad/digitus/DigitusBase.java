package com.afollestad.digitus;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * @author Aidan Follestad (afollestad)
 */
@TargetApi(Build.VERSION_CODES.M)
class DigitusBase {

    protected static Digitus mInstance;

    protected <T extends Activity & DigitusCallback> DigitusBase(T context, String keyName) {
        mKeyName = keyName;
        mCallback = context;
    }

    protected void deinitBase() {
        mKeyName = null;
        mCallback = null;
        mKeyguardManager = null;
        mFingerprintManager = null;
        mKeyStore = null;
        mKeyGenerator = null;
        mCipher = null;
    }

    public static boolean isReady() {
        invalidate();
        return mInstance != null && mInstance.mIsReady;
    }

    protected static void invalidate() {
        if (mInstance == null)
            throw new IllegalStateException("Digitus has not been initialized yet.");
        else if (!isReady())
            throw new IllegalStateException("Digitus is not yet ready.");
    }

    protected boolean mIsReady;
    protected String mKeyName;
    protected DigitusCallback mCallback;
    protected KeyguardManager mKeyguardManager;
    protected FingerprintManager mFingerprintManager;
    protected KeyStore mKeyStore;
    protected KeyGenerator mKeyGenerator;
    protected Cipher mCipher;

    /**
     * Initialize the {@link Cipher} instance with the created key in the {@link #recreateKey()}
     * method.
     *
     * @return {@code true} if initialization is successful, {@code false} if the lock screen has
     * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
     * the key was generated.
     */
    protected boolean initCipher() {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(mKeyName, null);
            mCipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     */
    public static void recreateKey() {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        invalidate();
        try {
            mInstance.mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            mInstance.mKeyGenerator.init(new KeyGenParameterSpec.Builder(mInstance.mKeyName,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                            // Require the user to authenticate with a fingerprint to authorize every use
                            // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            mInstance.mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
