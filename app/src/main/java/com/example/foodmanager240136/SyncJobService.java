package com.example.foodmanager240136;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class SyncJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        // JobService runs on main thread; all HTTP + SQLite sync runs on worker.
        new Thread(() -> {
            boolean success = SyncManager.sync(getApplicationContext());
            jobFinished(params, !success);
        }, "BIT240136-MockAPI-Sync").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
