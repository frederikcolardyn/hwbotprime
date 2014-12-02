package org.hwbot.prime;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.graphics.Color;
import android.os.AsyncTask;
import org.apache.commons.lang.StringUtils;
import org.hwbot.api.bench.dto.PersistentLoginDTO;
import org.hwbot.api.bench.dto.UserStatsDTO;
import org.hwbot.prime.api.CommentObserver;
import org.hwbot.prime.api.VoteObserver;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;
import org.hwbot.prime.tasks.*;
import org.hwbot.prime.util.AndroidUtil;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

public class TabFragmentAccount extends Fragment implements VoteObserver, CommentObserver {

	public static final int POSITION = 2;

	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	public UserLoginTask mAuthTask = null;
	public UserRegisterTask mRegisterTask = null;
	public UserUserNameAvailableTask mRegisterPreCheckTask = null;

	// Values for email and password at the time of the login attempt.
	public static String mUserName;
	public static String mEmail;
	public String mPassword;

	public static String mRegisterUserName;
	public static String mRegisterEmail;
	public String mRegisterPassword;
	public Boolean mRegisterUserNameAvailable;

	// login
	public EditText mUserNameView;
	public EditText mPasswordView;

	// register
	public EditText mRegisterEmailView;
	public EditText mRegisterUserNameView;
	public EditText mRegisterPasswordView;

	public View mLoginFormView;
	public View mLoginStatusView;
	public View mRegisterView;
	public TextView mLoginStatusMessageView;
	public static View rootView;

	public TextSwitcher leaguePoints, teamPoints, worldWideRank, nationalRank, teamRank;
	public static TabFragmentAccount fragment;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Log.i(this.getClass().getSimpleName(), "Creating account tab.");
		// Set up the login form.
		rootView = inflater.inflate(R.layout.fragment_main_account, container, false);

		fragment = this;

