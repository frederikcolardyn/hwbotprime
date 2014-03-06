package org.hwbot.prime;

import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;
import org.hwbot.prime.tasks.UserLoginTask;
import org.hwbot.prime.util.UIUtil;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class TabFragmentAccount extends Fragment {

	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	public UserLoginTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	public String mEmail;
	public String mPassword;

	// UI references.
	public EditText mEmailView;
	public EditText mPasswordView;
	public View mLoginFormView;
	public View mLoginStatusView;
	public TextView mLoginStatusMessageView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i("CREATE", "account tab");
		View rootView = inflater.inflate(R.layout.fragment_main_account, container, false);

		// check if already logged in
		if (SecurityService.getInstance().getCredentials() != null) {
			UIUtil.setTextInView(rootView, R.id.login_form, "Logged in as " + SecurityService.getInstance().getCredentials().getUserName());
			// TODO logout
		} else {
			// Set up the login form.
			mEmail = MainActivity.activity.getIntent().getStringExtra(EXTRA_EMAIL);
			mEmailView = (EditText) rootView.findViewById(R.id.email);
			mEmailView.setText(mEmail != null ? mEmail : MainActivity.username);

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
			mLoginStatusMessageView = (TextView) rootView.findViewById(R.id.login_status_message);

			rootView.findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					attemptLogin();
				}
			});

			rootView.findViewById(R.id.twitter_sign_in_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					attemptLogin("twitter");
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
		}

		return rootView;
	}

	public void reloadLogin() {
		Log.i(this.getClass().getSimpleName(), "Credentials: " + SecurityService.getInstance().getCredentials());
	}

	/**
	 * Attempts to sign in using social media provider
	 */
	public void attemptLogin(String providerId) {
		String url = BenchService.SERVER + "/signin/" + providerId + "/remote?platform=android";
		Log.i(this.getClass().getSimpleName(), "Logging in using " + url);
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
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = mEmailView.getText().toString();
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
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
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
			showProgress(true);
			mAuthTask = new UserLoginTask(this);
			mAuthTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	public void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
				}
			});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

}
