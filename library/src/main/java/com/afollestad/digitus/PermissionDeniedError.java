package com.afollestad.digitus;

import android.os.Build;

/**
 * @author Aidan Follestad (afollestad)
 */
public class PermissionDeniedError extends Exception {

    public PermissionDeniedError() {
        super(String.format("The USE_FINGERPRINT permission was denied %s",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? "by the user." : "because it's missing from this app's manifest."));
    }
}
