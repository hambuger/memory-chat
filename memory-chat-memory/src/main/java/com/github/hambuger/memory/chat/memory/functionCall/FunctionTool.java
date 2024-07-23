package com.github.hambuger.memory.chat.memory.functionCall;

import com.github.hambuger.memory.chat.memory.chat.dto.ChatSceneEnum;

import org.springframework.ai.openai.api.OpenAiApi;

import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/7/23
 */
@Data
public class FunctionTool {

    private OpenAiApi.FunctionTool functionTool;

    private ChatSceneEnum[] scene;

}
