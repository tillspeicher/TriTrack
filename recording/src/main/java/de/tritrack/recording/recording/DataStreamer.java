package de.tritrack.recording.recording;

import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.functions.FuncN;
import rx.subjects.PublishSubject;

/**
 * Created by till on 03.06.17.
 */

class DataStreamer {

    private static final String TAG = "DataStreamer";
    private static final int TIME_DELAY_MS = 1000;

    private Map<ActivityFeature, PublishSubject<Double>> mInputs;
    private Map<ActivityFeature, Observable<Double>> mProviders;
    private Map<ActivityFeature, UICommunication.UIDataListener> mDataListeners;
    private List<Operator> mOperators;
    private StorageManager mStorageManager;

    private PublishSubject<Double> mTimePublisher;
    private Handler mTimeHandler;

    private boolean mIsResumed = false;
    private long mCheckpointTimeMs;
    private long mTimeOffsetMs;


    protected DataStreamer() {
        mInputs = new HashMap<>();
        mProviders = new HashMap<>();
        mDataListeners = new HashMap<>();
        mOperators = new ArrayList<>();
        mTimeHandler = new Handler();
    }

    protected void setDataListeners(Map<ActivityFeature, UICommunication.UIDataListener> listeners) {
        mDataListeners = listeners;
    }

    protected StorageManager resetState() {
        // TODO: reset the UI
        mProviders.clear();
        mOperators.clear();
        mDataListeners.clear();
        mStorageManager = new StorageManager();

        mTimeHandler.removeCallbacksAndMessages(null);
        mTimePublisher = setInput(ActivityFeature.TIME_S, true);
        mTimeOffsetMs = 0;

        return mStorageManager;
    }

    protected void setResumed(boolean isResumed) {
        mIsResumed = isResumed;
        if (isResumed) {
            startTimeRecording();
        } else {
            mTimeHandler.removeCallbacksAndMessages(null);
            mTimeOffsetMs += getElapsedTimeSinceResume();
        }
    }

    private void startTimeRecording() {
        // TODO: do delays matter for accuracy, are they detectable?
        mCheckpointTimeMs = SystemClock.elapsedRealtime();
        mTimeHandler.post(new Runnable() {
            @Override
            public void run() {
                mTimePublisher.onNext(getTotalTime());
                mTimeHandler.postDelayed(this, TIME_DELAY_MS);
            }
        });
    }

    private long getElapsedTimeSinceResume() {
        long curMs = SystemClock.elapsedRealtime();
        return curMs - mCheckpointTimeMs;
    }

    protected double getTotalTime() {
        if (!mIsResumed) {
            return mTimeOffsetMs / 1000.;
        } else {
            return (getElapsedTimeSinceResume() + mTimeOffsetMs) / 1000.;
        }
    }

    protected boolean hasInputSource(ActivityFeature activityFeature) {
        return mProviders.containsKey(activityFeature);
    }

    protected PublishSubject<Double> setInput(ActivityFeature feature,
                  boolean logFeature) {
//        if (mProviders.containsKey(feature))
//            throw new IllegalArgumentException("Cannot add source of feature " + feature + " twice.");
        Log.i(TAG, "adding source for feature " + feature);

        PublishSubject<Double> inSubject = mInputs.get(feature);
        if (inSubject == null) {
            inSubject = PublishSubject.create();
            mInputs.put(feature, inSubject);
            Observable<Double> obs = inSubject.map(new Func1<Double, Double>() {
                @Override
                public Double call(Double val) {
                    return val;
                }
            });
            addUiObserver(feature, obs);
            if (logFeature) {
                mStorageManager.addFeature(feature);
                addLoggingObserver(feature, obs);
            }
            mProviders.put(feature, obs);
            checkDependantOperators(feature);
        }

        return inSubject;
    }

