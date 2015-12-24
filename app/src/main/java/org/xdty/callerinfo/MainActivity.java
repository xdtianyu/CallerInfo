package org.xdty.callerinfo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.NumberInfo;

import wei.mark.standout.StandOutWindow;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        StandOutWindow.closeAll(this, FloatWindow.class);
        new PhoneNumber(this, new PhoneNumber.Callback() {
            @Override
            public void onResponse(NumberInfo numberInfo) {
                String text = numberInfo.toString();
                Bundle bundle = new Bundle();
                bundle.putString(FloatWindow.NUMBER_INFO, text);
                bundle.putInt(FloatWindow.WINDOW_COLOR, android.R.color.holo_blue_light);
                StandOutWindow.show(MainActivity.this, FloatWindow.class, FloatWindow.VIEWER_FRONT);
                StandOutWindow.sendData(MainActivity.this, FloatWindow.class,
                        FloatWindow.VIEWER_FRONT, 0, bundle, FloatWindow.class, 0);
            }

            @Override
            public void onResponseFailed(NumberInfo numberInfo) {

            }
        }).fetch("10086");
    }

    @Override
    protected void onStop() {
        StandOutWindow.closeAll(this, FloatWindow.class);
        super.onStop();
    }
}
