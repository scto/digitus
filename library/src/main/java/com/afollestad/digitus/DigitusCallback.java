package com.afollestad.digitus;

import android.support.annotation.NonNull;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface DigitusCallback {

    void onDigitusReady(Digitus digitus);

    void onDigitusRegistrationNeeded(Digitus digitus);

    void onDigitusAuthenticated(Digitus digitus);

    void onDigitusError(Digitus digitus, Exception e);

    void onDigitusValidatePassword(Digitus digitus, String password);
}