    private void addOperator(ActivityFeature[] dependingFeatures, final ActivityFeature resFeature,
                               final Operator op, boolean logFeature) throws IllegalArgumentException {
        if (mProviders.containsKey(resFeature))
            throw new IllegalArgumentException("Cannot add operator for feature " + resFeature + " twice.");
        Log.i(TAG, "adding operator for feature " + resFeature);

        List<Observable<Double>> inObservables = new ArrayList<>();
        for (ActivityFeature depFeature : dependingFeatures) {
            Observable inObs = mProviders.get(depFeature);
            if (inObs != null) {
                inObservables.add(inObs.onBackpressureLatest());
                //inObservables.add(inObs);
                continue;
            }
            throw new IllegalArgumentException("No provider for feature " + depFeature + " available.");
        }

        Observable<Double> resObs;
        if (inObservables.size() > 1) {
            // TODO: is it better to use double[] instead of Double[]?
            // TODO: is zip() with drops from the sources in case of backpressure the right thing?
            resObs = Observable.zip(inObservables, new FuncN<Double[]>() {
                //        Observable<Double> resObs = Observable.combineLatest(inObservables, new FuncN<Double[]>() {
                @Override
                public Double[] call(Object[] args) {
                    //return Arrays.copyOf(args, args.length, Double[].class);
                    Double[] res = new Double[args.length];
                    int i = 0;
                    for (Object val : args) {
                        res[i++] = (double) val;
                    }
                    return res;
                }
            }).map(op);
        } else {
            // TODO: can this indirection be avoided?
            resObs = inObservables.get(0).map(new Func1<Double, Double>() {
                @Override
                public Double call(Double val) {
                    return op.call(new Double[]{val});
                }
            });
        }
        addUiObserver(resFeature, resObs);
        if (logFeature) {
            mStorageManager.addFeature(resFeature);
            addLoggingObserver(resFeature, resObs);
        }

        mProviders.put(resFeature, resObs);
        mOperators.add(op);
        checkDependantOperators(resFeature);
    }

