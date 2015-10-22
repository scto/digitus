package com.afollestad.digitus;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.provider.Settings;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.afollestad.digitus.google.FingerprintAuthenticationDialogFragment;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Digitus extends DigitusBase {

    private int mRequestCode;
    private FingerprintAuthenticationDialogFragment mFragment;

    public static boolean notifyPasswordValidation(boolean valid) {
        invalidate();
        if (mInstance.mFragment == null) return false;
        mInstance.mFragment.notifyPasswordValidation(valid);
        return true;
    }

    public static boolean openSecuritySettings() {
        invalidate();
        if (mInstance.mCallback == null)
            return false;
        ((Activity) mInstance.mCallback).startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        return true;
    }

    private <T extends Activity & DigitusCallback> Digitus(T context, String keyName) {
        super(context, keyName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            mFingerprintManager = context.getSystemService(FingerprintManager.class);
            try {
                mKeyStore = KeyStore.getInstance("AndroidKeyStore");
            } catch (KeyStoreException e) {
                throw new RuntimeException("Failed to get an instance of KeyStore", e);
            }
            try {
                mKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
            }
            try {
                mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new RuntimeException("Failed to get an instance of Cipher", e);
            }
        }
    }

    public static <T extends Activity & DigitusCallback> Digitus init(T context, String keyName, int requestCode) {
        mInstance = new Digitus(context, keyName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mInstance.mRequestCode = requestCode;
            int granted = ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT);
            if (granted != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.USE_FINGERPRINT}, requestCode);
            else finishInit();
        } else finishInit();
        return mInstance;
    }

    public static void deinit() {
        if (mInstance == null) return;
        if (mInstance.mFragment != null) mInstance.mFragment.dismiss();
        mInstance.mRequestCode = 0;
        mInstance.deinitBase();
        mInstance = null;
    }

    private static void finishInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isFingerprintRegistered()) {
                mInstance.mIsReady = true;
                recreateKey();
                mInstance.mCallback.onDigitusReady();
            } else {
                mInstance.mCallback.onDigitusRegistrationNeeded();
            }
        } else {
            mInstance.mIsReady = true;
            mInstance.mCallback.onDigitusReady();
        }
    }

    public static void handleResult(int requestCode, String[] permissions, int[] state) {
        if (requestCode == mInstance.mRequestCode && permissions != null &&
                permissions[0].equals(Manifest.permission.USE_FINGERPRINT)) {
            if (state[0] == PackageManager.PERMISSION_GRANTED) {
                finishInit();
            } else {
                mInstance.mCallback.onDigitusError(new PermissionDeniedError());
            }
        }
    }

    public static void beginAuthentication() {
        invalidate();
        final Activity context = (Activity) mInstance.mCallback;
        mInstance.mFragment = new FingerprintAuthenticationDialogFragment();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mInstance.mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
        } else if (mInstance.initCipher()) {
            mInstance.mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mInstance.mCipher));
            mInstance.mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
        } else {
            mInstance.mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mInstance.mCipher));
            mInstance.mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
        }

        mInstance.mFragment.show(context.getFragmentManager(), "[fingerprint-tag]");
    }

    public static boolean isFingerprintRegistered() {
        invalidate();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;
        int granted = ContextCompat.checkSelfPermission((Activity) mInstance.mCallback,
                Manifest.permission.USE_FINGERPRINT);
        if (granted != PackageManager.PERMISSION_GRANTED)
            return false;
        //noinspection ResourceType
        return mInstance.mKeyguardManager.isKeyguardSecure() &&
                mInstance.mFingerprintManager.hasEnrolledFingerprints();
    }
}