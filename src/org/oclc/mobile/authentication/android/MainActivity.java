package org.oclc.mobile.authentication.android;

/*******************************************************************************
 * Copyright (c) 2013 OCLC Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 ******************************************************************************/

import java.util.HashMap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Displays the view for the application and handles UI interaction. The
 * following resources are loaded into this Activity:
 * <ul>
 * <li>res/layout/activity_main.xml - the view objects and their layout</li>
 * <li>/res/values/strings.xml - any strings that are displayed</li>
 * <li>/res/values/authentication.xml - list of strings containing
 * authentication parameters</li>
 * </ul>
 * <p>
 * This class calls an instance of AuthenticatingWebView to handle the actual
 * sign in process with the OCLC Authentication servers. Call back methods are
 * defined by interfaces in AuthenticatingWebViewCallbackMethods, so that the
 * AuthenticatingWebView can turn an activity indicator on and off, and return
 * the authentication results for display.
 * <p>
 * The request and result parameters are as follows:
 * <p>
 * Request Parameters - stored in prop. file res/values/authentication.xml:
 * <ul>
 * <li>authenticatingServerBaseUrl</li>
 * <li>client_id</li>
 * <li>authenticatingInstitutionId</li>
 * <li>contextInstitutionId</li>
 * <li>redirect_uri</li>
 * <li>response_type</li>
 * <li>scope</li>
 * </ul>
 * Returned Parameters - stored in HashMap authorizationReturnParameters:
 * <ul>
 * <li>accessToken</li>
 * <li>principalID</li>
 * <li>principalIDNS</li>
 * <li>context_institution_id</li>
 * <li>token_type</li>
 * <li>expires_in</li>
 * <li>expires_at</li>
 * </ul>
 * <p>
 * The Cookie Manager, myCookieManager, gives access to this app's cookies so
 * that we can clear them when restarting the sign-in activity from scratch.
 *
 * @see android.app.Activity
 */