    // TODO: maybe move the declaration of the correspondences
    private void checkDependantOperators(ActivityFeature feature) {
        switch(feature) {
            case HEART_RATE:
                addOperator(new ActivityFeature[]{ActivityFeature.HEART_RATE},
                        ActivityFeature.AVG_HEART_RATE, new DataStreamer.TimeAvgOperator(), false);
                addOperator(new ActivityFeature[]{ActivityFeature.HEART_RATE},
                        ActivityFeature.MAX_HEART_RATE, new MaxOperator(), false);
                break;
            case POWER_LEFT:
                if (!mProviders.containsKey(ActivityFeature.POWER_RIGHT))
                    break;
            case POWER_RIGHT:
                if (!mProviders.containsKey(ActivityFeature.POWER_LEFT))
                    break;
                // TODO: the time correspondence of the values should be checked
                addOperator(new ActivityFeature[]{ActivityFeature.POWER_LEFT, ActivityFeature.POWER_RIGHT},
                        ActivityFeature.POWER_COMBINED, new Operator() {//new Func1<Double[], Double>() {
                            @Override
                            public Double call(Double[] vals) {
                                Double value = Double.valueOf(vals[0] + vals[1]);
                                return value;
                            }
                        }, false);
                break;
            case POWER_COMBINED:
                addOperator(new ActivityFeature[]{ActivityFeature.POWER_COMBINED},
                        ActivityFeature.AVG_POWER_COMBINED, new TimeAvgOperator(), false);
                addOperator(new ActivityFeature[]{ActivityFeature.POWER_COMBINED},
                        ActivityFeature.MAX_POWER_COMBINED, new MaxOperator(), false);
                break;
            case LATITUDE:
                if (!mProviders.containsKey(ActivityFeature.LONGITUDE))
                    break;
            case LONGITUDE:
                if (!mProviders.containsKey(ActivityFeature.LONGITUDE))
                    break;
                addOperator(new ActivityFeature[]{ActivityFeature.LATITUDE, ActivityFeature.LONGITUDE},
                        ActivityFeature.DISTANCE_RAW_M, new Operator() {
                            Double lastLat = null, lastLong = null;
                            double dist = 0.0;
                            @Override
                            public Double call(Double[] vals) {
                                double curLat = vals[0];
                                double curLong = vals[1];
                                if (lastLat == null) {
                                    lastLat = curLat;
                                    lastLong = curLong;
                                    return dist;
                                }
                                float[] distRes = new float[1];
                                Location.distanceBetween(lastLat, lastLong, curLat, curLong, distRes);
//                                Log.i(TAG, "distance is " + distRes[0]);
                                dist += distRes[0];
                                lastLat = curLat;
                                lastLong = curLong;
                                return dist;
                            }
                        }, false /*ok?*/);
                break;
            case DISTANCE_RAW_M:
                addOperator(new ActivityFeature[]{ActivityFeature.DISTANCE_RAW_M},
                        ActivityFeature.DISTANCE_M, new Operator() {
                            double totalDist = 0.;
                            Double lastDist = 0.;

                            @Override
                            public Double call(Double[] vals) {
                                if (!mIsResumed) {
                                    lastDist = null;
                                    return totalDist;
                                }
                                double rawDist = vals[0];
                                if (lastDist == null)
                                    lastDist = rawDist;
                                double distDiff = rawDist - lastDist;
                                totalDist += distDiff;
                                return totalDist;
                            }
                        }, false);
                addOperator(new ActivityFeature[]{ActivityFeature.DISTANCE_RAW_M},
                        ActivityFeature.SPEED_MS, new TimedOperator() {
                            // TODO: is it necessary to force updates in periodic time intervals in
                            // case the GPS connection breaks?
                            Double lastDist = null;
                            @Override
                            public Double call(Double[] vals) {
                                double timeDiff = timeCheckpoint(true);
                                double distM = vals[0];
                                if (lastDist == null || timeDiff <= 0.) {
                                    lastDist = distM;
                                    return 0.;
                                }
                                double distDiff = distM - lastDist;
                                lastDist = distM;
                                return distDiff / timeDiff ;
                            }
                        }, false);
                break;
            case DISTANCE_M:
                addOperator(new ActivityFeature[]{ActivityFeature.DISTANCE_M},
                        ActivityFeature.DISTANCE_KM, new Operator() {
                            @Override
                            public Double call(Double[] vals) {
                                return vals[0] / 1000.0;
                            }
                        }, false);
                break;
            case SPEED_MS:
                addOperator(new ActivityFeature[]{ActivityFeature.SPEED_MS},
                        ActivityFeature.SPEED_KMH, new Operator() {
                            @Override
                            public Double call(Double[] speedMs) {
                                double val = speedMs[0] * 3.6;
                                return val;
                            }
                        }, true);
                addOperator(new ActivityFeature[]{ActivityFeature.SPEED_MS},
                        ActivityFeature.PACE, new Operator() {
                            @Override
                            public Double call(Double[] vals) {
                                double speed = vals[0];
                                if (speed == 0.)
                                    return 0.;
                                return (100. / 6.) / speed;
                            }
                        }, false);
                break;
            case SPEED_KMH:
                addOperator(new ActivityFeature[]{ActivityFeature.SPEED_KMH},
                        ActivityFeature.AVG_SPEED_KMH, new TimeAvgOperator(), false);
                addOperator(new ActivityFeature[]{ActivityFeature.SPEED_KMH},
                        ActivityFeature.MAX_SPEED_KMH, new MaxOperator(), false);
                break;
            case PACE:
                addOperator(new ActivityFeature[]{ActivityFeature.PACE},
                        ActivityFeature.AVG_PACE, new TimeAvgOperator(), false);
                break;
            case ALTITUDE:
                addOperator(new ActivityFeature[]{ActivityFeature.ALTITUDE},
                        ActivityFeature.ELEVATION_GAIN, new Operator() {//new Func1<Double[], Double>() {
                            double gain = 0.0;
                            Double lastAltitude = null;
                            @Override
                            public Double call(Double[] value) {
                                if (!mIsResumed) {
                                    lastAltitude = null;
                                    return gain;
                                }
                                double curAltitude = value[0];
                                if (lastAltitude == null)
                                    lastAltitude = curAltitude;
                                gain += Math.max(curAltitude - lastAltitude, 0.0);
                                lastAltitude = curAltitude;
                                return gain;
                            }
                        }, false);
                break;
            case LAST_CRANK_EVENT:
                addOperator(new ActivityFeature[]{ActivityFeature.LAST_CRANK_EVENT},
                        ActivityFeature.CADENCE, new EventPerMinOperator(), true);
                break;
            case CADENCE:
                addOperator(new ActivityFeature[]{ActivityFeature.CADENCE},
                        ActivityFeature.AVG_CADENCE, new TimeAvgOperator(), false);
                break;
        }
    }

