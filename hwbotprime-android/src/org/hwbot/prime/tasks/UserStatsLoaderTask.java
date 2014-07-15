package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import org.hwbot.api.bench.dto.UserStatsDTO;
import org.hwbot.prime.MainActivity;
import org.hwbot.prime.TabFragmentAccount;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class UserStatsLoaderTask extends AsyncTask<Void, Void, UserStatsDTO> {

	/**
	 * @param tabFragmentAccount
	 */
	public UserStatsLoaderTask(TabFragmentAccount tabFragmentAccount) {
	}

	@Override
	protected UserStatsDTO doInBackground(Void... params) {
		BufferedReader reader = null;
		try {
			if (SecurityService.getInstance().isLoggedIn()) {
				// Log.i(this.getClass().getSimpleName(), "Credentials: " + SecurityService.getInstance().getCredentials());
				URL url = new URL(BenchService.SERVER + "/api/user/stats?userId=" + SecurityService.getInstance().getCredentials().getUserId()
						+ (params.length > 0 && params[0] != null ? "&from=" + params[0] : ""));
				// Log.i(this.getClass().getSimpleName(), "Loading user stats from: " + url);
				reader = new BufferedReader(new InputStreamReader(url.openStream()));
				UserStatsDTO userStatsDTO = new Gson().fromJson(reader, UserStatsDTO.class);
//				 Log.i(this.getClass().getSimpleName(), "Loaded " + userStatsDTO + " user stats.");
				return userStatsDTO;
			} else {
				Log.w(this.getClass().getSimpleName(), "Not logged in.");
				return null;
			}
		} catch (UnknownHostException e) {
			MainActivity.activity.showNetworkPopupOnce();
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Failed to load user stats: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(UserStatsDTO dto) {
		
		// store
		MainActivity.getActivity().storeUserStats(dto);
		
		if (TabFragmentAccount.rootView != null) {
			TabFragmentAccount.getInstance().updateUserStats(dto);
		}
	}

	@Override
	protected void onCancelled() {
	}
}