/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.launcher3.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Locale;
import com.android.launcher3.R;
// cg sai.pan begin
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.UserHandle;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.List;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import android.database.ContentObserver;
import android.location.LocationManager;
// cg sai.pan end
public class DigitalAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "DigitalAppWidgetProvider";

    /**
     * Intent to be used for checking if a world clock's date has changed. Must be every fifteen
     * minutes because not all time zones are hour-locked.
     **/
    public static final String ACTION_ON_QUARTER_HOUR = "com.android.deskclock.ON_QUARTER_HOUR";

    // Lazily creating this intent to use with the AlarmManager
    private PendingIntent mPendingIntent;
    // Lazily creating this name to use with the AppWidgetManager
    private ComponentName mComponentName;
	// cg sai.pan begin
	private Context mComtext;
	private WifiManager mWifiManager;
	private BluetoothAdapter bluetoothApapter;
	private BluetoothManager bluetoothManager;
	
	public LocationClient mLocationClient;
    public LocationListener mLocationListener;
	private LocationManager mLocationManager;

	private static final String WEATHER_SUNNY = "0"; //晴
	private static final String WEATHER_CLOUDY = "1"; //多云
	private static final String WEATHER_OVERCAST = "2"; //阴
	private static final String WEATHER_SHOWER = "3"; //阵雨
	private static final String WEATHER_SLEET = "6"; //雨夹雪
	private static final String WEATHER_LIGHT_RAIN = "7"; //小雨
	private static final String WEATHER_MODERATE_RAIN = "8"; //中雨
	private static final String WEATHER_HEAVY_RAIN = "9"; //大雨
	//private static final String WEATHER_HAIL
	//private static final String WEATHER_STORM
	private static final String WEATHER_SNOW_SHOWER = "13"; //阵雪
	private static final String WEATHER_LIGHT_SNOW = "14"; //小雪
	private static final String WEATHER_MODERATE_SNOW = "15"; //中雪
	private static final String WEATHER_HEAVY_SNOW = "16"; //大雪
	private static final String WEATHER_FOG = "18"; //雾
	// cg sai.pan end

    public DigitalAppWidgetProvider() {
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        startAlarmOnQuarterHour(context);
		// cg sai.pan begin
		importInitDatabase(context);
		context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.System.LOCATION_PROVIDERS_ALLOWED), true, mGpsObserver);
		context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true, mAirplaneModeObserver);
		//context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.MOBILE_DATA), true, mMobileDataObserver);
		// cg sai.pan end
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        cancelAlarmOnQuarterHour(context);
		context.getContentResolver().unregisterContentObserver(mGpsObserver);
		context.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
		context.getContentResolver().unregisterContentObserver(mMobileDataObserver);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
		mComtext = context;
        String action = intent.getAction();
		Log.i("sai", "onReceive: " + action);

        super.onReceive(context, intent);

        if (ACTION_ON_QUARTER_HOUR.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            if (appWidgetManager != null) {
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(context));
                for (int appWidgetId : appWidgetIds) {
                    RemoteViews widget = new RemoteViews(context.getPackageName(),
                            R.layout.digital_appwidget);
                    float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
                    // SPRD for bug421127 add am/pm for widget
                    WidgetUtils.setTimeFormat(widget,(int)context.getResources().getDimension(R.dimen.widget_label_font_size), R.id.the_clock);
                    WidgetUtils.setClockSize(context, widget, ratio);
                    //refreshAlarm(context, widget);
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
                }
            }
            if(!ACTION_ON_QUARTER_HOUR.equals(action)) {
                cancelAlarmOnQuarterHour(context);
            }
            startAlarmOnQuarterHour(context);
        }
		// cg sai.pan begin
		else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			if (appWidgetManager != null) {
				int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(context));
				for (int appWidgetId : appWidgetIds) {
					RemoteViews widget = new RemoteViews(context.getPackageName(),
							R.layout.digital_appwidget);
					refreshBtStatus(context, widget);
					appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
				}
			}
		} else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
			int wifiStatus = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
			Log.e("sai", "wifiStatus" + wifiStatus);
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			if (appWidgetManager != null) {
				int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(context));
				for (int appWidgetId : appWidgetIds) {
					RemoteViews widget = new RemoteViews(context.getPackageName(),
							R.layout.digital_appwidget);
					if (WifiManager.WIFI_STATE_ENABLED == wifiStatus || WifiManager.WIFI_STATE_ENABLING == wifiStatus) {
						widget.setImageViewResource(R.id.wifi, R.drawable.status_wifi_on);
					} else {
						widget.setImageViewResource(R.id.wifi, R.drawable.status_wifi_off);
					}
					appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
				}
			}
		} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			if (appWidgetManager != null) {
				int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(context));
				for (int appWidgetId : appWidgetIds) {
					RemoteViews widget = new RemoteViews(context.getPackageName(),
							R.layout.digital_appwidget);
					refreshWifiStatus(context, widget);
				}
			}
		} else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
			if (isNetworkConnected(context)) {
				Log.e("sai", "isNetworkConnected true");
				requestLocation(context);
			} else {
				Log.e("sai", "isNetworkConnected false");
			}
		}
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		/*Intent mIntent = new Intent();
		mIntent.setAction("android.intent.action.battery");
		Intent eintent = new Intent(getExplicitIntent(context, mIntent));
        context.startService(eintent);*/
		Intent mIntent = new Intent(context, BatteryService.class);
		//context.startServiceAsUser(mIntent, UserHandle.CURRENT);
		Log.e("sai", "onUpdate startService");
		context.startService(mIntent);
        for (int appWidgetId : appWidgetIds) {
            float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
            updateClock(context, appWidgetManager, appWidgetId, ratio);
        }
        startAlarmOnQuarterHour(context);
		// cg sai.pan begin
		Log.e("sai", "mLocationClient start");
		if (mLocationClient == null) {
			Log.e("sai", "onUpdate mLocationClient == null");
		} else {
			Log.e("sai", "onUpdate mLocationClient != null");
		}
		//mLocationClient.start();//定位SDK start之后会默认发起一次定位请求，开发者无须判断isstart并主动调用request
		//mLocationClient.requestLocation();
		//requestLocation(context);
		// cg sai.pan end
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        // scale the fonts of the clock to fit inside the new size
        float ratio = WidgetUtils.getScaleRatio(context, newOptions, appWidgetId);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        updateClock(context, widgetManager, appWidgetId, ratio);
    }

    private void updateClock(
            Context context, AppWidgetManager appWidgetManager, int appWidgetId, float ratio) {
        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.digital_appwidget);

        // Launch setinngs when clicking on the time in the widget only if not a lock screen widget
        Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        if (newOptions != null &&
                newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
                != AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
			Intent mIntent = new Intent(Intent.ACTION_MAIN);
			mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			ComponentName component = new ComponentName("com.android.settings", "com.android.settings.Settings");
			mIntent.setComponent(component);
            widget.setOnClickPendingIntent(R.id.digital_appwidget,
                    PendingIntent.getActivity(context, 0, mIntent, 0));
        }

		//cg sai.pan begin
		refreshWifiStatus(context, widget);
		refreshBtStatus(context, widget);
		refreshAirplaneStatus(context, widget, Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0);
		mLocationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
		refreshGpsStatus(context, widget, mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
		//refreshDataStatus(context, widget, Settings.Global.getInt(context.getContentResolver(), Settings.Global.MOBILE_DATA, 0) != 0);
		requestLocation(context);
		//cg sai.pan end

        // SPRD for bug421127 add am/pm for widget
        WidgetUtils.setTimeFormat(widget,(int)context.getResources().getDimension(R.dimen.widget_label_font_size), R.id.the_clock);
        WidgetUtils.setClockSize(context, widget, ratio);

        // Set today's date format
        CharSequence dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                context.getString(R.string.abbrev_wday_month_day_no_year));
        widget.setCharSequence(R.id.date, "setFormat12Hour", dateFormat);
        widget.setCharSequence(R.id.date, "setFormat24Hour", dateFormat);

        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    /**
     * Start an alarm that fires on the next quarter hour to update the world clock city
     * day when the local time or the world city crosses midnight.
     *
     * @param context The context in which the PendingIntent should perform the broadcast.
     */
    private void startAlarmOnQuarterHour(Context context) {
        if (context != null) {
            long onQuarterHour = Utils.getAlarmOnQuarterHour();
            PendingIntent quarterlyIntent = getOnQuarterHourPendingIntent(context);
            AlarmManager alarmManager = ((AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE));
            if (Utils.isKitKatOrLater()) {
                alarmManager.setExact(AlarmManager.RTC, onQuarterHour, quarterlyIntent);
            } else {
                alarmManager.set(AlarmManager.RTC, onQuarterHour, quarterlyIntent);
            }
        }
    }


    /**
     * Remove the alarm for the quarter hour update.
     *
     * @param context The context in which the PendingIntent was started to perform the broadcast.
     */
    public void cancelAlarmOnQuarterHour(Context context) {
        if (context != null) {
            PendingIntent quarterlyIntent = getOnQuarterHourPendingIntent(context);
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(
                    quarterlyIntent);
        }
    }

    /**
     * Create the pending intent that is broadcast on the quarter hour.
     *
     * @param context The Context in which this PendingIntent should perform the broadcast.
     * @return a pending intent with an intent unique to DigitalAppWidgetProvider
     */
    private PendingIntent getOnQuarterHourPendingIntent(Context context) {
        if (mPendingIntent == null) {
            mPendingIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_ON_QUARTER_HOUR), PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return mPendingIntent;
    }

    /**
     * Create the component name for this class
     *
     * @param context The Context in which the widgets for this component are created
     * @return the ComponentName unique to DigitalAppWidgetProvider
     */
    private ComponentName getComponentName(Context context) {
        if (mComponentName == null) {
            mComponentName = new ComponentName(context, getClass());
        }
        return mComponentName;
    }
    
    private void refreshCityOrWeather(Context context, String what, int which) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(context));
            for (int appWidgetId : appWidgetIds) {
                RemoteViews widget = new RemoteViews(context.getPackageName(),
                        R.layout.digital_appwidget);
                if (1 == which) {
					widget.setTextViewText(R.id.city, what);
				} else if (2 == which) {
					if (what.equals("")) {
						//widget.setTextViewText(R.id.temperature, context.getString(R.string.weather_network_error));
					} else if (what.contains(",")) {
						String weather = what.split(",")[0];
						String temperature = what.split(",")[1];
						String image = what.split(",")[2];
						widget.setTextViewText(R.id.temperature, temperature);
						widget.setImageViewResource(R.id.weather, getWeatherImageId(image));
					}
				}
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
            }
        }
    }

	private int getWeatherImageId(String img) {
		int imageId = 0;
		switch (img) {
			case WEATHER_SUNNY:
				imageId = R.drawable.sunny;
				break;
			case WEATHER_CLOUDY:
				imageId = R.drawable.cloudy;
				break;
			case WEATHER_OVERCAST:
				imageId = R.drawable.overcast;
				break;
			case WEATHER_SHOWER:
				imageId = R.drawable.rain;
				break;
			case WEATHER_SLEET:
				imageId = R.drawable.sleet;
				break;
			case WEATHER_LIGHT_RAIN:
				imageId = R.drawable.rain;
				break;
			case WEATHER_MODERATE_RAIN:
				imageId = R.drawable.rain;
				break;
			case WEATHER_HEAVY_RAIN:
				imageId = R.drawable.rain;
				break;
			case WEATHER_LIGHT_SNOW:
				imageId = R.drawable.snow;
				break;
			case WEATHER_MODERATE_SNOW:
				imageId = R.drawable.snow;
				break;
			case WEATHER_HEAVY_SNOW:
				imageId = R.drawable.snow;
				break;
			case WEATHER_FOG:
				imageId = R.drawable.fog;
				break;
		}
		return imageId;
	}

	public void getNetIp() {
		new Thread() {
			public void run() {
				String line = "";
				URL infoUrl = null;
				InputStream inStream = null;
				try {
					infoUrl = new URL("http://pv.sohu.com/cityjson?ie=utf-8");
					URLConnection connection = infoUrl.openConnection();
					HttpURLConnection httpConnection = (HttpURLConnection) connection;
					int responseCode = httpConnection.getResponseCode();
					if (responseCode == HttpURLConnection.HTTP_OK) {
						inStream = httpConnection.getInputStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "utf-8"));
						StringBuilder strber = new StringBuilder();
						// String line = "";
						while ((line = reader.readLine()) != null)
							strber.append(line + "\n");
						inStream.close();

						int begin = strber.indexOf("cname\": \"") + 8;

						int end = strber.indexOf("\"};") - 1;

						//Log.e("sai", "begin = " + begin + " end =" + end);

						line = strber.substring(begin + 1, end);
						//Log.e("sai", "strber = " + strber);
						Log.e("sai", "line = " + line);
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				// return null;
				Message message = new Message();
				message.what = 1;
				message.obj = line;
				myHandler.sendMessage(message);
			};
		}.start();
	}

	public void getWeather(final String url) {
		Log.e("sai", "getWeather url = " + url);
		new Thread() {
			public void run() {
				String line = "";
				HttpGet request = new HttpGet(url);

				HttpParams params = new BasicHttpParams();
				HttpClient httpClient = new DefaultHttpClient(params);
				try {
					HttpResponse response = httpClient.execute(request);
					if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						String content = EntityUtils.toString(response.getEntity());
						//Log.e("sai", "content = " + content);
						line = setWeatherSituation(content);
						Log.e("sai", "line = " + line);
					} else {
						//Toast.makeText(mContext, "ÍøÂç·ÃÎÊÊ§°Ü£¬Çë¼ì²éÄú»úÆ÷µÄÁªÍøÉè±¸!", Toast.LENGTH_LONG).show();
					}

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					httpClient.getConnectionManager().shutdown();
				}
				Message message = new Message();
				message.what = 2;
				message.obj = line;
				myHandler.sendMessage(message);
			};
		}.start();
	}

	public void importInitDatabase(Context context) {
		String dirPath = "/data/data/com.android.launcher3/databases";
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdir();
		}

		File dbfile = new File(dir, "db_weather.db");
		try {
			if (!dbfile.exists()) {
				dbfile.createNewFile();
			}

			InputStream is = context.getResources().openRawResource(R.raw.db_weather);
			FileOutputStream fos = new FileOutputStream(dbfile);
			byte[] buffere = new byte[is.available()];
			is.read(buffere);
			fos.write(buffere);
			is.close();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String setWeatherSituation(String content) {
		int weather_icon = 0;
		String city = "";
		int cityId;
		String temp1 = "";
		String weather1 = "";
		String img1 = "";
		try {
			JSONObject json = new JSONObject(content).getJSONObject("weatherinfo");

			city = json.getString("city");
			cityId = json.getInt("cityid");
			temp1 = json.getString("temp1");
			weather1 = json.getString("weather1");
			img1 = json.getString("img1");

			Log.e("sai", "city = " + city + "-cityId = " + cityId + "- temp1 = " + temp1 + "- weather1 = " + weather1);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return weather1 + "," + temp1 + "," + img1;
	}

	Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			//Log.e("sai", "myHandler msg = " + msg.toString());
			switch (msg.what) {
			case 1:
				String cityName = (String) msg.obj;
				Log.e("sai", "myHandler cityName = " + cityName);
				if (!cityName.equals("")) {
					Log.e("sai", "cityName != null ");
					refreshCityOrWeather(mComtext, cityName, 1);
					Log.e("sai", "refreshCityOrWeather");
					DBHelper dbHelper = new DBHelper(mComtext, "db_weather.db");
					Log.e("sai", "dbHelper");
					String cityCode = dbHelper.getCityCodeByName(cityName);
					//Log.e("sai", "cityCode=" + cityCode +".");
					if (cityCode != null) {
						Log.e("sai", "cityCode = " + cityCode);
						getWeather("http://weather.51wnl.com/weatherinfo/GetMoreWeather?cityCode=" + cityCode + "&weatherType=0");
					}
				} else {
					Log.e("sai", "cityName == null ");
				}
				break;
			case 2:
				String cityWeather = (String) msg.obj;
				refreshCityOrWeather(mComtext, cityWeather, 2);
				break;
			}
			super.handleMessage(msg);
		}
	};

	private void refreshWifiStatus(Context context, RemoteViews widget) {
		mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		int wifiStatus = mWifiManager.getWifiState();
		if (WifiManager.WIFI_STATE_ENABLED == wifiStatus || WifiManager.WIFI_STATE_ENABLING == wifiStatus) {
			widget.setImageViewResource(R.id.wifi, R.drawable.status_wifi_on);
		} else {
			widget.setImageViewResource(R.id.wifi, R.drawable.status_wifi_off);
		}
	}

	private void refreshBtStatus(Context context, RemoteViews widget) {
		bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothApapter = bluetoothManager.getAdapter();
		if(bluetoothApapter != null) {
			int btStatus = bluetoothApapter.getState();
			if (BluetoothAdapter.STATE_ON == btStatus || BluetoothAdapter.STATE_TURNING_ON == btStatus) {
				widget.setImageViewResource(R.id.bt, R.drawable.status_bt_on);
			} else {
				widget.setImageViewResource(R.id.bt, R.drawable.ic_qs_bluetooth_off_sprd);
			}
		}
	}

	private void refreshGpsStatus(Context context, RemoteViews widget, boolean isLocationEnabled) {
		if (isLocationEnabled) {
			widget.setImageViewResource(R.id.gps, R.drawable.status_gps_on);
		} else {
			widget.setImageViewResource(R.id.gps, R.drawable.status_gps_off);
		}
	}

	private void refreshDataStatus(Context context, RemoteViews widget, boolean isDataEnabled) {
		if (isDataEnabled) {
			widget.setImageViewResource(R.id.data, R.drawable.status_data_on);
		} else {
			widget.setImageViewResource(R.id.data, R.drawable.status_data_off);
		}
	}

	private void refreshAirplaneStatus(Context context, RemoteViews widget, boolean isAirplaneEnabled) {
		if (isAirplaneEnabled) {
			widget.setImageViewResource(R.id.airplane, R.drawable.status_airplane_on);
		} else {
			widget.setImageViewResource(R.id.airplane, R.drawable.status_airplane_off);
		}
	}

	public boolean isNetworkConnected(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
			if (mNetworkInfo != null) {
				return mNetworkInfo.isAvailable();
			}
		}
		return false;
	}

	private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("gcj02");//可选，默认gcj02，设置返回的定位结果坐标系，
        int span=1000;
        option.setScanSpan(0);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIgnoreKillProcess(true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        mLocationClient.setLocOption(option);
    }

	private void requestLocation(Context context) {
		mLocationClient = new LocationClient(context.getApplicationContext());
		if (mLocationClient == null) {
			Log.e("sai", "onEnabled mLocationClient == null");
		} else {
			Log.e("sai", "onEnabled mLocationClient != null");
		}
        mLocationListener = new LocationListener();
        mLocationClient.registerLocationListener(mLocationListener);
		initLocation();
		mLocationClient.start();
		mLocationClient.requestLocation();
	}

	public class LocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            //Receive Location
            StringBuffer sb = new StringBuffer(256);
			String city = "";
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation){// GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());// 单位：公里每小时
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\nheight : ");
                sb.append(location.getAltitude());// 单位：米
                sb.append("\ndirection : ");
                sb.append(location.getDirection());
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("gps定位成功");
				city = location.getCity();

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation){// 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
				city = location.getCity();
                //运营商信息
                sb.append("\noperationers : ");
                sb.append(location.getOperators());
                sb.append("\ndescribe : ");
                sb.append("网络定位成功");
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }

            Log.i("sai", sb.toString());
			Log.i("sai", "city = " + city);
			if (city != null && !city.equals("")) {
				city = city.substring(0, city.length() - 1);
				Message message = new Message();
				message.what = 1;
				message.obj = city;
				myHandler.sendMessage(message);
			}
           // mLocationClient.setEnableGpsRealTimeTransfer(true);
        }

    }

	private ContentObserver mGpsObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
            super.onChange(selfChange);
			mLocationManager = (LocationManager) mComtext.getSystemService(mComtext.LOCATION_SERVICE);
            boolean isLocationEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.i("sai", "gps ： "  + isLocationEnabled);
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mComtext);
			if (appWidgetManager != null) {
				int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(mComtext));
				for (int appWidgetId : appWidgetIds) {
					RemoteViews widget = new RemoteViews(mComtext.getPackageName(),
							R.layout.digital_appwidget);
					refreshGpsStatus(mComtext, widget, isLocationEnabled);
					appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
				}
			}
        }
    };

	private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			boolean isAirplaneEnabled = Settings.Global.getInt(mComtext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
			Log.i("sai", "airplane ： "  + isAirplaneEnabled);
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mComtext);
			if (appWidgetManager != null) {
				int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(mComtext));
				for (int appWidgetId : appWidgetIds) {
					RemoteViews widget = new RemoteViews(mComtext.getPackageName(),
							R.layout.digital_appwidget);
					refreshAirplaneStatus(mComtext, widget, isAirplaneEnabled);
					appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
				}
			}
		}
	};

	private ContentObserver mMobileDataObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			boolean isDataEnabled = true;//Settings.Global.getInt(mComtext.getContentResolver(), Settings.Global.MOBILE_DATA, 0) != 0;
			Log.i("sai", "data : " + isDataEnabled);
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mComtext);
			if (appWidgetManager != null) {
				int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(mComtext));
				for (int appWidgetId : appWidgetIds) {
					RemoteViews widget = new RemoteViews(mComtext.getPackageName(),
							R.layout.digital_appwidget);
					refreshDataStatus(mComtext, widget, isDataEnabled);
					appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
				}
			}
		}
	};
}
