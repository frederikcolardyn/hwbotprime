package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;

import org.hwbot.api.bench.dto.UserStatsDTO;
import org.hwbot.prime.MainActivity;
import org.hwbot.prime.R;
import org.hwbot.prime.TabFragmentAccount;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextSwitcher;

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
				Log.i(this.getClass().getSimpleName(), "Credentials: " + SecurityService.getInstance().getCredentials());
				URL url = new URL(BenchService.SERVER + "/api/user/stats?userId=" + SecurityService.getInstance().getCredentials().getUserId()
						+ (params.length > 0 && params[0] != null ? "&from=" + params[0] : ""));
				Log.i(this.getClass().getSimpleName(), "Loading user stats from: " + url);
				reader = new BufferedReader(new InputStreamReader(url.openStream()));
				UserStatsDTO userStatsDTO = new Gson().fromJson(reader, UserStatsDTO.class);
				Log.i(this.getClass().getSimpleName(), "Loaded " + userStatsDTO + " user stats.");
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
		if (TabFragmentAccount.rootView != null) {
			TextSwitcher leaguePoints, teamPoints, worldWideRank, nationalRank, teamRank;
			leaguePoints = (TextSwitcher) TabFragmentAccount.rootView.findViewById(R.id.tableRowLeagePoints);
			teamPoints = (TextSwitcher) TabFragmentAccount.rootView.findViewById(R.id.tableRowTeamPowerPoints);
			worldWideRank = (TextSwitcher) TabFragmentAccount.rootView.findViewById(R.id.tableRowWorlWideRank);
			nationalRank = (TextSwitcher) TabFragmentAccount.rootView.findViewById(R.id.tableRowNationalRank);
			teamRank = (TextSwitcher) TabFragmentAccount.rootView.findViewById(R.id.tableRowTeamRank);

			if (dto != null) {
				Log.i(this.getClass().getSimpleName(), "Updating user stats: " + dto);
				teamPoints.setText(String.format(Locale.ENGLISH, "%.2f points", dto.getTeamPowerPoints() != null ? dto.getTeamPowerPoints() : 0f));
				leaguePoints.setText(String.format(Locale.ENGLISH, "%.2f points", dto.getLeaguePoints() != null ? dto.getLeaguePoints() : 0f));
				worldWideRank.setText((dto.getLeagueRank() != null ? "#" + dto.getLeagueRank() : "not ranked"));
				nationalRank.setText((dto.getLeagueNationalRank() != null ? "#" + dto.getLeagueNationalRank() : "not ranked"));
				teamRank.setText((dto.getLeagueTeamRank() != null ? "#" + dto.getLeagueTeamRank() : "not ranked"));

				//			setRowValue(context, R.id.tableRowAchievements, dto.getAchievements() + "/" + dto.getAchievementsTotal() + " achieved");
				//			setRowValue(context, R.id.tableRowChallenges, dto.getChallengesWon() + "/" + dto.getChallengesTotal() + " won");
				//setRowValue(context, R.id.tableRowHardwareMasters, (dto.getHardwareMastersRank() != null ? "#" + dto.getHardwareMastersRank() : "not ranked"));
			} else {
				Resources resources = TabFragmentAccount.rootView.getResources();
				leaguePoints.setText(resources.getString(R.string.not_available));
				teamPoints.setText(resources.getString(R.string.not_available));
				worldWideRank.setText(resources.getString(R.string.not_available));
				nationalRank.setText(resources.getString(R.string.not_available));
				teamRank.setText(resources.getString(R.string.not_available));
			}
		}
	}

	@Override
	protected void onCancelled() {
	}
}