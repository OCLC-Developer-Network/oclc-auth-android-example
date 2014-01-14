package org.oclc.mobile.authentication.android;

/*******************************************************************************
 * Copyright (c) 2014 OCLC Inc.
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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Extends a generic webView to execute an Oauth2 authentication. Returns a token and its associated parameters for use
 * in calling OCLC web services.
 *
 * @see android.webkit.WebView
 */
public class AuthenticatingWebView {

    /**
     * Parameters returned by a successful authentication.
     */
    private final HashMap<String, String> authorizationReturnParameters = new HashMap<String, String>();

    /**
     * Browser Window that hosts the authentication process.
     */
    private final WebView webView;

    /**
     * Listener for callbacks from webView.
     */
    private final AuthenticatingWebViewCallbackMethods listener;

    /**
     * Stores the webview and call back listener into class instance variables for later use.
     *
     * @param webView the WebView that is handling the authentication interaction with the user.
     * @param listener handle to the callback methods to MainActivity.java.
     */
    public AuthenticatingWebView(final WebView webView, final AuthenticatingWebViewCallbackMethods listener) {
        this.webView = webView;
        this.listener = listener;
    }

    /**
     * Sets up the WebView to make a a HTTP GET to the OCLC Authentication server to retrieve an access token.
     * <p>
     * The request URL is of this form:
     * <p>
     * {baseURL}/authorizeCode?client_id={wskey client ID}&authenticatingInstitutionId={Inst ID}
     * &contextInstitutionId={Inst ID}&redirect_uri={redirect Url}&response_type={token} &scope={scope_1 scope_2 ...}
     *
     * @param requestUrl the request URL that initiates the token request
     */
    @SuppressLint("SetJavaScriptEnabled")
    public final void makeRequest(final String requestUrl) {

        /*
         * Clear the webView, in case it is showing a previous authentication error. Make the webView visible, in case
         * the last attempt succeeded and it is hidden.
         */
        webView.loadUrl("about:blank");
        webView.setVisibility(View.VISIBLE);

        /*
         * Enable javascript in the WebView (off by default). The annotation
         */
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        /*
         * Set the callback function for the webview. Inspect the URL before loading to detect successful or failed
         * loads.
         */
        webView.setWebViewClient(new MyCustomWebViewClient());

        /*
         * Execute the token request
         */
        webView.loadUrl(requestUrl);
    }

    /**
     * Handles callbacks from the webView.
     */
    private class MyCustomWebViewClient extends WebViewClient {

        /**
         * Callback executes BEFORE the WebView makes the http request We examine the url to see if it contains the
         * redirect URI, and if so intercept it, hide the webview and display the token. Note - if something goes wrong
         * in the long OAuth2 dance, the user will end up with an error displayed on the webview. They can restart sign
         * in by pushing the [Sign In Again] button.
         *
         * @param view the WebView that executed the callback
         * @param url the URL that the WebView is about to load
         * @return returns true to permit the url to be loaded into the webview
         */
        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {

            /*
             * Is this the "redirect URI" that we are about to load? If so, parse it and don't load it. Parsing is based
             * on the # and the & characters, so make sure they are present before accepting this as a valid redirect
             * URI.
             */
            if (url.indexOf("ncipapp://user_agent_flow#") == 0 && url.indexOf("&") != -1) {

                parseRedirectURI(url);

                /*
                 * Clear the webView and hide it
                 */
                view.loadUrl("about:blank");
                webView.setVisibility(View.INVISIBLE);

                /*
                 * Display all the parameters returned with the token
                 */
                listener.displayResults(authorizationReturnParameters);

                return true;

            } else {

                /*
                 * The url we are about to load is not the "redirect URI", so load it. Note that if anything goes wrong
                 * with the authentication, the last message in the webview, and
                 * listener.displayResults(authorizationReturnParameters) will never be called.
                 */
                view.loadUrl(url);
                return true;
            }
        }

        /**
         * Callback fires when page starts to load. Used to start the Progress Dialog.
         *
         * @param view the webView referred to by this callback
         * @param url the URL that the webView started to load
         * @param favicon the favicon of the page being loaded
         */
        @Override
        public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
            listener.startProgressDialog();
        }

        /**
         * Callback fires when page finishes loading. We use it to turn off the Progress Dialog.
         *
         * @param view the webView referred to by this callback
         * @param url the URL that this page is loading
         */
        @Override
        public void onPageFinished(final WebView view, final String url) {
            listener.stopProgressDialog();
        }
    }

    /**
     * Parse a redirect url into its parameters. The string has the form
     * [redirectURI]#[param1]=[val1]&[param2]=[val2]...
     *
     * @param redirectUrl the redirect url to be parsed
     */
    private void parseRedirectURI(final String redirectUrl) {

        String[] params = redirectUrl.split("#")[1].split("&");

        for (String parameter : params) {
            if (parameter.contains("=")) {
                authorizationReturnParameters.put(parameter.split("=")[0], parameter.split("=")[1]);
            } else {
                authorizationReturnParameters.put(parameter, "");
            }
        }
    }
}