    private void addUiObserver(final ActivityFeature feature, Observable<Double> obs) {
        final UICommunication.UIDataListener listener = mDataListeners.get(feature);
        if (listener == null)
            // TODO: maybe change this such that you can only register listeners if there is a
            // provider for the feature
            return;
        obs.subscribe(new Observer<Double>() {
            @Override
            public void onNext(Double val) {
                listener.onFeatureChanged(val);
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, "Error updating the UI: " + e.getMessage(), e);
            }

            @Override
            public void onCompleted() {
                Log.i(TAG, "Completed UI updating");
            }
        });
    }

    private void addLoggingObserver(final ActivityFeature feature, Observable<Double> obs) {
        obs.subscribe(new Observer<Double>() {
            @Override
            public void onNext(Double val) {
                //Log.i(TAG, "receiving value for " + feature + " for logging: " + val);
                mStorageManager.setValue(feature, val);
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, "Error in logging subscription: " + e.getMessage(), e);
            }

            @Override
            public void onCompleted() {
                Log.i(TAG, "Completed logging subscription");
            }
        });
    }

    private abstract class Operator implements Func1<Double[], Double> {
    }

    private abstract class TimedOperator extends Operator {
        long lastTimeMs = -1;

        double timeCheckpoint(boolean ignoreResumes) {
            if (!ignoreResumes && !mIsResumed) {
                lastTimeMs = -1;
                return -1.;
            }
            long curMs = SystemClock.elapsedRealtime();
            long timeDiff;
            if (lastTimeMs < 0) {
                // TODO: if the recording was paused and resumed but this method was not called in
                // between, this will be off, maybe we should use a counter for resumes
                timeDiff = curMs - mCheckpointTimeMs;
            } else {
                timeDiff = curMs - lastTimeMs;
            }
            lastTimeMs = curMs;
            return timeDiff / 1000.;
        }

    }

    private class TimeAvgOperator extends TimedOperator {
        Double lastAvg = null;

        @Override
        public Double call(Double[] vals) {
            double timeDiff = timeCheckpoint(false);
            if (timeDiff < 0.) {
                return lastAvg == null ? 0.0 : lastAvg;
            }
            double totalTime = getTotalTime();
            double curVal = vals[0];
            if (lastAvg == null)
                lastAvg = curVal;
            double curFrac = totalTime == 0.0 ? 0.0 : timeDiff / totalTime;
            // TODO: this is a stepwise function, should we interpolate?
            double avgVal = curVal * curFrac + (1.0 - curFrac) * lastAvg;
            lastAvg = avgVal;
            return avgVal;
        }
    }

    private class MaxOperator extends Operator {//implements Func1<Double[], Double> {
        Double maxVal = null;
        @Override
        public Double call(Double[] vals) {
            if (!mIsResumed)
                return maxVal == null ? 0.0 : maxVal;
            Double val = vals[0];
            if (maxVal == null)
                maxVal = val;
            maxVal = Math.max(val, maxVal);
            return maxVal;
        }
    }

    private class EventPerMinOperator extends Operator {
        Double lastTime = null;
        @Override
        public Double call(Double[] doubles) {
            Double time = doubles[0];
            if (lastTime == null) {
                lastTime = time;
                return 0.;
            }
            if (time == lastTime)
                return 0.;
            double diff = time - lastTime;
            if (time < lastTime)
                diff += 65535.;
            double rpm = 60. * 1024. / diff;
            lastTime = time;
            return rpm;
        }
    }

}
