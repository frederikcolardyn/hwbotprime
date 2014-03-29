package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import org.hwbot.prime.MainActivity;
import org.hwbot.prime.TabFragmentAccount;
import org.hwbot.prime.model.PersistentLogin;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class UserLoginTask extends AsyncTask<Void, Void, PersistentLogin> {
	/**
	 * 
	 */
	private final TabFragmentAccount tabFragmentAccount;

	/**
	 * @param tabFragmentAccount
	 */
	public UserLoginTask(TabFragmentAccount tabFragmentAccount) {
		this.tabFragmentAccount = tabFragmentAccount;
	}

	@Override
	protected PersistentLogin doInBackground(Void... params) {
		URL login;
		try {
			login = new URL(BenchService.SERVER + "/api/login?username=" + this.tabFragmentAccount.mEmail + "&password=" + this.tabFragmentAccount.mPassword);
			BufferedReader in = new BufferedReader(new InputStreamReader(login.openStream()));
			JsonReader reader = null;
			try {
				reader = new JsonReader(in);
				PersistentLogin loginToken = LoginTokenTask.readLoginToken(reader);
				return loginToken;
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), "Login not succesful: " + e.getMessage());
				e.printStackTrace();
			} finally {
				reader.close();
				in.close();
			}
		} catch (UnknownHostException e) {
			MainActivity.activity.showNetworkPopupOnce();
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Failed to authenticate: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(final PersistentLogin persistentLogin) {
		this.tabFragmentAccount.mAuthTask = null;
		this.tabFragmentAccount.showProgress(false);
		if (persistentLogin != null && persistentLogin.getToken() != null) {
			SecurityService.getInstance().setCredentials(persistentLogin);
			Log.i(this.getClass().getSimpleName(), "Logged in " + SecurityService.getInstance().getCredentials().getToken());
			MainActivity.activity.loggedIn();
		} else {
			this.tabFragmentAccount.mPasswordView.setError(this.tabFragmentAccount.getString(org.hwbot.prime.R.string.error_incorrect_password));
			this.tabFragmentAccount.mPasswordView.requestFocus();
		}
	}

	@Override
	protected void onCancelled() {
		this.tabFragmentAccount.mAuthTask = null;
		this.tabFragmentAccount.showProgress(false);
	}
}