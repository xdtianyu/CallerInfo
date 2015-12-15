package org.xdty.callerinfo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import wei.mark.standout.StandOutWindow;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StandOutWindow.closeAll(this, FloatWindow.class);
    }
}
