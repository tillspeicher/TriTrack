package de.tritrack.tritrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.tritrack.recording.recording.ActivityFeature;
import de.tritrack.recording.recording.Recorder;
import de.tritrack.recording.recording.UICommunication;
import de.tritrack.recording.ui.DataTableProvider;

public class MainActivity extends WearableActivity {

    private static final int REQUEST_CODE_BOTH = 2;

    private Recorder mRec;

    private BoxInsetLayout mContainerView;
    private ImageButton mStartStopButton;
    private ImageButton mPauseResumeButton;
    private List<TextView> mTextViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);

        mRec = Recorder.Companion.getInstance(getApplicationContext());
        ActivityFeature[][] featureLayout = new ActivityFeature[][]{
                new ActivityFeature[]{ActivityFeature.TIME_S, ActivityFeature.DISTANCE_KM},
                new ActivityFeature[]{ActivityFeature.PACE, ActivityFeature.AVG_PACE},
//                new ActivityFeature[]{ActivityFeature.HEART_RATE, ActivityFeature.AVG_HEART_RATE}
                new ActivityFeature[]{ActivityFeature.SPEED_MS, ActivityFeature.SPEED_KMH}
        };
        LinearLayout contentLayout = (LinearLayout) findViewById(R.id.content_main);
        mTextViews = new ArrayList<>();
        Map<ActivityFeature, UICommunication.UIDataListener> listeners =
                DataTableProvider.INSTANCE.getTableView(featureLayout, this, contentLayout, mTextViews);
        mRec.setDataListeners(listeners);

        mStartStopButton = (ImageButton) findViewById(R.id.button_start_stop);
        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopTracking();
            }
        });
        mPauseResumeButton = (ImageButton) findViewById(R.id.button_pause_resume);
        mPauseResumeButton.setEnabled(false);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRec.isRecording())
            toggleTracking();
    }

    // TODO: copy past from wear

    public void startStopTracking() {
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            // TODO: show rationale?
//            ActivityCompat.requestPermissions(this,
//                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
//                    REQUEST_CODE_LOCATION);
//            return;
//        }
//        // TODO: revise this and try to remove it
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            // TODO: show rationale?
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    REQUEST_CODE_EXTERNAL_STORAGE);
//            return;
//        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: show rationale?
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_BOTH);
            return;
        }
        toggleTracking();
    }

    private void toggleTracking() {
        boolean isRecording = mRec.toggleRecording();
        if (isRecording) {
            // TODO: add Lap button functionality
            mStartStopButton.setImageResource(R.drawable.stop_sym);
            mPauseResumeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isResumed = mRec.togglePause();
                    if (isResumed)
                        mPauseResumeButton.setImageResource(R.drawable.pause_sym);
                    else
                        mPauseResumeButton.setImageResource(R.drawable.start_sym);
                }
            });
            mPauseResumeButton.setEnabled(true);
        } else {
            mStartStopButton.setImageResource(R.drawable.start_sym);
            mPauseResumeButton.setImageResource(R.drawable.pause_sym);
            mPauseResumeButton.setEnabled(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
//            case REQUEST_CODE_LOCATION: {
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // TODO: revise this behavior
//                    toggleTracking();
//                } else {
//                    // TODO
//                    throw new IllegalStateException();
//                }
//            }
            case REQUEST_CODE_BOTH:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO: revise this behavior
                    toggleTracking();
                } else {
                    // TODO
                    throw new IllegalStateException();
                }
                break;
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(Color.BLACK);
            for (TextView tv : mTextViews) {
                tv.setTextColor(Color.WHITE);
            }
        } else {
            mContainerView.setBackground(null);
            for (TextView tv : mTextViews) {
                tv.setTextColor(Color.BLACK);
            }
//            mStatsView.setTextColor(getResources().getColor(android.R.color.black));
            //mClockView.setVisibility(View.GONE);
        }
    }
}
