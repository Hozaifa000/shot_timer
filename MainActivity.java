package com.shottimer.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(MicAudioPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
