package de.tritrack.recording.recording;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by till on 01.07.17.
 */

class StorageManager {

    private static final String TAG = "StorageManager";
    private static final long STORAGE_INTERVAL_MS = 4000;

    private Map<ActivityFeature, Integer> mFeaturePositions;
    private List<Double> mCurData;
    private Handler mHandler;
    private CSVWriter mWriter;

    public StorageManager() {
        mFeaturePositions = new HashMap<>();
        mCurData = new ArrayList<>();
        mHandler = new Handler();
    }

    public void addFeature(ActivityFeature feature) {
        // TODO: disallow adding features while recording is in progress
        assert !mFeaturePositions.containsKey(feature);
        Integer featurePos = mCurData.size();
        mFeaturePositions.put(feature, featurePos);
        mCurData.add(null);
    }

    public void setValue(ActivityFeature feature, Double value) {
        assert mFeaturePositions.containsKey(feature);
        Log.i(TAG, "getting feature " + feature);
        int featurePos = mFeaturePositions.get(feature);
        mCurData.set(featurePos, value);
    }

    public void startStoring() {
        // TODO: use internal directory and only export on request
        File outDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "TriTracks");
        if (!outDir.exists() && !outDir.mkdirs()) {
            Log.e(TAG, "Cannot create out dir " + outDir.getPath());
            return;
        }
        long time = System.currentTimeMillis() / 1000;
        final File outFile = new File(outDir, "Track_" + time);

        try {
            outFile.createNewFile();
            FileWriter fw = new FileWriter(outFile, true);
            mWriter = new CSVWriter(fw, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        } catch(IOException ex) {
            Log.e(TAG, "Error creating out file: " + ex.getMessage());
            return;
        }

        String[] featureDescriptions = new String[mCurData.size()];
        for (Map.Entry<ActivityFeature, Integer> featurePos : mFeaturePositions.entrySet()) {
            featureDescriptions[featurePos.getValue()] = featurePos.getKey().getDescription();
        }
        mWriter.writeNext(featureDescriptions);
        resumeStoring();
    }

    // TODO: check that these methods are called in the right way/states
    public void resumeStoring() {
        mHandler.post(new Runnable() {
            String[] values = new String[mCurData.size()];
            @Override
            public void run() {
                for (int i = 0; i < mCurData.size(); i++) {
                    Double data = mCurData.get(i);
                    values[i] = data == null ? "" : data.toString();
                }
                mWriter.writeNext(values);
                mHandler.postDelayed(this, STORAGE_INTERVAL_MS);
            }
        });
    }

    public void pauseStoring() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public void stopStoring() {
        pauseStoring();
        try {
            mWriter.close();
        } catch (IOException ex) {
            Log.e(TAG, "Error closing csv writer: " + ex.getMessage());
        }
    }

}
