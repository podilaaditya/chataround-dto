package com.service.chataround;

import java.util.Calendar;

import org.springframework.util.StringUtils;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.google.android.gcm.GCMRegistrar;
import com.service.chataround.dto.chat.ChatAroundDto;
import com.service.chataround.fragment.ChatAroundListFragment;
import com.service.chataround.listener.MyLocationListener;
import com.service.chataround.task.ChatAroundTask;
import com.service.chataround.util.ChatConstants;
import com.service.chataround.util.PushUtils;

public class ChatAroundActivity extends Activity {
	private Dialog settingsDialog;
	private EditText nickName;
	private EditText moodText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat_around);

		Fragment frg = Fragment.instantiate(this,
				ChatAroundListFragment.class.getName());
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.add(R.id.frameLayoutId, frg);
		ft.addToBackStack(null);
		ft.commit();

		if (isOnline()) {
			registerToCloud();
		}
		final SharedPreferences settings = getSharedPreferences(
				ChatConstants.PREFS_NAME, 0);
		
		String nick = settings.getString(ChatConstants.USER_NICKNAME, "");
		
		if(!StringUtils.hasText(nick)) {
				settingsDialog();
		}		
		
	}

	@Override
	public void onResume() {
		super.onResume();
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		MyLocationListener locationListener = new MyLocationListener(
				locationManager, getApplicationContext());
		locationListener.start();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_chat_around, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.menu_settings:
			settingsDialog();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void settingsDialog() {
		final SharedPreferences settings = getSharedPreferences(
				ChatConstants.PREFS_NAME, 0);

		settingsDialog = new Dialog(ChatAroundActivity.this);
		settingsDialog.setContentView(R.layout.settingsdialog);
		settingsDialog.setTitle(R.string.menu_settings);
		settingsDialog.setCancelable(true);

		nickName = (EditText) settingsDialog
				.findViewById(R.id.nicknameTextView);
		moodText = (EditText) settingsDialog.findViewById(R.id.moodTextView);
		
		String nick = settings.getString(ChatConstants.USER_NICKNAME, "");
		String mood = settings.getString(ChatConstants.USER_MOOD, "");
		
		nickName.setText(nick);
		moodText.setText(mood);

		Switch switchButton = (Switch) settingsDialog
				.findViewById(R.id.switchNotifId);
		Boolean isNotifications = settings.getBoolean(
				ChatConstants.USER_NOTIFICATIONS, true);
		switchButton.setChecked(isNotifications);
		switchButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Switch notif = (Switch) v;
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(ChatConstants.USER_NOTIFICATIONS,
						notif.isChecked());
				editor.commit();
			}
		});

		Switch switchButtonSound = (Switch) settingsDialog
				.findViewById(R.id.switchNotifSoundId);
		Boolean isSound = settings.getBoolean(ChatConstants.USER_STAY_ONLINE,
				true);
		switchButtonSound.setChecked(isSound);
		switchButtonSound.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Switch sound = (Switch) v;
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(ChatConstants.USER_STAY_ONLINE,
						sound.isChecked());
				editor.commit();
			}
		});

		Button button = (Button) settingsDialog.findViewById(R.id.saveSettings);
		button.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (StringUtils.hasText(nickName.getText().toString().trim())) {
					String nickname = nickName.getText().toString().trim();
					String mood = moodText.getText().toString().trim();
					// We need an Editor object to make preference changes.
					// All objects are from android.context.Context
					SharedPreferences.Editor editor = settings.edit();
					editor.putString(ChatConstants.USER_NICKNAME, nickname);
					editor.putString(ChatConstants.USER_MOOD, mood);
					editor.commit();
					settingsDialog.hide();
				}
			}
		});

		Button closeButton = (Button) settingsDialog
				.findViewById(R.id.closeSettings);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				settingsDialog.hide();
			}
		});

		settingsDialog.show();
	}

	private void registerToCloud() {
		checkNotNull(ChatConstants.SERVER_URL, "SERVER_URL");
		checkNotNull(ChatConstants.SENDER_ID, "SENDER_ID");
		// Make sure the device has the proper dependencies.
		GCMRegistrar.checkDevice(this);
		// Make sure the manifest was properly set - comment out this line
		// while developing the app, then uncomment it when it's ready.
		GCMRegistrar.checkManifest(this);

		final String regId = GCMRegistrar.getRegistrationId(this);
		if (regId.equals("")) {
			// Automatically registers application on startup.
			GCMRegistrar.register(this, ChatConstants.SENDER_ID);
		} else {
			// Device is already registered on GCM, check server.
			if (GCMRegistrar.isRegisteredOnServer(this)) {
				// Skips registration.
				// No need to display anything cause it�s background thingy
				// mDisplay.append(getString(R.string.already_registered) +
				// "\n");
			} else {
				// Try to register again, but not in the UI thread.
				// It's also necessary to cancel the thread onDestroy(),
				// hence the use of AsyncTask instead of a raw thread.
				final Context context = this;
				ChatAroundDto dto = new ChatAroundDto();
				dto.setDeviceId(regId);
				dto.setAppId(PushUtils.APP_ID);
				dto.setTime(Calendar.getInstance().getTime());

				new ChatAroundTask(context,null).execute(dto,
						ChatConstants.SERVER_URL + ChatConstants.REGISTER_URL);
			}
		}
	}

	private void checkNotNull(Object reference, String name) {
		if (reference == null) {
			throw new NullPointerException(getString(R.string.error_config,
					name));
		}
	}

	public void finishTask(ChatAroundDto result) {
		if (result != null
				&& result.getResponse().equals(
						"push.server.operation.unregister.ok")) {
			GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);

		} else if (result != null
				&& result.getResponse().equals(
						"push.server.operation.register.ok")) {
			GCMRegistrar.setRegisteredOnServer(this, true);
			Log.d("HttpCall",
					"Call to google made!!!!! call2WebApp="
							+ result.getResponse() + "]");
		} else if (result != null
				&& result.getResponse().equals(
						"push.server.operation.register.ok")) {
			GCMRegistrar.setRegisteredOnServer(this, true);
			Log.d("HttpCall",
					"Call to google made!!!!! call2WebApp="
							+ result.getResponse() + "]");

		}
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}
}