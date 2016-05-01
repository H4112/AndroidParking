package com.h4112.androidparking;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Contenu de la fenêtre de préférences.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}