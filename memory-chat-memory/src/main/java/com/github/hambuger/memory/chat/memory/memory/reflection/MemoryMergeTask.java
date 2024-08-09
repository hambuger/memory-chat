package com.github.hambuger.memory.chat.memory.memory.reflection;

import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;

import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/8/9
 */
@Slf4j
@Component
public class MemoryMergeTask {

    @Resource
    private RedisUtil redisUtil;

    public void memoryMerge(){







    }


    public boolean insertNewMemory(){

        return true;
    }

    public boolean updateOldMemory(){

        return true;
    }
    public boolean deleteOldMemory(){

        return true;
    }






}
