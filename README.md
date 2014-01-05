# OCLC Android Authentication Example

This sample Android Application demonstrates how to authenticate an OCLC user to obtain an access token. 

A use case would be to allow a library patron to view their checked out items, place holds or renew materials on an iPhone or iPad.

A tutorial explaining this application's code in detail <a>is available here</a>.

## Installation

```bash
$ git clone https://github.com/OCLC-Developer-Network/oclc-auth-android.git
```

Open the library using <a href="http://www.eclipse.org/downloads/">Eclipse</a>.

## Usage

Set the authentication parameters in the **authentication.xml** file.

```xml
<resources>
    <string name="authenticatingServerBaseUrl">https://authn.sd00.worldcat.org/oauth2</string>
    <string name="wskey"></string>
    <string name="authenticatingInstitutionId"></string>
    <string name="contextInstitutionId"></string>
    <string name="redirectUrl"></string>
    <string name="scopes"></string>
    <string name="responseType">token</string>
</resources>
```

* **wskey** - the public key that identifies the client
* **authenticatingInstitution** - the institution that is responsible for authenticating the user.
* **contextInstitution** – the institution’s whose data the client is requesting access to.
* **redirectUrl** – the url the authorization server should redirect the user to after login. For mobile flow, this should be a **non-http** reference unique to your application, such as **oclcApp://user_agent_flow**.
* **scopes** – the service(s) that the client is requesting access to. Multiple scopes are separated by a space. Note that adding "refresh_token" to the list of scopes causes a refresh token to be issued.
* **responseType** - should be "token"

To request or manage web service keys, use <a href="https://www.worldcat.org/config/">OCLC Service Configuration</a>.

To learn more about authentication and access tokens, see <a href="http://www.oclc.org/developer/platform/user-agent-or-mobile-pattern">this article on Mobile Flow</a> from the <a href="http://oclc.org/developer/">OCLC Developer Network</a>.

## Example 1 - Get an access token

<ol>
<li>The sample app will assemble this request and send it to the OCLC server.

<pre>
https://authn.sd00.worldcat.org/oauth2/authorizeCode?
client_id={a valid wskey}
&authenticatingInstitutionId=128807
&contextInstitutionId=128807
&redirect_uri=oclcApp%3A%2F%2Fuser_agent_flow
&response_type=token
&scope=WMS_NCIP
</pre>
</li>

<li>The client will be prompted to sign in with a userid and password.</li>
<li>A service authorization page will list the scopes requested and ask the user if they will allow it.</li>
<li>The client will receive an access token.
<pre>
{
    "access_token" = "tk_U13DrzOHW8eep3jvwIpNX2rDcfuhvetNbrFm";
    "context_institution_id" = 128807;
    "expires_at" = "2014-01-05%2011:57:26Z";
    "expires_in" = 1199;
    principalID = "{your principalID}";
    principalIDNS = "{your principalIDNS}";
    "token_type" = bearer;
}
</pre>
</li>
<li>This access token can now be used to make requests against the scoped OCLC services until it expires, typically in 20 minutes.</li>
</ol>

## Example 2 - Get a refresh token

Currently a refresh token can only get an authentication token by making an HMAC request. However, you cannot make an HMAC request from a mobile device because that would require storing the key and the secret in the device, which is unsafe and insecure. So for now, it is not recommended to use refresh tokens for mobile devices to access OCLC services.

<ol>
<li>Add "refresh_token" to the list of scopes (each scope is separated with a single space) in the **authenticationList.plist** file. The app will then make a request similar to this:

<pre>
https://authn.sd00.worldcat.org/oauth2/authorizeCode?
client_id={a valid wskey}
&authenticatingInstitutionId=128807
&contextInstitutionId=128807
&redirect_uri=oclcApp%3A%2F%2Fuser_agent_flow
&response_type=token
&scope=WMS_NCIP%20refresh_token
</pre>
</li>
<li>The client will be prompted to sign in with a userid and password.</li>
<li>A service authorization page will list the scopes requested and ask the user if they will allow it.</li>
<li>The client will receive an access token.
<pre>
{
    "access_token" = "tk_nd4GoLXjFcAabig2AJzOMpzhPkI2LFZtbLD6";
    "context_institution_id" = 128807;
    "expires_at" = "2014-01-05%2012:07:09Z";
    "expires_in" = 1199;
    principalID = "{your principalID}";
    principalIDNS = "{your principalIDNS}";
    "refresh_token" = "rt_nucaPASHXXZ3L2F6vNYucr2xudlKfnc8v8si";
    "refresh_token_expires_at" = "2014-01-12%2011:47:09Z";
    "refresh_token_expires_in" = 604799;
    "token_type" = bearer;
}
</pre>
</li>
<li>The refresh token can then be used to request an access token, as described in the <a href="http://www.oclc.org/developer/news/authentication-and-authorization-refresh-tokens">developer network documentation</a>.</li>
