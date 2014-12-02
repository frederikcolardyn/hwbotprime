package org.hwbot.prime.tasks;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;
import org.hwbot.api.bench.dto.PersistentLoginDTO;
import org.hwbot.prime.MainActivity;
import org.hwbot.prime.R;
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
public class UserUserNameAvailableTask extends AsyncTask<String, Void, Boolean> {
	/**
	 *
	 */
	private final TabFragmentAccount tabFragmentAccount;

	/**
	 * @param tabFragmentAccount
	 */
	public UserUserNameAvailableTask(TabFragmentAccount tabFragmentAccount) {
		this.tabFragmentAccount = tabFragmentAccount;
	}

	@Override
	protected Boolean doInBackground(String... param) {
		URL login;
		BufferedReader reader = null;
		try {
			login = new URL(BenchService.SERVER + "/api/register/available?username=" + URLEncoder.encode(param[0], "UTF8"));
			Log.i("register", "Checking availability: " + login);
			reader = new BufferedReader(new InputStreamReader(login.openStream()));
			try {
				this.tabFragmentAccount.mRegisterUserNameAvailable = Boolean.parseBoolean(reader.readLine());
				return this.tabFragmentAccount.mRegisterUserNameAvailable;
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), "Available check not succesful: " + e.getMessage());
				e.printStackTrace();
			} finally {
				if (reader != null) {
					reader.close();
				}
			}
		} catch (UnknownHostException e) {
			MainActivity.activity.showNetworkPopupOnce();
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Available check to authenticate: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

}