public class MainActivity extends Activity implements
AuthenticatingWebViewCallbackMethods {

    /**
     * Multiplier to convert seconds to milliseconds
     */
    private static final int SECONDS_TO_MILLISECONDS = 1000;

    /**
     * An extension that is passed a webview and uses it to handle
     * authentication
     */
    private AuthenticatingWebView authenticatingWebView;

    /**
     * Holds the context of MainActivity so it can be passed to the WebView. The
     * WebView uses that context to call back to the MainActivity to
     * <ul>
     * <li>Start the activity spinner.</li>
     * <li>Stop the activity spinner.</li>
     * <li>Display the authentication results.</li>
     * </ul>
     */
    private Context myContext;

    /**
     * Handle for managing this application's cookies
     */
    private CookieManager myCookieManager;

    /**
     * Timer activated when authentication token is received to decrement the
     * seconds remaining until the authentication token expires.
     */
    private CountDownTimer tokenCountDownTimer;

    /**
     * A progress dialog to indicate to the user that the app is waiting for an
     * http response
     */
    private ProgressDialog myProgressDialog;

    /**
     * The webview used for authentication
     */
    private WebView webView;

    /**
     * The full URL of the Access Token request
     */
    private String requestUrl;

    /**
     * This method initializes the class and only fires once - when the app
     * loads into memory. Once an app is initialized, it stays in the run state
     * until the client runs out of memory or shuts off, which could be for
     * days. This method does not fire when the app returns from the background
     * to the foreground.
     * <p>
     * The method loads the layout from the xml file, instantiates an instance
     * of the webview. A WebView is an embedded Chrome browser with no user
     * controls, which we will use to handle OAuth2 authentication. Initially
     * the webView is invisible. It is made visible to facilitate sign in, and
     * hidden after a token is received so that the token can be displayed.
     *
     * @param savedInstanceState state information for the app
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected final void onCreate(final Bundle savedInstanceState) {

        /**
         * Set the context required for the progress dialog
         */
        myContext = this;

        /**
         * Required for all Android apps to pass the savedInstanceState to the
         * parent
         */
        super.onCreate(savedInstanceState);

        /**
         * Loads the view elements from the xml file. R.layout.activity_main
         * refers to res/layout/activity_main.xml
         */
        setContentView(R.layout.activity_main);

        /**
         * Get a handle to the webView whose position and size is defined in
         * activity_main.xml
         */
        webView = (WebView) findViewById(R.id.webView);

        /**
         * Get a handle to the CookieManager, which is global for this app, and
         * use it to enable cookies.
         */
        myCookieManager = CookieManager.getInstance();
        myCookieManager.setAcceptCookie(true);

        /**
         * Build the request url by getting the request parameters from
         * res/values/authentication.xml.
         */
        requestUrl = new StringBuffer()
        .append(getString(R.string.authenticatingServerBaseUrl))
        .append("/authorizeCode?client_id=")
        .append(getString(R.string.wskey))
        .append("&authenticatingInstitutionId=")
        .append(getString(R.string.authenticatingInstitutionId))
        .append("&contextInstitutionId=")
        .append(getString(R.string.contextInstitutionId))
        .append("&redirect_uri=")
        .append(getString(R.string.redirectUrl))
        .append("&response_type=")
        .append(getString(R.string.responseType)).append("&scope=")
        .append(getString(R.string.scopes)).toString();

        if (getString(R.string.wskey).equals("")) {
            // If the wskey is blank, then the user probably forgot to set the
            // parameters in authentication.xml
            LinearLayout resultLayout = (LinearLayout) findViewById(R.id.resultLayout);
            resultLayout.setVisibility(View.VISIBLE);

            ((TextView) findViewById(R.id.access_token))
            .setText("You must set the authentication parameters in the authentication.xml properties file.");
        } else {
            /**
             * Create the AuthenticatingWebView, a custom WebView, to make the url
             * request. We also pass this class's context so that the
             * AuthenticatingWebView can execute callbacks.
             */
            authenticatingWebView = new AuthenticatingWebView(webView, this);
            authenticatingWebView.makeRequest(requestUrl);
        }
    }

    /**
     * Boilerplate code required by Android to display any menu xml that may
     * exist in res/menu.
     *
     * @param menu the menu associated with this activity
     * @return returns true to create the menu
     */
    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Standard form callback for the [Clear Cookies] button, whose properties
     * are described in res/layout/activity_main.xml.
     *
     * @param view [Clear Cookies] button's view
     */
    public final void clearCookies(final View view) {

        /**
         * Removes the cookies associated with this app only - browser cookies
         * and other app's cookies are not affected.
         */
        myCookieManager.removeAllCookie();
    }

    /**
     * Standard form callback for the [Sign In Again] button, whose properties
     * are described in res/layout/activity_main.xml.
     *
     * @param view [Sign In Again] button's view
     */
    public final void signInAgain(final View view) {

        /**
         * Clear the token count down timer if it is running and set the timer
         * text to expired.
         */
        if (tokenCountDownTimer != null) {
            tokenCountDownTimer.cancel();
            tokenCountDownTimer = null;
            ((TextView) findViewById(R.id.timeRemainingTextView))
            .setText(getString(R.string.time_remaining_expired));
        }

        /**
         *  Hide the text result views.
         */
        LinearLayout resultLayout = (LinearLayout) findViewById(R.id.resultLayout);
        resultLayout.setVisibility(View.INVISIBLE);

        /**
         * Make another request.
         */
        authenticatingWebView.makeRequest(requestUrl);
    }

    /**
     * Display a progress indicator while authenticating. Implements a callback
     * function called by AuthenticatingWebViewCallbackMethods
     */
    @Override
    public final void startProgressDialog() {

        /**
         *  Create a progressDialog if it does not exist.
         */
        if (myProgressDialog == null) {
            myProgressDialog = new ProgressDialog(myContext);
            myProgressDialog.setTitle(getString(R.string.authenticating));
            myProgressDialog.setMessage(getString(R.string.please_wait));
            myProgressDialog.setCancelable(false);
            myProgressDialog.setIndeterminate(true);
        }

        /**
         *  Show the progress dialog.
         */
        myProgressDialog.show();
    }

    /**
     * Stop and destroy a progress indicator (if it exists). Implements a
     * callback function called by AuthenticatingWebViewCallbackMethods.
     */
    @Override
    public final void stopProgressDialog() {
        myProgressDialog.hide();
    }

    /**
     * Display the results by extracting the values from the Hash Map and
     * inserting them into the TextViews which are defined in
     * res/layout/activity_main.xml. Implements a callback function called by
     * AuthenticatingWebViewCallbackMethods.
     *
     * @param authorizationReturnParameters A list of return params and values
     */
    @Override
    public final void displayResults(
            final HashMap<String, String> authorizationReturnParameters) {

        /**
         * Make the text result views visible. Each result parameter's textview
         * is grouped into a LinearLayout.
         */
        LinearLayout resultLayout = (LinearLayout) findViewById(R.id.resultLayout);
        resultLayout.setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.access_token))
        .setText(authorizationReturnParameters.get("access_token"));
        ((TextView) findViewById(R.id.principalID))
        .setText(authorizationReturnParameters.get("principalID"));
        ((TextView) findViewById(R.id.principalIDNS))
        .setText(authorizationReturnParameters.get("principalIDNS"));
        ((TextView) findViewById(R.id.context_institution_id))
        .setText(authorizationReturnParameters
                .get("context_institution_id"));
        ((TextView) findViewById(R.id.token_type))
        .setText(authorizationReturnParameters.get("token_type"));
        ((TextView) findViewById(R.id.expires_in))
        .setText(authorizationReturnParameters.get("expires_in"));
        ((TextView) findViewById(R.id.expires_at))
        .setText(authorizationReturnParameters.get("expires_at"));

        /**
         *  Check if the token CountDownTimer, and cancel it if it does.
         */
        if (tokenCountDownTimer != null) {
            tokenCountDownTimer.cancel();
        }

        /**
         * Start a new token count down timer based on the time remaining
         * returned with the token (time remaining is in seconds).
         */
        if (authorizationReturnParameters.get("expires_in") != null) {
            tokenCountDownTimer = new CountDownTimer(
                    Integer.parseInt(authorizationReturnParameters
                            .get("expires_in")) * SECONDS_TO_MILLISECONDS,
                            SECONDS_TO_MILLISECONDS) {

                /* Callback fires every 1000 ms. */
                @Override
                public void onTick(final long millisUntilFinished) {
                    ((TextView) findViewById(R.id.timeRemainingTextView))
                    .setText(getString(R.string.time_remaining)
                            + millisUntilFinished
                            / SECONDS_TO_MILLISECONDS);
                }

                /**
                 *  Callback fires when timer counts down to zero.
                 */
                @Override
                public void onFinish() {
                    ((TextView) findViewById(R.id.timeRemainingTextView))
                    .setText(getString(R.string.time_remaining_expired));
                }
            }.start();
        } else {
            /**
             * If the "expires_in" parameter is null, then something has gone
             * wrong during authentication.
             */
            ((TextView) findViewById(R.id.timeRemainingTextView))
            .setText(getString(R.string.invalid_authentication_request));
        }
    }
}
