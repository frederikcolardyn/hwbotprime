package org.hwbot.prime.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;
import org.hwbot.api.bench.dto.PersistentLoginDTO;
import org.hwbot.api.bench.dto.UserStatsDTO;
import org.hwbot.prime.MainActivity;
import org.hwbot.prime.TabFragmentAccount;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class UserRegisterTask extends AsyncTask<Void, Void, PersistentLoginDTO> {
	/**
	 *
	 */
	private final TabFragmentAccount tabFragmentAccount;

	/**
	 * @param tabFragmentAccount
	 */
	public UserRegisterTask(TabFragmentAccount tabFragmentAccount) {
		this.tabFragmentAccount = tabFragmentAccount;
	}

	@Override
	protected PersistentLoginDTO doInBackground(Void... params) {
		URL login;
		BufferedReader reader = null;
		try {
			login = new URL(BenchService.SERVER + "/api/register?userName=" + URLEncoder.encode(this.tabFragmentAccount.mRegisterUserName, "UTF8") +"&email="+URLEncoder.encode(this.tabFragmentAccount.mRegisterEmail, "UTF8")+ "&password=" + URLEncoder.encode(this.tabFragmentAccount.mRegisterPassword, "UTF8"));
			reader = new BufferedReader(new InputStreamReader(login.openStream()));
			try {
				PersistentLoginDTO loginToken = new Gson().fromJson(reader, PersistentLoginDTO.class);
				MainActivity.getActivity().storeUserStats(new UserStatsDTO());
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

	@Override
	protected void onPostExecute(final PersistentLoginDTO persistentLogin) {
		this.tabFragmentAccount.mRegisterTask = null;
		this.tabFragmentAccount.showProgress(false, false);
		if (persistentLogin != null && persistentLogin.getToken() != null) {
			SecurityService.getInstance().setCredentials(persistentLogin);
			// Log.i(this.getClass().getSimpleName(), "Logged in " + SecurityService.getInstance().getCredentials().getToken());
			MainActivity.activity.loggedIn();
		} else {
			this.tabFragmentAccount.mRegisterEmailView.setError(this.tabFragmentAccount.getString(org.hwbot.prime.R.string.error_registration_failed));
			this.tabFragmentAccount.mRegisterEmailView.requestFocus();
		}
	}

	@Override
	protected void onCancelled() {
		this.tabFragmentAccount.mAuthTask = null;
		this.tabFragmentAccount.showProgress(false, false);
	}
}