package com.github.hambuger.memory.chat.memory.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RedisUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EMOJI_AND_MEDIA_ID_MAP_KEY = "emojiAndMediaIdMap";

    public void delOldMemory(String msgListKey, int i) {
        String json = redisTemplate.opsForValue().get(msgListKey);
        if (json != null) {
            try {
                List<MemoryDTO> oldMemoryList = objectMapper.readValue(json, List.class);
                List<MemoryDTO> newMemoryList = oldMemoryList.subList(i + 1, oldMemoryList.size());
                redisTemplate.opsForValue().set(msgListKey, objectMapper.writeValueAsString(newMemoryList));
            } catch (JsonProcessingException e) {
                log.warn("delOldMemory error", e);
            }
        }
    }

    public void incrBy(String key, Integer amount) {
        redisTemplate.opsForValue().increment(key, amount);
    }

    public long get(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    public void addElement(String key, String element) {
        redisTemplate.opsForList().rightPush(key, element);
    }

    public void reset(String key) {
        redisTemplate.delete(key);
    }

    public List<String> getList(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void addMsg(String key, MemoryDTO msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);
            redisTemplate.opsForList().rightPush(key, json);
        } catch (JsonProcessingException e) {
            log.warn("addMsg error", e);
        }
    }

    public List<MemoryDTO> getMsg(String key) {
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        List<MemoryDTO> msgList = new ArrayList<>();
        if (jsonList != null) {
            for (String json : jsonList) {
                try {
                    msgList.add(objectMapper.readValue(json, MemoryDTO.class));
                } catch (JsonProcessingException e) {
                    log.warn("getMsg error", e);
                }
            }
        }
        return msgList;
    }

    public void putEmojiAndMediaId(String key, List<String> value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForHash().put(EMOJI_AND_MEDIA_ID_MAP_KEY, key, json);
        } catch (JsonProcessingException e) {
            log.warn("putEmojiAndMediaId error", e);
        }
    }

    public List<String> getEmojiAndMediaId(String key) {
        String json = (String) redisTemplate.opsForHash().get(EMOJI_AND_MEDIA_ID_MAP_KEY, key);
        if (json != null) {
            try {
                return objectMapper.readValue(json, List.class);
            } catch (JsonProcessingException e) {
                log.warn("getEmojiAndMediaId error", e);
            }
        }
        return new ArrayList<>();
    }
}

