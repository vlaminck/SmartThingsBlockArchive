package com.smartthings.utility;


public enum STExceptionType {
  UNKNOWN,
  ST_API_403, // SmartThings API returned a 403; clear all auth info
  ST_SERVER_FAIL, // There was an exception starting the STServer
  ST_REST_SERVER_FAIL // There was an exception starting the STRestServer
}
