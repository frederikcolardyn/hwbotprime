package org.hwbot.opengl.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import org.hwbot.api.bench.dto.PersistentLoginDTO;
import org.hwbot.opengl.MainActivity;
import org.hwbot.opengl.TabFragmentAccount;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class UserLoginTask extends AsyncTask<Void, Void, PersistentLoginDTO> {
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

	// @Override
	protected PersistentLoginDTO doInBackground(Void... params) {
		URL login;
		BufferedReader reader = null;
		try {
			login = new URL(BenchService.SERVER + "/api/login?username=" + this.tabFragmentAccount.mEmail + "&password=" + this.tabFragmentAccount.mPassword);
			reader = new BufferedReader(new InputStreamReader(login.openStream()));
			try {
				PersistentLoginDTO loginToken = new Gson().fromJson(reader, PersistentLoginDTO.class);
				return loginToken;
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), "Login not succesful: " + e.getMessage());
				e.printStackTrace();
			} finally {
				if (reader != null) {
					reader.close();
				}
			}
		} catch (UnknownHostException e) {
			MainActivity.activity.showNetworkPopupOnce();
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Failed to authenticate: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	// @Override
	protected void onPostExecute(final PersistentLoginDTO persistentLogin) {
		this.tabFragmentAccount.mAuthTask = null;
		this.tabFragmentAccount.showProgress(false);
		if (persistentLogin != null && persistentLogin.getToken() != null) {
			SecurityService.getInstance().setCredentials(persistentLogin);
			// Log.i(this.getClass().getSimpleName(), "Logged in " + SecurityService.getInstance().getCredentials().getToken());
			MainActivity.activity.loggedIn();
		} else {
			this.tabFragmentAccount.mPasswordView.setError(this.tabFragmentAccount.getString(org.hwbot.opengl.R.string.error_incorrect_password));
			this.tabFragmentAccount.mPasswordView.requestFocus();
		}
	}

	// @Override
	protected void onCancelled() {
		this.tabFragmentAccount.mAuthTask = null;
		this.tabFragmentAccount.showProgress(false);
	}
}