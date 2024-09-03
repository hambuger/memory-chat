package com.github.hambuger.memory.chat.memory.other.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import com.github.hambuger.memory.chat.memory.portrait.model.FriendPortrait;
import com.github.hambuger.memory.chat.memory.portrait.model.GroupPortrait;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.PORTRAIT_KEY_SUFFIX;


@Component
@Slf4j
public class RedisUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisTemplate<String, Object> commonRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String LEARN_SKILL_KEY = "learnSkillMap";

    private static final String CHAT_FRIEND_LIST_KEY = "chatFriends";

    public void putKeyValue(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public void saveMap(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    public Map<Object, Object> getMap(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    // 添加成员到集合
    public void addMember(Object member) {
        commonRedisTemplate.opsForSet().add(CHAT_FRIEND_LIST_KEY, member);
    }

    // 获取集合中的所有成员
    public Set<Object> getAllMembers() {
        return commonRedisTemplate.opsForSet().members(CHAT_FRIEND_LIST_KEY);
    }


    public void delOldMemory(String msgListKey, int i) {
        Long listSize = redisTemplate.opsForList().size(msgListKey);
        if (listSize == null || i < 0 || i > listSize) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        redisTemplate.opsForList().trim(msgListKey, i + 1, listSize);
    }

    public FriendPortrait getFriendPortraitInfo(String name) {
        String value = redisTemplate.opsForValue().get(name + PORTRAIT_KEY_SUFFIX);
        if (StringUtils.isNotBlank(value)) {
            try {
                return objectMapper.readValue(value, FriendPortrait.class);
            } catch (JsonProcessingException e) {
                log.error("getFriendPortrait error", e);
            }
        }
        return null;
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
        return redisTemplate.opsForValue().get(selfStatus);
    }

    public void setString(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
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

    public void addElement(String key, String element, int maxSize) {
        // 添加元素到List末尾
        redisTemplate.opsForList().rightPush(key, element);

        // 获取当前List的长度
        Long size = redisTemplate.opsForList().size(key);

        // 如果List长度超过最大尺寸，进行修剪
        if (size != null && size > maxSize) {
            // 保留List的最后 maxSize 个元素
            redisTemplate.opsForList().trim(key, size - maxSize, size - 1);
        }
    }


    public void reset(String key) {
        redisTemplate.delete(key);
    }


    public List<String> getList(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void updateMsgContentById(String key, String targetId, String newContent) {
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        if (jsonList != null) {
            for (int i = 0; i < jsonList.size(); i++) {
                String json = jsonList.get(i);
                try {
                    MemoryDTO msg = objectMapper.readValue(json, MemoryDTO.class);
                    if (msg.getMessageId().equals(targetId)) {
                        msg.setMessageContent(newContent);
                        String updatedJson = objectMapper.writeValueAsString(msg);
                        redisTemplate.opsForList().set(key, i, updatedJson);
                        break;
                    }
                } catch (Exception e) {
                    log.warn("updateMsgContentById error", e);
                }
            }
        }
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
        msgList.sort(Comparator.comparing((msg -> DateUtil.parse(msg.getMessageCreateAt(), DatePattern.NORM_DATETIME_FORMAT))));
        return msgList;
    }

    public void putLearnSkill(String key, String value) {
        try {
            redisTemplate.opsForHash().put(LEARN_SKILL_KEY, key, value);
        } catch (Exception e) {
            log.warn("putLearnSkill error", e);
        }
    }

    public Set<String> getAllLearnSkill() {
        try {
            redisTemplate.opsForHash().keys(LEARN_SKILL_KEY);
        } catch (Exception e) {
            log.warn("getAllLearnSkill error", e);
        }
        return null;
    }


    public String getLearnSkill(String key) {
        try {
            redisTemplate.opsForHash().get(LEARN_SKILL_KEY, key);
        } catch (Exception e) {
            log.warn("getLearnSkill error", e);
        }
        return null;
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
            if (friendPortrait.getOtherImportantInfo() != null) {
                portrait.getOtherImportantInfo().addAll(friendPortrait.getOtherImportantInfo());
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
            if (groupPortrait.getOtherImportantInfo() != null) {
                portrait.getOtherImportantInfo().addAll(groupPortrait.getOtherImportantInfo());
            }
            String json = objectMapper.writeValueAsString(portrait);
            redisTemplate.opsForValue().set(name + PORTRAIT_KEY_SUFFIX, json);
        } catch (JsonProcessingException e) {
            log.error("updateGroupPortrait error", e);
        }
    }

    public boolean acquireLock(String lockKey, String lockValue, long lockMaxTime, long maxWaitTime) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        long beginTime = System.currentTimeMillis();
        while (true) {
            Boolean success = ops.setIfAbsent(lockKey, lockValue, lockMaxTime, TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(success)) {
                return true;
            }
            if (System.currentTimeMillis() - beginTime > maxWaitTime) {
                return false;
            }
        }
    }

    public boolean releaseLock(String lockKey, String lockValue) {
        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else " +
                        "return 0 " +
                        "end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockValue);
        return result != null && result == 1L;
    }
}

