package com.smartthings.network;

import com.smartthings.utility.STException;
import com.smartthings.utility.STExceptionType;
import com.smartthings.utility.STFileAccess;
import com.smartthings.utility.STLogger;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class STRest {

  //================================================================================
  // Public
  //================================================================================

  public static String GET(String relativeUrl, HashMap<String, String> params, String accessToken) throws STException {
    return makeGETRequest(relativeUrl, params, accessToken);
  }

  public static String PUT(String relativeUrl, HashMap<String, String> params, String accessToken) throws STException {
    return makeRequest("PUT", relativeUrl, params, accessToken);
  }

  public static String POST(String relativeUrl, HashMap<String, String> params, String accessToken) throws STException {
    return makeRequest("POST", relativeUrl, params, accessToken);
  }

  public static String DELETE(String relativeUrl, HashMap<String, String> params, String accessToken) throws STException {
    return makeRequest("DELETE", relativeUrl, params, accessToken);
  }

  public static void asyncGET(String relativeUrl, HashMap<String, String> params, String accessToken) {
    new STRestRunnable("GET", relativeUrl, params, accessToken).run();
  }

  public static void asyncPUT(String relativeUrl, HashMap<String, String> params, String accessToken) {
    new STRestRunnable("PUT", relativeUrl, params, accessToken).run();
  }

  public static void asyncPOST(String relativeUrl, HashMap<String, String> params, String accessToken) {
    new STRestRunnable("POST", relativeUrl, params, accessToken).run();
  }

  public static void asyncDELETE(String relativeUrl, HashMap<String, String> params, String accessToken) {
    new STRestRunnable("DELETE", relativeUrl, params, accessToken).run();
  }

  public static void authenticate() {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return;
    }


    try {

      Desktop.getDesktop().browse(new URI(authenticationURL())); // start the oauth flow

    }
    catch (Exception e) {
      e.printStackTrace();
    }

    new Thread(new Runnable() {
      public void run() {

        try {
          STServer.run(); // throws STException. Let it bubble out.
        }
        catch (STException e) {
          e.printStackTrace();

          switch (e.type) {
            case UNKNOWN:
              break;
            case ST_API_403:
              // these will also cleanup all SmartBlockTileEntities currently loaded
              STFileAccess.deleteToken();
              STFileAccess.deleteEndpoint();
              break;
            case ST_SERVER_FAIL:
              break;
          }
        }

      }
    }).start();

  }

  public static void getToken(String code) {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return;
    }

    getOauthToken(code);
  }

  public static void getEndpoint(String accessToken) throws STException {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return;
    }

    collectEndpoints(accessToken);
  }

  //================================================================================
  // GET
  //================================================================================

  private static String makeGETRequest(String relativePath, HashMap<String, String> params, String accessToken) throws STException {
    String getResponse = null;

    try {
      getResponse = httpGETRequest(relativePath, params, accessToken);
      String getResponseString = getResponse == null ? "" : getResponse;
      log(relativePath + " => " + getResponseString);
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    return getResponse;
  }

  private static String httpGETRequest(String relativePath, HashMap<String, String> params, String accessToken) throws IOException, STException {

    if (accessToken != null) {
      params.put("access_token", accessToken);
    }

    String fullURL = baseURL() + relativePath + processGETParams(params);
    log(fullURL);
    URL url = new URL(fullURL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    int responseCode = conn.getResponseCode();
    log("conn.getResponseCode(): " + responseCode);

    if (responseCode < 200 || responseCode >= 300) {
      if (responseCode == 403) {
        log("403");
        STFileAccess.deleteToken();
        STFileAccess.deleteEndpoint();
        throw new STException(conn.getResponseMessage(), STExceptionType.ST_API_403);
      }
      throw new IOException(conn.getResponseMessage());
    }

    // Buffer the result into a string
    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = rd.readLine()) != null) {
      sb.append(line);
    }
    rd.close();

    conn.disconnect();
    return sb.toString();
  }

  private static String processGETParams(HashMap paramsMap) {
    String entries = "";

    Iterator it = paramsMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pairs = (Map.Entry) it.next();
      entries = addGETEntry(entries, pairs.getKey(), pairs.getValue());
      it.remove(); // avoids a ConcurrentModificationException
    }

    String params = wrapGETEntries(entries);
    log("processed params: " + params);
    return params;
  }

  private static String addGETEntry(String params, Object key, Object value) {
    if (params.length() > 0) params += "&"; // adding another entry
    return params + processGETEntry(key, value);
  }

  private static String processGETEntry(Object key, Object value) {
    return key.toString() + "=" + value.toString();
  }

  private static String wrapGETEntries(String entries) {
    if (entries.length() > 0) entries = "?" + entries;
    return entries;
  }

  //================================================================================
  // PUT, POST, DELETE
  //================================================================================

  // TODO: don't forget to make this private again
  public static String makeRequest(String httpMethod, String relativePath, HashMap<String, String> params, String accessToken) throws STException {
    String response = null;

    try {
      response = httpRequest(httpMethod, relativePath, params, accessToken);
      String responseString = response == null ? "" : response;
      log(relativePath + " => " + responseString);
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    return response;
  }

  private static String httpRequest(String httpMethod, String relativePath, HashMap<String, String> params, String accessToken) throws IOException, STException {

    String baseURL = baseURL();
    String urlString = baseURL + relativePath;

    log("accessToken: " + accessToken);
    String tokenParam = "";
    if (accessToken != null) {
      tokenParam = "access_token=" + accessToken;
    }

    if (urlString.contains("?")) {
      urlString += "&";
    } else {
      urlString += "?";
    }
    urlString += tokenParam;

    log("making httpRequest: " + urlString);

    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod(httpMethod);
    conn.setDoOutput(true);
    conn.setDoInput(true);
    conn.setUseCaches(false);
    conn.setAllowUserInteraction(false);
    conn.setRequestProperty("Content-Type", "application/json");

    String urlParameters = processParams(params);
    conn.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
    conn.setRequestProperty("Content-Language", "en-US");

    //Send request
    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
    wr.writeBytes(urlParameters);
    wr.flush();
    wr.close();

    int responseCode = conn.getResponseCode();
    log("conn.getResponseCode(): " + responseCode);

    if (responseCode < 200 || responseCode >= 300) {
      if (responseCode == 403) {
        STFileAccess.deleteToken();
        STFileAccess.deleteEndpoint();
        throw new STException(conn.getResponseMessage(), STExceptionType.ST_API_403);
      }
      throw new IOException(conn.getResponseMessage());
    }

    // Buffer the result into a string
    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = rd.readLine()) != null) {
      sb.append(line);
    }
    rd.close();

    conn.disconnect();
    return sb.toString();
  }

  private static String processParams(HashMap paramsMap) {

    String mapEntries = "";

    Iterator it = paramsMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pairs = (Map.Entry) it.next();
      mapEntries = addEntry(mapEntries, pairs.getKey(), pairs.getValue());
      it.remove(); // avoids a ConcurrentModificationException
    }

    String params = wrapEntries(mapEntries);
    log("processed params: " + params);
    return params;
  }

  private static String processEntry(Object key, Object value) {
    return key.toString() + ":" + value.toString();
  }

  private static String addEntry(String params, Object key, Object value) {
    if (params.length() > 0) params += ","; // adding another entry
    return params + processEntry(key, value);
  }

  private static String wrapEntries(String entries) {
    if (entries.length() == 0) entries = ":"; // empty map
    return "{" + entries + "}";
  }

  //================================================================================
  // URL // TODO: don't hard-code anything in here
  //================================================================================

  private static String clientId() {
    return STFileAccess.getClientId();
  }

  private static String clientSecret() {
    return STFileAccess.getClientSecret();
  }

  private static String port() {
    return "4444";
  }

  private static int portNumber() {
    return 4444;
  }

  private static String redirectUri() {
    return "http://localhost:" + port() + "/oauth/callback";
  }

  private static String baseURL() {

    log("baseURL");

    String baseURL = STFileAccess.getSmartThingsBaseURL();

    log("baseURL: " + baseURL);

    if (baseURL != null && baseURL.length() > 0) {
      return baseURL;
    }

    log("not returning baseURL: " + baseURL);

    return "https://graph.api.smartthings.com";
  }

  private static String authenticationURL() {
    String base = baseURL() + "/oauth/authorize";
    base += "?response_type=code";
    base += "&client_id=" + clientId();
    base += "&client_secret=" + clientSecret();
    base += "&redirect_uri=" + redirectUri();
    base += "&scope=app";
    return base;
  }

  //================================================================================
  // Logging
  //================================================================================

  private static void log(Object message) {
    STLogger.fLog(message, STRest.class);
  }

  //================================================================================
  // Authentication
  //================================================================================

  private static void getOauthToken(String code) {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return;
    }

    if (code == null || code.length() == 0) {
      return;
    }

    try {
      STServer.kill();

      // this isn't working for some reason. Figure out why at some point
      HashMap<String, String> params = new HashMap<String, String>();
      params.put("grant_type", "authorization_code");
      params.put("client_id", clientId());
      params.put("client_secret", clientSecret());
      params.put("scope", "app");
      params.put("redirect_uri", redirectUri());
      params.put("code", code);

      // but this is working
      String relativePath = "/oauth/token";
      String fullPath = relativePath + "?grant_type=authorization_code";
      fullPath += "&client_id=" + clientId();
      fullPath += "&client_secret=" + clientSecret();
      fullPath += "&scope=app";
      fullPath += "&redirect_uri=" + redirectUri();
      fullPath += "&code=" + code;

      String response = httpRequest("POST", fullPath, params, null);
      String token = parseTokenResponse(response);
      if (token != null) {
        collectEndpoints(token);
      }
    }
    catch (STException se) {
      // TODO: alert the user?
      log("failed to get token");
      se.printStackTrace();
    }
    catch (Exception e) {
      log("failed to get token");
      e.printStackTrace();
    }
  }

  private static void collectEndpoints(String token) throws STException {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return;
    }

    try {
      String relativePath = "/api/smartapps/endpoints";

      HashMap<String, String> params = new HashMap<String, String>();
      params.put("client_id", clientId());

      String endpoints = httpGETRequest(relativePath, params, token);
      processEndpoints(endpoints); // TODO: throw STException?

    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void processEndpoints(String endpoints) {
    log("endpoints: " + endpoints);

    if (endpoints == null || endpoints.length() == 0 || endpoints.equals("[]")) {
      log("No endpoints; clearing credentials");
      STFileAccess.deleteToken();
      STFileAccess.deleteEndpoint();
      return;
    }

    try {
      String[] parts = endpoints.split("/api/smartapps/installations/");
      String id = parts[1];
      int i = id.indexOf("\"");
      id = id.substring(0, i);

      String endpoint = "/api/smartapps/installations/" + id;
      STFileAccess.writeEndpointToFile(endpoint);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

  private static String parseTokenResponse(String response) {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return null;
    }

    response = response.replace("{", "");
    response = response.replace("}", "");
    response.trim();

    HashMap<String, String> responseMap = new HashMap<String, String>();

    String[] pairs = response.split(",");
    for (String pair : pairs) {
      pair = pair.replace("\"", "").trim();
      String[] pairParts = pair.split(":");
      String key = pairParts[0].replace("\"", "").trim();
      String value = pairParts[1].replace("\"", "").trim();
      responseMap.put(key, value);
    }

    String accessToken = responseMap.get("access_token");
    return STFileAccess.writeTokenToFile(accessToken);
  }

  //================================================================================
  // TESTING
  //================================================================================

}
