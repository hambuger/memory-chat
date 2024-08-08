package com.github.hambuger.memory.chat.memory.tools.weather;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.hambuger.memory.chat.memory.util.MyHttpUtils;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/8/8
 */
@Slf4j
@Component
public class WeatherQuery {

    private static final String Weather_url = "http://t.weather.itboy.net/api/weather/city/%s";

    public static String getWeather(String city) {

        String response = null;
        try {
            response = MyHttpUtils.get(String.format(Weather_url, city), null, null);
            System.out.println(response);
            JSONObject jsonObject = JSON.parseObject(response);
            // 提取天气数据
            JSONObject todayWeather = jsonObject.getJSONObject("data");
            String shidu = todayWeather.getString("shidu");
            String pm25 = todayWeather.getString("pm25");
            String pm10 = todayWeather.getString("pm10");
            String quality = todayWeather.getString("quality");
            String wendu = todayWeather.getString("wendu");
            String ganmao = todayWeather.getString("ganmao");
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }

        return null;
    }


    public static void main(String[] args) {
        getWeather("101210106");
    }


}
