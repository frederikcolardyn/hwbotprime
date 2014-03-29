package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;

import org.hwbot.prime.MainActivity;
import org.hwbot.prime.R;
import org.hwbot.prime.TabFragmentAccount;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import android.content.Context;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class UserStatsLoaderTask extends AsyncTask<Void, Void, UserStatsDTO> {
	/**
	 * 
	 */
	private final TabFragmentAccount tabFragmentAccount;

	/**
	 * @param tabFragmentAccount
	 */
	public UserStatsLoaderTask(TabFragmentAccount tabFragmentAccount) {
		this.tabFragmentAccount = tabFragmentAccount;
	}

	@Override
	protected UserStatsDTO doInBackground(Void... params) {
		JsonReader reader = null;
		try {
			if (SecurityService.getInstance().isLoggedIn()) {
				Log.i(this.getClass().getSimpleName(), "Credentials: " + SecurityService.getInstance().getCredentials());
				URL url = new URL(BenchService.SERVER + "/api/user/stats?userId=" + SecurityService.getInstance().getCredentials().getUserId()
						+ (params.length > 0 && params[0] != null ? "&from=" + params[0] : ""));
				Log.i(this.getClass().getSimpleName(), "Loading user stats from: " + url);
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				reader = new JsonReader(in);
				UserStatsDTO userStatsDTO = readUserStatsDTO(reader);
				Log.i(this.getClass().getSimpleName(), "Loaded " + userStatsDTO + " user stats.");
				return userStatsDTO;
			} else {
				return null;
			}
		} catch (UnknownHostException e) {
			MainActivity.activity.showNetworkPopupOnce();
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Failed to load notifications: " + e.getMessage());
			e.printStackTrace();

			ViewGroup notificationTable = (ViewGroup) tabFragmentAccount.getView().findViewById(R.id.notifications);
			Context context = tabFragmentAccount.getView().getContext();
			// TableRow row = new TableRow(context);
			TextView message = new TextView(context);
			message.setText("Failed to load notifications: " + e.getMessage());
			// row.addView(message);
			notificationTable.addView(message);
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

	private static UserStatsDTO readUserStatsDTO(JsonReader reader) {
		try {
			reader.beginObject();
			UserStatsDTO dto = new UserStatsDTO();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("achievements")) {
					dto.setAchievements(reader.nextInt());
				} else if (name.equals("achievementsTotal")) {
					dto.setAchievementsTotal(reader.nextInt());
				} else if (name.equals("challengesTotal")) {
					dto.setChallengesTotal(reader.nextInt());
				} else if (name.equals("challengesWon")) {
					dto.setChallengesWon(reader.nextInt());
				} else if (name.equals("leagueNationalRank")) {
					dto.setLeagueNationalRank(reader.nextInt());
				} else if (name.equals("leaguePoints")) {
					dto.setLeaguePoints((float) reader.nextDouble());
				} else if (name.equals("leagueRank")) {
					dto.setLeagueRank(reader.nextInt());
				} else if (name.equals("leagueTeamRank")) {
					dto.setLeagueTeamRank(reader.nextInt());
				} else if (name.equals("hardwareMasterRank")) {
					dto.setHardwareMastersRank(reader.nextInt());
				} else if (name.equals("teamPowerPoints")) {
					dto.setTeamPowerPoints((float) reader.nextDouble());
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
			return dto;
		} catch (IOException e) {
			Log.e(LoginTokenTask.class.getName(), "error loading rankings: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected void onPostExecute(UserStatsDTO dto) {
		if (dto != null && TabFragmentAccount.rootView != null) {
			Context context = TabFragmentAccount.rootView.getContext();

			setRowValue(context, R.id.tableRowAchievements, dto.getAchievements() + "/" + dto.getAchievementsTotal() + " achieved");
			setRowValue(context, R.id.tableRowChallenges, dto.getChallengesWon() + "/" + dto.getChallengesTotal() + " won");
			setRowValue(context, R.id.tableRowWorlWideRank, (dto.getLeagueRank() != null ? "#" + dto.getLeagueRank() : "not ranked"));
			setRowValue(context, R.id.tableRowNationalRank, (dto.getLeagueNationalRank() != null ? "#" + dto.getLeagueNationalRank() : "not ranked"));
			setRowValue(context, R.id.tableRowTeamRank, "#" + (dto.getLeagueTeamRank() != null ? "#" + dto.getLeagueTeamRank() : "not ranked"));
			setRowValue(context, R.id.tableRowHardwareMasters, (dto.getHardwareMastersRank() != null ? "#" + dto.getHardwareMastersRank() : "not ranked"));

			setRowValue(context, R.id.tableRowTeamPowerPoints,
					String.format(Locale.ENGLISH, "%.2f points", dto.getTeamPowerPoints() != null ? dto.getTeamPowerPoints() : 0f));
			setRowValue(context, R.id.tableRowLeagePoints,
					String.format(Locale.ENGLISH, "%.2f points", dto.getLeaguePoints() != null ? dto.getLeaguePoints() : 0f));
		} else {
			Log.e(this.getClass().getSimpleName(), "Can not show notifications: " + tabFragmentAccount.getView());
		}
	}

	private void setRowValue(Context context, int rowId, Object text) {
		TableRow row = (TableRow) TabFragmentAccount.rootView.findViewById(rowId);
		row.removeViewAt(1);
		TextView textView = new TextView(context);
		textView.setText(String.valueOf(text));
		textView.setPadding(130, 5, 5, 5);
		// textView.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		row.addView(textView, 1);
	}

	@Override
	protected void onCancelled() {
	}
}