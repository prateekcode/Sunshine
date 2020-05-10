package com.androidmonk.sunshine.sync;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class SunshineSyncUtils {


    public static void startImmediateSync(@NonNull final Context context) {
//      COMPLETED (11) Within that method, start the SunshineSyncIntentService
        Intent intentToSyncImmediately = new Intent(context, SunshineSyncIntentService.class);
        context.startService(intentToSyncImmediately);
    }
}
