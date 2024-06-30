package com.github.hambuger.memory.chat.memory.util;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.Map;

@Slf4j
public class MyHttpUtils {

    private static final int MAX_RETRIES = 3;

    public static String put(String urlString, Map<String, Object> paramMap, Map<String, String> headers) throws UnirestException {
        JSONObject jsonObject = new JSONObject(paramMap);
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                HttpResponse<String> response = Unirest.put(urlString).headers(headers).body(jsonObject).asString();
                return response.getBody();
            } catch (UnirestException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw e;
                }
                log.info("Attempt " + attempt + " failed, retrying...");
            }
        }
        throw new UnirestException("Failed after " + MAX_RETRIES + " attempts");
    }

    public static String get(String urlString, Map<String, String> headers) throws UnirestException {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                HttpResponse<String> response = Unirest.get(urlString).headers(headers).asString();
                return response.getBody();
            } catch (UnirestException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw e;
                }
                log.info("Attempt " + attempt + " failed, retrying...");
            }
        }
        throw new UnirestException("Failed after " + MAX_RETRIES + " attempts");
    }

}

