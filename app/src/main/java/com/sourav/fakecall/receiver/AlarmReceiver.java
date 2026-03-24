package com.sourav.fakecall.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.sourav.fakecall.FakeCallActivity;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String callerName = intent.getStringExtra("CALLER_NAME");
        
        Intent callIntent = new Intent(context, FakeCallActivity.class);
        callIntent.putExtra("CALLER_NAME", callerName != null ? callerName : "The Future");
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        context.startActivity(callIntent);
    }
}
