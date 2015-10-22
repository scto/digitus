package com.afollestad.digitus;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface DigitusCallback {

    void onDigitusReady();

    void onDigitusRegistrationNeeded();

    void onDigitusAuthenticated();

    void onDigitusError(Exception e);

    void onDigitusValidatePassword(String password);
}