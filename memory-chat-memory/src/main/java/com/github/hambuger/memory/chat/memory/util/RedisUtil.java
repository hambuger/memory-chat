package com.github.hambuger.memory.chat.memory.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.hambuger.memory.chat.memory.chat.dto.FriendPortrait;
import com.github.hambuger.memory.chat.memory.chat.dto.GroupPortrait;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.github.hambuger.memory.chat.memory.constants.MemoryChatConstants.PORTRAIT_KEY_SUFFIX;


@Component
@Slf4j
public class RedisUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EMOJI_AND_MEDIA_ID_MAP_KEY = "emojiAndMediaIdMap";


    public void delOldMemory(String msgListKey, int i) {
        Long listSize = redisTemplate.opsForList().size(msgListKey);
        if (listSize == null || i < 0 || i > listSize) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        redisTemplate.opsForList().trim(msgListKey, i + 1, listSize);
    }


    public String getFriendPortrait(String name) {
        String value = redisTemplate.opsForValue().get(name + PORTRAIT_KEY_SUFFIX);
        if (StringUtils.isNotBlank(value)) {
            try {
                FriendPortrait friendPortrait = objectMapper.readValue(value, FriendPortrait.class);
                return friendPortrait.toMarkDown();
            } catch (JsonProcessingException e) {
                log.error("getFriendPortrait error", e);
            }
        }
        return null;
    }


    public String getGroupPortrait(String name) {
        String value = redisTemplate.opsForValue().get(name + PORTRAIT_KEY_SUFFIX);
        if (StringUtils.isNotBlank(value)) {
            try {
                GroupPortrait groupPortrait = objectMapper.readValue(value, GroupPortrait.class);
                return groupPortrait.toMarkDown();
            } catch (JsonProcessingException e) {
                log.error("getGroupPortrait error", e);
            }
        }
        return null;
    }


    public String getString(String selfStatus) {

        return null;
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


    public void updateFriendPortrait(String name, String jsonString) {
        String value = redisTemplate.opsForValue().get(name + PORTRAIT_KEY_SUFFIX);
        try {
            if (StringUtils.isBlank(value)) {
                redisTemplate.opsForValue().set(name + PORTRAIT_KEY_SUFFIX, jsonString);
                return;
            }
            FriendPortrait portrait = objectMapper.readValue(value, FriendPortrait.class);
            FriendPortrait friendPortrait = objectMapper.readValue(jsonString, FriendPortrait.class);
            BeanUtils.copyProperties(friendPortrait, portrait);
            if (friendPortrait.getOtherInfo() != null) {
                portrait.getOtherInfo().putAll(friendPortrait.getOtherInfo());
            }
            String json = objectMapper.writeValueAsString(portrait);
            redisTemplate.opsForValue().set(name + PORTRAIT_KEY_SUFFIX, json);
        } catch (JsonProcessingException e) {
            log.error("updateFriendPortrait error", e);
        }
    }


    public void updateGroupPortrait(String name, String jsonString) {
        String value = redisTemplate.opsForValue().get(name + PORTRAIT_KEY_SUFFIX);
        try {
            if (StringUtils.isBlank(value)) {
                redisTemplate.opsForValue().set(name + PORTRAIT_KEY_SUFFIX, jsonString);
                return;
            }
            GroupPortrait portrait = objectMapper.readValue(value, GroupPortrait.class);
            GroupPortrait groupPortrait = objectMapper.readValue(jsonString, GroupPortrait.class);
            BeanUtils.copyProperties(groupPortrait, portrait);
            if (groupPortrait.getOtherInfo() != null) {
                portrait.getOtherInfo().putAll(groupPortrait.getOtherInfo());
            }
            String json = objectMapper.writeValueAsString(portrait);
            redisTemplate.opsForValue().set(name + PORTRAIT_KEY_SUFFIX, json);
        } catch (JsonProcessingException e) {
            log.error("updateGroupPortrait error", e);
        }
    }
}

