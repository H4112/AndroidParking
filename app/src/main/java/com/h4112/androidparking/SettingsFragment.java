package com.h4112.androidparking;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.SeekBar;

import com.example.googlemaps.R;
import com.pavelsikun.seekbarpreference.SeekBarPreference;

import java.util.Map;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        updateSeekBarTitle();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSeekBarTitle();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void updateSeekBarTitle(){
        com.pavelsikun.seekbarpreference.SeekBarPreference seekBar = (SeekBarPreference)findPreference("portee");
        seekBar.setTitle(getString(R.string.afficherPlacesPortee)+" "+Integer.toString(seekBar.getCurrentValue())+" m");
    }
}