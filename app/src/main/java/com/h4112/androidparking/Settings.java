package com.h4112.androidparking;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Fenêtre des préférences.
 */
public class Settings extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}