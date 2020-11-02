package com.smartthings.network;

import com.smartthings.utility.STException;
import com.smartthings.utility.STExceptionType;
import com.smartthings.utility.STFileAccess;

import java.util.HashMap;

public class STRestRunnable implements Runnable {

  private String method, relativeUrl, accessToken;
  private HashMap<String, String> params;

//makeRequest("PUT", relativeUrl, params, accessToken);

  STRestRunnable(String httpMethod, String relativePath, HashMap<String, String> params, String accessToken) {
    this.method = httpMethod;
    this.relativeUrl = relativePath;
    this.params = params;
    this.accessToken = accessToken;
  }


  @Override
  public void run() {

    new Thread(new Runnable() {
      public void run() {

        try {
          String response = "";

          if (method.equals("PUT")) {
            response = STRest.PUT(relativeUrl, params, accessToken);
          } else if (method.equals("POST")) {
            response = STRest.POST(relativeUrl, params, accessToken);
          } else if (method.equals("DELETE")) {
            response = STRest.DELETE(relativeUrl, params, accessToken);
          } else if (method.equals("GET")) {
            response = STRest.GET(relativeUrl, params, accessToken);
          }

          System.out.println("STRestRunnable response: " + response);

        }
        catch (STException e) {
          e.printStackTrace();

          if (e.type == STExceptionType.ST_API_403) {

            STFileAccess.deleteToken();
            STFileAccess.deleteEndpoint();

          }
        }

      }
    }).start();

  }
}
