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

/**
 * This interface defines the callback functions that AuthenticatingWebView.java will execute in MainActivity.java,
 * since the MainActivity handles displaying results on the screen, and AuthenticatingWebView makes asynchronous http
 * requests.
 */
public interface AuthenticatingWebViewCallbackMethods {
    /**
     * Method is called when it is time to display the progress spinner, for example when loading on the web view.
     */
    void startProgressDialog();

    /**
     * Method is called when it is time to hide the progress spinner
     */
    void stopProgressDialog();

    /**
     * Method is called when authentication is complete
     *
     * @param authorizationReturnParameters The params returned with the token
     */
    void displayResults(HashMap<String, String> authorizationReturnParameters);
}
