package org.hwbot.prime;

import java.util.Locale;

import org.hwbot.api.bench.dto.DeviceInfoDTO;
import org.hwbot.api.bench.dto.DeviceRecordDTO.RecordType;
import org.hwbot.api.bench.dto.DeviceRecordsDTO;
import org.hwbot.api.bench.dto.PersistentLoginDTO;
import org.hwbot.api.bench.dto.UserStatsDTO;
import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.api.PersistentLoginAware;
import org.hwbot.prime.api.VersionStatusAware;
import org.hwbot.prime.exception.UnsignedAppException;
import org.hwbot.prime.model.BenchmarkResult;
import org.hwbot.prime.service.AndroidHardwareService;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;
import org.hwbot.prime.tasks.HardwareDetectionTask;
import org.hwbot.prime.tasks.HardwareRecordsTask;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.gson.Gson;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener, PersistentLoginAware, NetworkStatusAware, VersionStatusAware {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	// menu reference
	public static Menu menu;

	// login form
	public static String username;
	public static CharSequence password;

	// store
	public static final String PREFS_NAME = "HWBOTPrimePreferences";
	public static final String SETTINGS_TOKEN = "token";
	public static final String SETTINGS_CREDENTIALS = "credentials";
	public static final String SETTINGS_DEVICE = "device";
	public static final String SETTINGS_STATS = "stats";
	public static final String SETTINGS_RECORDS = "deviceRecords";
	public static final String SETTINGS_RECORDS_PERSONAL = "deviceRecordsPersonal";
	public static final String SETTINGS_BEST_SCORE = "bestScore";
	public static final String COMPETE_INFO = "compete_info";
	public static final String MY_INFO = "my_info";

	// needed for bench functionality
	public static MainActivity activity;
	public static BenchService bench = PrimeBenchService.getInstance();
	private static boolean showNetworkPopup = true;

	public MainActivity() {
		MainActivity.activity = this;
	}

	public static MainActivity getActivity() {
		return activity;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent != null && intent.getData() != null) {
			Uri uri = intent.getData();
			String token = uri.getQueryParameter(SETTINGS_TOKEN);
			SecurityService.getInstance().loadToken(this, this, token);
		}

		setContentView(R.layout.activity_main);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}
		// version check
		new VersionCheckTask(this, this).execute((Void) null);

		// Restore preferences
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		restoreSettings(settings);
	}

	private void restoreSettings(SharedPreferences settings) {
		// Log.i(this.getClass().getSimpleName(), "Restoring settings.");
		username = settings.getString("username", null);
		TabFragmentAccount.mEmail = username;
		String token = settings.getString(SETTINGS_TOKEN, null);
		String device = settings.getString(SETTINGS_DEVICE, null);
		if (device != null) {
			try {
				DeviceInfoDTO deviceInfo = new Gson().fromJson(device, DeviceInfoDTO.class);
				if (deviceInfo != null) {
					AndroidHardwareService.getInstance().setDeviceInfo(deviceInfo);
				}
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), "Failed to restore device info: " + e.getMessage());
				e.printStackTrace();
			}
		}

		boolean loadToken = token != null;
		String credentials = settings.getString(SETTINGS_CREDENTIALS, null);
		if (credentials != null) {
			PersistentLoginDTO persistentLoginDTO = new Gson().fromJson(credentials, PersistentLoginDTO.class);
			if (persistentLoginDTO != null && persistentLoginDTO.getDateUntil() != null && persistentLoginDTO.getDateUntil() > System.currentTimeMillis()) {
				// Log.i(this.getClass().getSimpleName(), "Restoring credentials.");
				notifyPersistentLoginOk(persistentLoginDTO);
				loadToken = false;
			}
		}

		if (loadToken) {
			SecurityService.getInstance().loadToken(this, (PersistentLoginAware) this, token);
		}

		BenchmarkResult bestScore = getBestScore();
		if (bestScore != null && !bestScore.isSubmitted()) {
			// Log.i(this.getClass().getSimpleName(), "Best score is not yet submitted. Offline mode: " + this.isOfflineMode());
			if (!this.isOfflineMode()) {
				new SubmitResultTask(MainActivity.this, TabFragmentBench.getInstance(), bestScore.getEncryptedXml()).execute((Void) null);
			}
		}
	}

	public DeviceInfoDTO loadDeviceInfo() {
		// Log.i(this.getClass().getSimpleName(), "Loading device info.");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String info = settings.getString(SETTINGS_DEVICE, null);
		if (info != null) {
			DeviceInfoDTO fromJson = new Gson().fromJson(info, DeviceInfoDTO.class);
			return fromJson;
		} else {
			return null;
		}
	}

	public void storeDeviceInfo(DeviceInfoDTO deviceInfo) {
		if (deviceInfo != null) {
			// Log.i(this.getClass().getSimpleName(), "Storing device info with records.");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(SETTINGS_DEVICE, new Gson().toJson(deviceInfo));

			// Commit the edits!
			editor.commit();
		}
	}

	private void storeSettings(SharedPreferences settings) {
		// Log.i(this.getClass().getSimpleName(), "Storing settings.");
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("username", username);
		if (SecurityService.getInstance().isLoggedIn()) {
			editor.putString(SETTINGS_TOKEN, SecurityService.getInstance().getCredentials().getToken());
		}

		// Commit the edits!
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		MainActivity.menu = menu;
		menu.getItem(1).setVisible(SecurityService.getInstance().isLoggedIn());
		return true;
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		// Log.i(this.getClass().getSimpleName(), "Tab selected: " + tab.getText() + " #" + tab.getPosition());
		mViewPager.setCurrentItem(tab.getPosition());
		prepareTab(tab);
	}

	private void prepareTab(ActionBar.Tab tab) {
		switch (tab.getPosition()) {
		case 0:
			TabFragmentBench tabFragmentBench = (TabFragmentBench) mSectionsPagerAdapter.getItem(tab.getPosition());
			if (tabFragmentBench.isVisible()) {
				tabFragmentBench.prepareView();
			}
			break;
		case 1:
			TabFragmentCompare tabFragmentCompare = (TabFragmentCompare) mSectionsPagerAdapter.getItem(tab.getPosition());
			if (tabFragmentCompare.isVisible()) {
				tabFragmentCompare.prepareView();
			}
			break;
		case 2:
			TabFragmentAccount tabFragmentAccount = (TabFragmentAccount) mSectionsPagerAdapter.getItem(tab.getPosition());
			//			if (tabFragmentAccount.isVisible()){
			tabFragmentAccount.prepareView();
			//			}
			break;
		default:
			Log.e(this.getClass().getSimpleName(), "unkown tab");
		}
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// Log.i(this.getClass().getSimpleName(), "Unselected: " + tab.getText() + " #" + tab.getPosition());
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// Log.i(this.getClass().getSimpleName(), "Tab reselected: " + tab.getText() + " #" + tab.getPosition());
		// prepareTab(tab);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	static public class SectionsPagerAdapter extends FragmentPagerAdapter {

		TabFragmentBench tabFragmentBench;
		TabFragmentCompare tabFragmentCompare;
		TabFragmentAccount tabFragmentLoggedAccount;

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				if (tabFragmentBench == null) {
					// Log.i(this.getClass().getSimpleName(), "creating bench tab");
					tabFragmentBench = new TabFragmentBench();
				} else {
					// Log.i(this.getClass().getSimpleName(), "reusing bench tab");
				}
				return tabFragmentBench;
			case 1:
				if (tabFragmentCompare == null) {
					// Log.i(this.getClass().getSimpleName(), "creating compare tab");
					tabFragmentCompare = new TabFragmentCompare();
				} else {
					// Log.i(this.getClass().getSimpleName(), "reusing compare tab");
				}
				return tabFragmentCompare;
			case 2:
				if (tabFragmentLoggedAccount == null) {
					// Log.i(this.getClass().getSimpleName(), "creating account tab");
					tabFragmentLoggedAccount = new TabFragmentAccount();
				} else {
					// Log.i(this.getClass().getSimpleName(), "reusing account tab");
				}
				return tabFragmentLoggedAccount;
			default:
				Log.e(this.getClass().getSimpleName(), "creating default tab");
				return null;
			}
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getActivity().getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getActivity().getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getActivity().getString(R.string.title_section3).toUpperCase(l);
			}
			return null;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		storeSettings(getSharedPreferences(PREFS_NAME, 0));
	}

	@Override
	public void notifyPersistentLoginOk(PersistentLoginDTO credentials) {
		// Log.i(this.getClass().getSimpleName(), "Login OK: " + credentials);
		// notification?
		Editor edit = getSharedPreferences(PREFS_NAME, 0).edit();
		edit.putString(SETTINGS_CREDENTIALS, new Gson().toJson(credentials));
		edit.commit();
		SecurityService.getInstance().setCredentials(credentials);
		// reload hardware stats
		DeviceInfoDTO deviceInfo = loadDeviceInfo();
		if (deviceInfo != null && deviceInfo.getId() != null && loadPersonalRecords() == null) {
			new HardwareRecordsTask(this, TabFragmentBench.getInstance(), deviceInfo.getId(), credentials.getUserId()).execute((Void) null);
		}
		TabFragmentBench.getInstance().updateShowPersonalRecords();
	}

	@Override
	public void notifyPersistentLoginFailed(String message) {
		Log.w(this.getClass().getSimpleName(), "Login NOT ok: " + message);
		// notification?
		SecurityService.getInstance().setCredentials(null);
	}

	public void resetSettings() {
		Editor edit = getSharedPreferences(PREFS_NAME, 0).edit();
		edit.remove(SETTINGS_BEST_SCORE);
		edit.remove(SETTINGS_DEVICE);
		edit.remove(COMPETE_INFO);
		edit.commit();

		HardwareDetectionTask hardwareDetectionTask = new HardwareDetectionTask(MainActivity.activity, TabFragmentBench.getInstance());
		hardwareDetectionTask.execute(Build.MODEL);
	}

	public void resetToken() {
		Editor edit = getSharedPreferences(PREFS_NAME, 0).edit();
		edit.remove(SETTINGS_TOKEN);
		edit.remove(SETTINGS_CREDENTIALS);
		edit.commit();
	}

	public boolean updateBestScore() throws UnsignedAppException {
		BenchmarkResult bestScore = getBestScore();
		// Log.i("scores", "Current: " + bench.getScore() + " - best: " + bestScore);
		if (bench.getScore() != null && bestScore == null || bestScore.getScore() < bench.getScore().floatValue()) {
			try {
				byte[] dataFile = bench.getDataFile(this.getApplicationContext());
				BenchmarkResult benchmarkResult = new BenchmarkResult();
				benchmarkResult.setDate(System.currentTimeMillis());
				benchmarkResult.setEncryptedXml(dataFile);
				benchmarkResult.setMaxCpuFrequency(AndroidHardwareService.getInstance().getMaxRecordedProcessorSpeed());
				benchmarkResult.setScore(bench.getScore().floatValue());

				Editor edit = getSharedPreferences(PREFS_NAME, 0).edit();
				edit.putString(SETTINGS_BEST_SCORE, new Gson().toJson(benchmarkResult));
				edit.commit();
				// Log.i(this.getClass().getSimpleName(), "Updated best score to " + benchmarkResult);
				return true;
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), "Can not submit: " + e.getMessage());
				throw new UnsignedAppException();
			}
		} else {
			return false;
		}
	}

	public boolean markBestScoreSubmitted() {
		BenchmarkResult bestScore = getBestScore();
		if (bestScore != null) {

			if (!bestScore.isSubmitted()) {
				bestScore.setSubmitted(true);
				Editor edit = getSharedPreferences(PREFS_NAME, 0).edit();
				edit.putString(SETTINGS_BEST_SCORE, new Gson().toJson(bestScore));
				edit.commit();
				return true;
			} else {
				Log.w(this.getClass().getSimpleName(), "Best score was already submitted!");
				return false;
			}

		} else {
			return false;
		}
	}

	public BenchmarkResult getBestScore() {
		BenchmarkResult benchmarkResult = null;
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
			String bestScoreJson = sharedPreferences.getString(SETTINGS_BEST_SCORE, null);
			if (bestScoreJson != null) {
				benchmarkResult = new Gson().fromJson(bestScoreJson, BenchmarkResult.class);
			}
		} catch (Exception e) {
			Log.w(this.getClass().getSimpleName(), "Best score can not be restored: " + e.getMessage());
			Editor edit = getSharedPreferences(PREFS_NAME, 0).edit();
			edit.remove(SETTINGS_BEST_SCORE);
			edit.commit();
		}
		return benchmarkResult;
	}

	public void setOfflineMode(boolean offlineMode) {
		SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
		Editor edit = sharedPreferences.edit();
		edit.putBoolean("offline_mode", offlineMode);
		edit.commit();
		Log.w(this.getClass().getSimpleName(), "Saved offline mode: " + offlineMode);
	}

	public boolean isOfflineMode() {
		SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
		boolean offlineMode = sharedPreferences.getBoolean("offline_mode", false);
		Log.w(this.getClass().getSimpleName(), "Loaded offline mode: " + offlineMode);
		return offlineMode;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Log.i(this.getClass().getSimpleName(), "Preparing options menu, logged in " + SecurityService.getInstance().isLoggedIn());
		menu.getItem(1).setVisible(SecurityService.getInstance().isLoggedIn());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_logout:
			logout();
			return true;
		case R.id.action_reset:
			// Log.i(this.getClass().getSimpleName(), "Reset best score.");
			resetSettings();
			return true;
		case R.id.offlinemode:
			// Log.i(this.getClass().getSimpleName(), "Offline mode " + item.getClass().getSimpleName());
			updateOfflineMode();
			return true;
		case R.id.action_settings:
			new AboutDialog().show(getSupportFragmentManager(), "about");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void updateOfflineMode() {
		MenuItem menuItem = (MenuItem) MainActivity.menu.findItem(R.id.offlinemode);
		// Log.i(this.getClass().getSimpleName(), "Offline mode: " + menuItem);
		if (menuItem != null) {
			//		CheckBox checkBox = (CheckBox) rootView.findViewById(R.id.offlineModeButton);
			menuItem.setChecked(this.isOfflineMode());
			//		// register click
			menuItem.setOnMenuItemClickListener(offlineModeListener);
		}
	}

	MenuItem.OnMenuItemClickListener offlineModeListener = new MenuItem.OnMenuItemClickListener() {
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			try {
				if (item.isChecked()) {
					item.setChecked(false);
				} else {
					item.setChecked(true);
				}
				boolean isChecked = item.isChecked();
				MainActivity.this.setOfflineMode(isChecked);
				if (!isChecked) {
					BenchmarkResult bestScore2 = MainActivity.this.getBestScore();
					if (bestScore2 != null && !bestScore2.isSubmitted()) {
						TabFragmentBench fragment = TabFragmentBench.getInstance();
						if (fragment != null) {
							toast("Uploading best score...");
							new SubmitResultTask(MainActivity.this, fragment, bestScore2.getEncryptedXml()).execute((Void) null);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(this.getClass().getName(), "error offline check: " + e.getMessage());
			}
			return true;
		}
	};

	/**
	 * offline due to network
	 */
	public void setOffline() {
		MainActivity.this.setOfflineMode(true);
		// Log.i(this.getClass().getSimpleName(), "Offline mode: " + MainActivity.this.findViewById(R.id.offlinemode));
		CheckBox checkBox = (CheckBox) MainActivity.this.findViewById(R.id.offlinemode);
		checkBox.setChecked(true);
	}

	private void logout() {
		// Log.i(this.getClass().getSimpleName(), "Logging out.");
		SecurityService.getInstance().setCredentials(null);
		resetToken();
		TabFragmentAccount tabFragmentAccount = (TabFragmentAccount) mSectionsPagerAdapter.getItem(2);
		tabFragmentAccount.prepareView();
	}

	public void loggedIn() {
		// Log.i(this.getClass().getSimpleName(), "Logged in.");
		TabFragmentAccount tabFragmentAccount = (TabFragmentAccount) mSectionsPagerAdapter.getItem(2);
		tabFragmentAccount.prepareView();
	}

	public void showNetworkPopupOnce() {
		if (isShowNetworkPopup()) {
			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// Log.i(this.getClass().getSimpleName(), "Show network popup.");
					boolean noLooper = Looper.myLooper() == null;
					if (noLooper) {
						Looper.prepare();
						Looper.loop();
					}
					Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setMessage(R.string.no_network);
					builder.setTitle(R.string.no_network_title);
					builder.setPositiveButton(R.string.no_network_btn, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// Log.i(NoNetworkDialog.class.getSimpleName(), "Okay...");
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
					if (noLooper) {
						Looper.myLooper().quit();
					}
				};
			});
		} else {
			// Log.i(this.getClass().getSimpleName(), "Network popup already shown.");
			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// Log.i(this.getClass().getSimpleName(), "Show network popup.");
					boolean noLooper = Looper.myLooper() == null;
					if (noLooper) {
						Looper.prepare();
						Looper.loop();
					}
					toast("No network connection.");
					if (noLooper) {
						Looper.myLooper().quit();
					}
				};
			});
		}
	}

	public boolean isShowNetworkPopup() {
		if (showNetworkPopup) {
			showNetworkPopup = false;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void showNewVersionPopup(final String version, final String url, final boolean updateRequired) {
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// Log.i(this.getClass().getSimpleName(), "A new version is available, update " + (updateRequired ? "REQUIRED." : "NOT required."));
				boolean noLooper = Looper.myLooper() == null;
				if (noLooper) {
					Looper.prepare();
					Looper.loop();
				}
				Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle(R.string.new_version_title);
				Context baseContext = MainActivity.this.getBaseContext();
				if (updateRequired) {
					builder.setMessage(baseContext.getString(R.string.new_version_required_text, PrimeBenchService.getInstance().version, version));
				} else {
					builder.setMessage(baseContext.getString(R.string.new_version_recommended_text, PrimeBenchService.getInstance().version, version));
				}
				builder.setPositiveButton(R.string.new_version_upgrade, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// Log.i(NoNetworkDialog.class.getSimpleName(), "Opening " + url + " for update.");
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
						MainActivity.this.startActivity(intent);
					}
				});
				builder.setNegativeButton(R.string.new_version_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// Log.i(NoNetworkDialog.class.getSimpleName(), "Do not update...");
					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
				if (noLooper) {
					Looper.myLooper().quit();
				}
			};
		});
	}

	public void hideCompeteInfo(View view) {
		markSeen(COMPETE_INFO);
		MainActivity.activity.findViewById(R.id.competeBox).setVisibility(View.GONE);
	}

	public void goToSignIn(View view) {
		markSeen(COMPETE_INFO);
		MainActivity.activity.findViewById(R.id.competeBox).setVisibility(View.GONE);
		getActionBar().setSelectedNavigationItem(2);
	}

	public void markSeen(String key) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(key, true);

		// Commit the edits!
		editor.commit();
	}

	public boolean isSeen(String key) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		return settings.getBoolean(key, false);
	}

	public Drawable getAnonymousIcon() {
		return getResources().getDrawable(R.drawable.ic_action_person);
	}

	public void storeUserStats(UserStatsDTO dto) {
		if (dto != null) {
			// Log.i(this.getClass().getSimpleName(), "Storing user stats.");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(SETTINGS_STATS, new Gson().toJson(dto));

			// Commit the edits!
			editor.commit();
		}
	}

	public UserStatsDTO loadUserStats() {
		// Log.i(this.getClass().getSimpleName(), "Loading user stats info.");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String info = settings.getString(SETTINGS_STATS, null);
		if (info != null) {
			UserStatsDTO fromJson = new Gson().fromJson(info, UserStatsDTO.class);
			return fromJson;
		} else {
			return null;
		}
	}

	public void storePersonalRecords(DeviceRecordsDTO dto) {
		if (dto != null && !dto.getRecords().isEmpty() && dto.getRecords().get(RecordType.best_device) != null) {
			// Log.i(this.getClass().getSimpleName(), "Storing device personal records stats.");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(SETTINGS_RECORDS_PERSONAL, new Gson().toJson(dto));

			// Commit the edits!
			editor.commit();
		}
	}

	public void storeRecords(DeviceRecordsDTO dto) {
		if (dto != null && !dto.getRecords().isEmpty() && dto.getRecords().get(RecordType.best_device) != null) {
			// Log.i(this.getClass().getSimpleName(), "Storing device records stats.");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(SETTINGS_RECORDS, new Gson().toJson(dto));

			// Commit the edits!
			editor.commit();
		}
	}

	public DeviceRecordsDTO loadPersonalRecords() {
		// Log.i(this.getClass().getSimpleName(), "Loading personal records info.");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String info = settings.getString(SETTINGS_RECORDS_PERSONAL, null);
		if (info != null) {
			DeviceRecordsDTO fromJson = new Gson().fromJson(info, DeviceRecordsDTO.class);
			return fromJson;
		} else {
			return null;
		}
	}

	public DeviceRecordsDTO loadRecords() {
		// Log.i(this.getClass().getSimpleName(), "Loading records info.");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String info = settings.getString(SETTINGS_RECORDS, null);
		if (info != null) {
			DeviceRecordsDTO fromJson = new Gson().fromJson(info, DeviceRecordsDTO.class);
			return fromJson;
		} else {
			return null;
		}
	}

	public static void toast(String message) {
		Toast toast = Toast.makeText(MainActivity.getActivity(), message, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 30);
		toast.show();
	}
}
