/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.miadzin.shelves.util.auth;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * AuthManager keeps track of the current auth token for a user. The advantage
 * over just passing around a String is that this class can renew the auth token
 * if necessary, and it will change for all classes using this AuthManager.
 */
public class ModernAuthManager implements AuthManager {
	private static final String LOG_TAG = "ModernAuthManager";
	public static final int GET_LOGIN = 0;

	/** The activity that will handle auth result callbacks. */
	private final Activity activity;

	/** The name of the service to authorize for. */
	private final String service;

	/** The most recently fetched auth token or null if none is available. */
	private String authToken;

	private final AccountManager accountManager;

	private AuthCallback authCallback;

	private Account lastAccount;

	public static final String ACCOUNT_TYPE = "com.google";

	/**
	 * AuthManager requires many of the same parameters as
	 * {@link com.google.android.googlelogindist.GoogleLoginServiceHelper #getCredentials(Activity, int, Bundle, boolean, String, boolean)}
	 * . The activity must have a handler in {@link Activity#onActivityResult}
	 * that calls {@link #authResult(int, Intent)} if the request code is the
	 * code given here.
	 * 
	 * @param activity
	 *            An activity with a handler in
	 *            {@link Activity#onActivityResult} that calls
	 *            {@link #authResult(int, Intent)} when {@literal code} is the
	 *            request code
	 * @param service
	 *            The name of the service to authenticate as
	 */
	public ModernAuthManager(Activity activity, String service) {
		this.activity = activity;
		this.service = service;
		this.accountManager = AccountManager.get(activity);
	}

	/**
	 * Call this to do the initial login. The user will be asked to login if
	 * they haven't already. The {@link Runnable} provided will be executed when
	 * the auth token is successfully fetched.
	 * 
	 * @param runnable
	 *            A {@link Runnable} to execute when the auth token has been
	 *            successfully fetched and is available via
	 *            {@link #getAuthToken()}
	 */
	public void doLogin(AuthCallback runnable, Object o) {
		this.authCallback = runnable;
		if (!(o instanceof Account)) {
			throw new IllegalArgumentException(
					"ModernAuthManager requires an account.");
		}
		Account account = (Account) o;
		doLogin(account);
	}

	private void doLogin(Account account) {
		// Keep the account in case we need to retry.
		this.lastAccount = account;

		// NOTE: Many Samsung phones have a crashing bug in
		// AccountManager#getAuthToken(Account, String, boolean,
		// AccountManagerCallback<Bundle>)
		// so we use the other version of the method.
		// More details here:
		// http://forum.xda-developers.com/showthread.php?p=15155487
		// http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/accounts/AccountManagerService.java
		accountManager.getAuthToken(account, service, null, activity,
				new AccountManagerCallback<Bundle>() {
					public void run(AccountManagerFuture<Bundle> future) {
						try {
							authToken = future.getResult().getString(
									AccountManager.KEY_AUTHTOKEN);
							Log.i(LOG_TAG, "Got auth token");
						} catch (OperationCanceledException e) {
							Log.e(LOG_TAG, "Auth token operation Canceled", e);
						} catch (IOException e) {
							Log.e(LOG_TAG, "Auth token IO exception", e);
						} catch (AuthenticatorException e) {
							Log.e(LOG_TAG, "Authentication Failed", e);
						}

						runAuthCallback();
					}
				}, null /* handler */);
	}

	/**
	 * The {@link Activity} passed into the constructor should call this
	 * function when it gets {@link Activity#onActivityResult} with the request
	 * code passed into the constructor. The resultCode and results should come
	 * directly from the {@link Activity#onActivityResult} function. This
	 * function will return true if an auth token was successfully fetched or
	 * the process is not finished.
	 * 
	 * @param resultCode
	 *            The result code passed in to the {@link Activity}'s
	 *            {@link Activity#onActivityResult} function
	 * @param results
	 *            The data passed in to the {@link Activity}'s
	 *            {@link Activity#onActivityResult} function
	 * @return True if the auth token was fetched or we aren't done fetching the
	 *         auth token, or False if there was an error or the request was
	 *         canceled
	 */
	public void authResult(int resultCode, Intent results) {
		boolean retry = false;
		if (results == null) {
			Log.e(LOG_TAG, "No auth token!!");
		} else {
			authToken = results.getStringExtra(AccountManager.KEY_AUTHTOKEN);
			retry = results.getBooleanExtra("retry", false);
		}

		if (authToken == null && retry) {
			Log.i(LOG_TAG, "Retrying to get auth result");
			doLogin(lastAccount);
			return;
		}

		runAuthCallback();
	}

	/**
	 * Returns the current auth token. Response may be null if no valid auth
	 * token has been fetched.
	 * 
	 * @return The current auth token or null if no auth token has been fetched
	 */
	public String getAuthToken() {
		return authToken;
	}

	/**
	 * Invalidates the existing auth token and request a new one. The
	 * {@link Runnable} provided will be executed when the new auth token is
	 * successfully fetched.
	 * 
	 * @param runnable
	 *            A {@link Runnable} to execute when a new auth token is
	 *            successfully fetched
	 */
	public void invalidateAndRefresh(final AuthCallback callback) {
		this.authCallback = callback;

		activity.runOnUiThread(new Runnable() {
			public void run() {
				accountManager.invalidateAuthToken(ACCOUNT_TYPE, authToken);
				authToken = null;

				AccountChooser accountChooser = new AccountChooser();
				accountChooser.chooseAccount(activity,
						new AccountChooser.AccountHandler() {
							// @Override
							public void onAccountSelected(Account account) {
								if (account != null) {
									doLogin(account);
								} else {
									runAuthCallback();
								}
							}
						});
			}
		});
	}

	private void runAuthCallback() {
		lastAccount = null;

		if (authCallback != null) {
			(new Thread() {
				@Override
				public void run() {
					authCallback.onAuthResult(authToken != null);
					authCallback = null;
				}
			}).start();
		}
	}

	// @Override
	public Object getAccountObject(String accountName, String accountType) {
		return new Account(accountName, accountType);
	}
}