		// login stuff
		mUserName = MainActivity.activity.getIntent().getStringExtra(EXTRA_EMAIL);
		mUserNameView = (EditText) rootView.findViewById(R.id.email);
		mUserNameView.setText(mUserName != null ? mUserName : MainActivity.username);
		mPasswordView = (EditText) rootView.findViewById(R.id.password);
		if (MainActivity.password != null) {
			mPasswordView.setText(MainActivity.password);
		}
		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});
		mLoginFormView = rootView.findViewById(R.id.login_form);
		mLoginStatusView = rootView.findViewById(R.id.login_status);
		mRegisterView = rootView.findViewById(R.id.register);
		mLoginStatusMessageView = (TextView) rootView.findViewById(R.id.login_status_message);

		// register stuff
		mRegisterEmailView = (EditText) rootView.findViewById(R.id.register_email);
		mRegisterPasswordView = (EditText) rootView.findViewById(R.id.register_password);
		mRegisterUserNameView = (EditText) rootView.findViewById(R.id.register_login);

		// action listeners
		rootView.findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});

		rootView.findViewById(R.id.register_flow_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				prepareViewAsRegister();
			}
		});

		rootView.findViewById(R.id.register_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptRegister();
			}
		});

		rootView.findViewById(R.id.twitter_sign_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin("twitter");
			}
		});

		rootView.findViewById(R.id.register_login).setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				String text = ((EditText) v).getText().toString();
				Log.i("register", "focus: " + hasFocus + " text: " + text);
				if (!hasFocus){
					attemptRegisterPreCheck(text);
				}
			}

		});

		rootView.findViewById(R.id.facebook_sign_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin("facebook");
			}
		});

		rootView.findViewById(R.id.google_sign_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin("google");
			}
		});

		leaguePoints = (TextSwitcher) rootView.findViewById(R.id.tableRowLeagePoints);
		teamPoints = (TextSwitcher) rootView.findViewById(R.id.tableRowTeamPowerPoints);
		worldWideRank = (TextSwitcher) rootView.findViewById(R.id.tableRowWorlWideRank);
		nationalRank = (TextSwitcher) rootView.findViewById(R.id.tableRowNationalRank);
		teamRank = (TextSwitcher) rootView.findViewById(R.id.tableRowTeamRank);

		ViewFactory ViewFactory = new ViewFactory() {
			public View makeView() {
				TextView myText = new TextView(MainActivity.activity, null, R.style.ValueScoreBig);
				myText.setEllipsize(TruncateAt.START);
				myText.setGravity(Gravity.CENTER_HORIZONTAL);
				myText.setTextAppearance(MainActivity.activity.getApplicationContext(), R.style.ValueScoreBig);
				return myText;
			}
		};
		leaguePoints.setFactory(ViewFactory);
		teamPoints.setFactory(ViewFactory);
		worldWideRank.setFactory(ViewFactory);
		nationalRank.setFactory(ViewFactory);
		teamRank.setFactory(ViewFactory);

		leaguePoints.setText(getResources().getString(R.string.not_available));
		teamPoints.setText(getResources().getString(R.string.not_available));
		worldWideRank.setText(getResources().getString(R.string.not_available));
		nationalRank.setText(getResources().getString(R.string.not_available));
		teamRank.setText(getResources().getString(R.string.not_available));

		// prepareView();
		return rootView;
	}

	public void prepareView() {
		// Log.i(this.getClass().getSimpleName(), "Security credentials:" + SecurityService.getInstance().getCredentials());
		if (SecurityService.getInstance().isLoggedIn()) {
			prepareViewAsLoggedIn();
		} else {
			prepareViewAsLoggedOut();
		}
	}

	public void prepareViewAsLoggedOut() {
		// Log.i(this.getClass().getSimpleName(), "prepareViewAsLoggedOut");
		if (rootView != null) {
			View loginView = rootView.findViewById(R.id.login_form);
			View loggedInView = rootView.findViewById(R.id.logged_in);
			View registerView = rootView.findViewById(R.id.register);
			loginView.setVisibility(ScrollView.VISIBLE);
			loggedInView.setVisibility(ScrollView.GONE);
			registerView.setVisibility(ScrollView.GONE);
		} else {
			Log.e(this.getClass().getSimpleName(), "rootview null");
		}
	}

	public void prepareViewAsRegister() {
		Log.i(this.getClass().getSimpleName(), "prepareViewAsRegister");
		if (rootView != null) {
			View loginView = rootView.findViewById(R.id.login_form);
			View loggedInView = rootView.findViewById(R.id.logged_in);
			View registerView = rootView.findViewById(R.id.register);
			loginView.setVisibility(ScrollView.GONE);
			loggedInView.setVisibility(ScrollView.GONE);
			registerView.setVisibility(ScrollView.VISIBLE);
		} else {
			Log.e(this.getClass().getSimpleName(), "rootview null");
		}
	}

	public void prepareViewAsLoggedIn() {
		// Log.i(this.getClass().getSimpleName(), "prepareViewAsLoggedIn");
		if (rootView != null) {
			View loginView = rootView.findViewById(R.id.login_form);
			View loggedInView = rootView.findViewById(R.id.logged_in);
			View registerView = rootView.findViewById(R.id.register);
			loggedInView.setVisibility(ScrollView.VISIBLE);
			loginView.setVisibility(ScrollView.INVISIBLE);
			loginView.setVisibility(ScrollView.GONE);
			registerView.setVisibility(ScrollView.GONE);

			PersistentLoginDTO credentials = SecurityService.getInstance().getCredentials();

			LinearLayout personalInfoBox = (LinearLayout) rootView.findViewById(R.id.personalInfoBox);
			personalInfoBox.removeAllViews();

			TextView user = new TextView(rootView.getContext());
			user.setText(credentials.getUserName());
			user.setTextAppearance(rootView.getContext(), R.style.leaderboardTextHuge);
			user.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

			TextView team = new TextView(rootView.getContext());
			team.setText((StringUtils.isEmpty(credentials.getTeamName()) ? getResources().getString(R.string.no_team) : credentials.getTeamName()));
			team.setTextAppearance(rootView.getContext(), R.style.leaderboardText);
			team.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

			TextView league = new TextView(rootView.getContext());
			league.setText((StringUtils.isEmpty(credentials.getLeague()) ? getResources().getString(R.string.no_league) : credentials.getLeague()));
			league.setTextAppearance(rootView.getContext(), R.style.leaderboardText);
			league.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

			personalInfoBox.addView(user);
			personalInfoBox.addView(team);
			personalInfoBox.addView(league);

			ImageView avatar = (ImageView) rootView.findViewById(R.id.avatarIcon);
			// Log.i(ImageLoaderTask.class.getSimpleName(), "credentials.getAvatar(): " + credentials.getAvatar());
			if (StringUtils.isNotEmpty(credentials.getAvatar())) {
				try {
					String url;
					if (credentials.getAvatar().startsWith("http")) {
						url = credentials.getAvatar();
					} else {
						url = BenchService.SERVER + credentials.getAvatar();
					}
					// Log.i(ImageLoaderTask.class.getSimpleName(), "url: " + url);
					avatar.setScaleType(ScaleType.FIT_XY);
					avatar.setTag(url);
					new ImageLoaderTask(MainActivity.getActivity().getAnonymousIcon()).execute(avatar);
				} catch (Exception e) {
					Log.w(this.getClass().getSimpleName(), "Failed to load image: " + e.getMessage());
					e.printStackTrace();
					avatar.setImageDrawable(MainActivity.getActivity().getAnonymousIcon());
				}
			} else {
				// avatar.setImageDrawable(MainActivity.getActivity().getAnonymousIcon());
			}

			ViewGroup notificationContainer = (ViewGroup) TabFragmentAccount.rootView.findViewById(R.id.notifications);

			UserStatsDTO userStatsDTO = MainActivity.getActivity().loadUserStats();
			Log.i("account tab", "user stats: " + userStatsDTO);
			if (userStatsDTO != null) {
				updateUserStats(userStatsDTO);
			} else {
				updateUserStats(new UserStatsDTO());
			}

			notificationContainer.removeAllViews();
			TextView noComments = new TextView(rootView.getContext());
			noComments.setText(R.string.loading);
			LinearLayout.LayoutParams authorLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			authorLayout.topMargin = AndroidUtil.dpToPx(20);
			authorLayout.bottomMargin = AndroidUtil.dpToPx(20);
			authorLayout.gravity = Gravity.CENTER;
			noComments.setLayoutParams(authorLayout);
			noComments.setGravity(Gravity.CENTER);
			notificationContainer.addView(noComments);

			if (userStatsDTO == null) {
				UserStatsLoaderTask userStatsLoaderTask = new UserStatsLoaderTask(this);
				userStatsLoaderTask.execute((Void) null);
			}

			NotificationLoaderTask notificationLoaderTask = new NotificationLoaderTask(this);
			notificationLoaderTask.execute((String) null);
		} else {
			Log.e(this.getClass().getSimpleName(), "rootview null");
		}
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		// Log.i(this.getClass().getSimpleName(), "View state restored.");
		super.onViewStateRestored(savedInstanceState);
	}

	/**
	 * Attempts to sign in using social media provider
	 */
	public void attemptLogin(String providerId) {
		String url = BenchService.SERVER + "/signin/" + providerId + "/remote?platform=android";
		// Log.i(this.getClass().getSimpleName(), "Logging in using " + url);
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mUserNameView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = mUserNameView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mEmail)) {
			mUserNameView.setError(getString(R.string.error_field_required));
			focusView = mUserNameView;
			cancel = true;
		}
		//		else if (!mEmail.contains("@")) {
		//			mEmailView.setError(getString(R.string.error_invalid_email));
		//			focusView = mEmailView;
		//			cancel = true;
		//		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true, true);
			mAuthTask = new UserLoginTask(this);
			mAuthTask.execute((Void) null);
		}
	}

	public boolean attemptRegisterPreCheck(String partialLogin) {
		mRegisterPreCheckTask = new UserUserNameAvailableTask(this);
		AsyncTask<String, Void, Boolean> available = mRegisterPreCheckTask.execute(partialLogin);
		try {
			Boolean nameAvailable = available.get(10l, TimeUnit.SECONDS);
			if (Boolean.FALSE.equals(nameAvailable)){
				if (nameAvailable){
					mRegisterUserNameView.setError(null);
				} else {
					mRegisterUserNameView.setError(this.getString(R.string.error_username_taken));
				}
			}
			return nameAvailable;
		} catch (Exception e) {
			Log.e("register", "error: " + e);
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptRegister() {
		if (mRegisterTask != null) {
			return;
		}

		if (!attemptRegisterPreCheck(mRegisterUserNameView.getText().toString())){
			return;
		}

		// Reset errors.

		mRegisterUserNameView.setError(null);
		mRegisterEmailView.setError(null);
		mRegisterPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mRegisterEmail = mRegisterEmailView.getText().toString();
		mRegisterUserName = mRegisterUserNameView.getText().toString();
		mRegisterPassword = mRegisterPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mRegisterPassword)) {
			mRegisterPasswordView.setError(getString(R.string.error_field_required));
			focusView = mRegisterPasswordView;
			cancel = true;
		} else if (mRegisterPassword.length() < 4) {
			mRegisterPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mRegisterPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mRegisterEmail)) {
			mRegisterEmailView.setError(getString(R.string.error_field_required));
			focusView = mRegisterEmailView;
			cancel = true;
		}

		if (!mRegisterEmail.contains("@")) {
			mRegisterEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mRegisterEmailView;
			cancel = true;
		} else if (mRegisterEmail != null && mRegisterUserName == null || mRegisterUserName.length() > 0){
			mRegisterUserName = StringUtils.substringBefore(mRegisterEmail, "@");
		}

		if (mRegisterUserName == null || mRegisterUserName.length() < 3){
			mRegisterUserNameView.setError(getString(R.string.error_invalid_login));
			focusView = mRegisterUserNameView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.register_progress_signing_in);
			showProgress(true, false);
			mRegisterTask = new UserRegisterTask(this);
			mRegisterTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	public void showProgress(final boolean show, final boolean login) {
		mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
		mLoginFormView.setVisibility(show || !login ? View.GONE : View.VISIBLE);
		mRegisterView.setVisibility(show || login ? View.GONE : View.VISIBLE);
	}

	@Override
	public void notifyVoteFailed(final View view) {
		MainActivity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setAlpha(0.2f);
			}
		});
	}

	@Override
	public void notifyCommentFailed(final View view) {
		MainActivity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setAlpha(0.2f);
				view.setClickable(false);
			}
		});
	}

	@Override
	public void notifyCommentSucceeded(final View view, final TextSwitcher textSwitcher) {
		MainActivity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setAlpha(1.0f);
				TextView currentView = (TextView) textSwitcher.getCurrentView();
				int currentComments = Integer.parseInt(String.valueOf(currentView.getText()));
				textSwitcher.setText(String.valueOf(++currentComments));
			}
		});
	}

	@Override
	public void notifyVoteSucceeded(final View view, final TextSwitcher textSwitcher) {
		MainActivity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setAlpha(1.0f);
				view.setClickable(false);
				TextView currentView = (TextView) textSwitcher.getCurrentView();
				int currentVotes = Integer.parseInt(String.valueOf(currentView.getText()));
				textSwitcher.setText(String.valueOf(++currentVotes));
			}
		});
	}

	public static TabFragmentAccount getInstance() {
		if (fragment == null) {
			fragment = (TabFragmentAccount) MainActivity.getActivity().mSectionsPagerAdapter.getItem(TabFragmentAccount.POSITION);
		}
		return fragment;
	}

	public void updateUserStats(UserStatsDTO dto) {
		TextSwitcher leaguePoints, teamPoints, worldWideRank, nationalRank, teamRank;
		leaguePoints = (TextSwitcher) rootView.findViewById(R.id.tableRowLeagePoints);
		teamPoints = (TextSwitcher) rootView.findViewById(R.id.tableRowTeamPowerPoints);
		worldWideRank = (TextSwitcher) rootView.findViewById(R.id.tableRowWorlWideRank);
		nationalRank = (TextSwitcher) rootView.findViewById(R.id.tableRowNationalRank);
		teamRank = (TextSwitcher) rootView.findViewById(R.id.tableRowTeamRank);

		if (dto != null) {
			// Log.i(this.getClass().getSimpleName(), "Updating user stats: " + dto);
			teamPoints.setText(String.format(Locale.ENGLISH, "%.2f points", dto.getTeamPowerPoints() != null ? dto.getTeamPowerPoints() : 0f));
			leaguePoints.setText(String.format(Locale.ENGLISH, "%.2f points", dto.getLeaguePoints() != null ? dto.getLeaguePoints() : 0f));
			worldWideRank.setText((dto.getLeagueRank() != null ? "#" + dto.getLeagueRank() : "not ranked"));
			nationalRank.setText((dto.getLeagueNationalRank() != null ? "#" + dto.getLeagueNationalRank() : "not ranked"));
			teamRank.setText((dto.getLeagueTeamRank() != null ? "#" + dto.getLeagueTeamRank() : "not ranked"));

			//			setRowValue(context, R.id.tableRowAchievements, dto.getAchievements() + "/" + dto.getAchievementsTotal() + " achieved");
			//			setRowValue(context, R.id.tableRowChallenges, dto.getChallengesWon() + "/" + dto.getChallengesTotal() + " won");
			//setRowValue(context, R.id.tableRowHardwareMasters, (dto.getHardwareMastersRank() != null ? "#" + dto.getHardwareMastersRank() : "not ranked"));
		} else {
			Resources resources = rootView.getResources();
			leaguePoints.setText(resources.getString(R.string.not_available));
			teamPoints.setText(resources.getString(R.string.not_available));
			worldWideRank.setText(resources.getString(R.string.not_available));
			nationalRank.setText(resources.getString(R.string.not_available));
			teamRank.setText(resources.getString(R.string.not_available));
		}
	}

}
