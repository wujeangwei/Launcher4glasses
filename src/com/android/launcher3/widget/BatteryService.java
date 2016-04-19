package com.android.launcher3.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.util.Log;
import com.android.launcher3.R;

public class BatteryService extends Service {

	public BatteryService() {

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int currentBatteryLevel = intent.getIntExtra("level", 0);
			int currentBatteryStatus = intent.getIntExtra("status", 0);
			Log.e("sai", "currentBatteryLevel = " + currentBatteryLevel + " currentBatteryStatus = " + currentBatteryStatus);
			refreshBatteryStatus(context, currentBatteryLevel);
		}

	};

	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.e("sai", "onStart()");
		registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		//manager = AppWidgetManager.getInstance(this);
		//views = new RemoteViews(getPackageName(), R.layout.digital_appwidget);
		//thisWidget = new ComponentName(this, DigitalAppWidgetProvider.class);
	}

	private void refreshBatteryStatus(Context context, int currentBatteryLevel) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, DigitalAppWidgetProvider.class));
            for (int appWidgetId : appWidgetIds) {
                RemoteViews widget = new RemoteViews(context.getPackageName(),
                        R.layout.digital_appwidget);
                widget.setImageViewResource(R.id.battery, getBatteryIcon(currentBatteryLevel));
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
            }
        }
    }

	private int getBatteryIcon(int level) {
		if (level < 10) {
			return R.drawable.stat_sys_battery_10;
		} else if (level < 20) {
			return R.drawable.stat_sys_battery_20;
		} else if (level < 30) {
			return R.drawable.stat_sys_battery_30;
		} else if (level < 40) {
			return R.drawable.stat_sys_battery_40;
		} else if (level < 50) {
			return R.drawable.stat_sys_battery_50;
		} else if (level < 60) {
			return R.drawable.stat_sys_battery_60;
		} else if (level < 70) {
			return R.drawable.stat_sys_battery_70;
		} else if (level < 80) {
			return R.drawable.stat_sys_battery_80;
		} else if (level < 90) {
			return R.drawable.stat_sys_battery_90;
		} else {
			return R.drawable.stat_sys_battery_100;
		}
	}

}
