package org.hwbot.prime;

import java.util.Locale;

import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.api.PersistentLoginAware;
import org.hwbot.prime.model.PersistentLogin;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener, PersistentLoginAware, NetworkStatusAware {

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

	// login form
	public static String username;
	public static CharSequence password;

	// store
	public static final String PREFS_NAME = "HWBOTPrimePreferences";

	// needed for bench functionality
	public static MainActivity activity;
	public static BenchService bench = BenchService.getInstance();
	private static boolean showNetworkPopup = true;

	public MainActivity() {
		MainActivity.activity = this;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Log.i("CREATE", "main activity");

		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent != null && intent.getData() != null) {
			Uri uri = intent.getData();
			String token = uri.getQueryParameter("token");
			Log.i("INTENT", "got token: " + token);
			SecurityService.getInstance().loadToken(this, this, token);
		} else {
			Log.i("INTENT", "no intent data: " + intent);
		}

		// Restore preferences
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		restoreSettings(settings);

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
	}

	private void restoreSettings(SharedPreferences settings) {
		Log.i(this.getClass().getSimpleName(), "Restoring settings.");
		username = settings.getString("username", null);
		TabFragmentAccount.mEmail = username;
		String token = settings.getString("token", null);

		SecurityService.getInstance().loadToken(this, (PersistentLoginAware) this, token);
	}

	private void storeSettings(SharedPreferences settings) {
		Log.i(this.getClass().getSimpleName(), "Storing settings.");
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("username", username);
		if (SecurityService.getInstance().getCredentials() != null) {
			editor.putString("token", SecurityService.getInstance().getCredentials().getToken());
		}

		// Commit the edits!
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		Log.i(this.getClass().getSimpleName(), "Tab selected: " + tab.getText() + " #" + tab.getPosition());
		mViewPager.setCurrentItem(tab.getPosition());
		prepareTab(tab);
	}

	private void prepareTab(ActionBar.Tab tab) {
		switch (tab.getPosition()) {
		case 0:
			// nothing to prepare
			break;
		case 1:
			TabFragmentCompare tabFragmentCompare = (TabFragmentCompare) mSectionsPagerAdapter.getItem(tab.getPosition());
			tabFragmentCompare.loadActiveRanking();
			break;
		case 2:
			TabFragmentAccount tabFragmentAccount = (TabFragmentAccount) mSectionsPagerAdapter.getItem(tab.getPosition());
			tabFragmentAccount.prepareView();
			break;
		default:
			Log.e(this.getClass().getSimpleName(), "unkown tab");
		}
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		Log.i(this.getClass().getSimpleName(), "Unselected: " + tab.getText() + " #" + tab.getPosition());
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		Log.i(this.getClass().getSimpleName(), "Tab reselected: " + tab.getText() + " #" + tab.getPosition());
		prepareTab(tab);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

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
					Log.i(this.getClass().getSimpleName(), "creating bench tab");
					tabFragmentBench = new TabFragmentBench();
				} else {
					Log.i(this.getClass().getSimpleName(), "reusing bench tab");
				}
				return tabFragmentBench;
			case 1:
				if (tabFragmentCompare == null) {
					Log.i(this.getClass().getSimpleName(), "creating compare tab");
					tabFragmentCompare = new TabFragmentCompare();
				} else {
					Log.i(this.getClass().getSimpleName(), "reusing compare tab");
				}
				return tabFragmentCompare;
			case 2:
				if (tabFragmentLoggedAccount == null) {
					Log.i(this.getClass().getSimpleName(), "creating account tab");
					tabFragmentLoggedAccount = new TabFragmentAccount();
				} else {
					Log.i(this.getClass().getSimpleName(), "reusing account tab");
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
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
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
	public void notifyPersistentLoginOk(PersistentLogin credentials) {
		Log.i(this.getClass().getSimpleName(), "Login OK: " + credentials);
		// notification?
		SecurityService.getInstance().setCredentials(credentials);
	}

	@Override
	public void notifyPersistentLoginFailed(String message) {
		Log.w(this.getClass().getSimpleName(), "Login NOT ok: " + message);
		// notification?
		SecurityService.getInstance().setCredentials(null);
	}

	public void resetBestScore() {
		Editor edit = getSharedPreferences(PREFS_NAME, 0).edit();
		edit.remove("bestScore");
		edit.commit();
	}

	public void resetToken() {
		Editor edit = getSharedPreferences(PREFS_NAME, 0).edit();
		edit.remove("token");
		edit.commit();
	}

	public boolean updateBestScore(Number score) {
		float bestScore = getBestScore();
		if (score.floatValue() > bestScore) {
			Editor edit = getSharedPreferences(PREFS_NAME, 0).edit();
			edit.putFloat("bestScore", score.floatValue());
			edit.commit();
			return true;
		} else {
			return false;
		}
	}

	public float getBestScore() {
		SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
		float bestScore = sharedPreferences.getFloat("bestScore", 0f);
		return bestScore;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.i(this.getClass().getSimpleName(), "Preparing options menu.");
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
			Log.i(this.getClass().getSimpleName(), "Reset best score.");
			resetBestScore();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void logout() {
		Log.i(this.getClass().getSimpleName(), "Logging out.");
		SecurityService.getInstance().setCredentials(null);
		resetToken();
		TabFragmentAccount tabFragmentAccount = (TabFragmentAccount) mSectionsPagerAdapter.getItem(2);
		tabFragmentAccount.prepareView();
	}

	public void loggedIn() {
		Log.i(this.getClass().getSimpleName(), "Logged in.");
		TabFragmentAccount tabFragmentAccount = (TabFragmentAccount) mSectionsPagerAdapter.getItem(2);
		tabFragmentAccount.prepareView();
	}

	public void showNetworkPopupOnce() {
		if (isShowNetworkPopup()) {
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.no_network);
			builder.setPositiveButton(R.string.no_network_btn, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Log.i(NoNetworkDialog.class.getSimpleName(), "okay...");
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
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
}
