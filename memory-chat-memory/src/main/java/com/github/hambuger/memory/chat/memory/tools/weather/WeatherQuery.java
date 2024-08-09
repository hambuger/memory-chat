package com.github.hambuger.memory.chat.memory.tools.weather;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.util.MyHttpUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * @author hanjiabao
 * @since 2024/8/8
 */
@Slf4j
@Component
public class WeatherQuery {

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo?address=%s&output=json&key=%s";

    private static final String WEATHER_URL = "https://restapi.amap.com/v3/weather/weatherInfo?city=%s&key=%s";

    @Value("${weather.key}")
    private String weatherKey;

    @Data
    public static class WeatherParam {

        @JsonPropertyDescription("地址名")
        @JsonProperty(required = true)
        private String address;

    }

    @FunctionCallRegistry(functionDesc = "查询天气", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.SCHEDULE, ChatSceneEnum.NEWS_SCHEDULE, ChatSceneEnum.TASK, ChatSceneEnum.PLAN})
    public String getWeather(WeatherParam param) {
        try {
            String locationName = param.getAddress();
            String geocodeUrl = String.format(GEOCODE_URL, locationName, weatherKey);
            String geocodeResponse = MyHttpUtils.get(geocodeUrl, null, null);
            JSONObject geocodeJson = JSON.parseObject(geocodeResponse);
            JSONArray geocodes = geocodeJson.getJSONArray("geocodes");
            if (geocodes == null || geocodes.isEmpty()) {
                return null;
            }
            String adcode = geocodes.getJSONObject(0).getString("adcode");
            String weatherUrl = String.format(WEATHER_URL, adcode, weatherKey);
            String weatherResponse = MyHttpUtils.get(weatherUrl, null, null);
            JSONObject weatherJson = JSON.parseObject(weatherResponse);
            JSONArray lives = weatherJson.getJSONArray("lives");
            if (lives == null || lives.isEmpty()) {
                return null;
            }
            return lives.getJSONObject(0).toJSONString();

        } catch (Exception e) {
            log.error("get weather error", e);
            return null;
        }
    }


}
