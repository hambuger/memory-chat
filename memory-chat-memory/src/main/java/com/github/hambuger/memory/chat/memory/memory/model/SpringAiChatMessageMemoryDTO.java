package com.github.hambuger.memory.chat.memory.memory.model;

import com.alibaba.fastjson.annotation.JSONField;

import org.springframework.ai.openai.api.OpenAiApi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * @author hanjiabao
 * @since 2024/6/26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class SpringAiChatMessageMemoryDTO extends MemoryDTO {

    @JSONField(serialize = false)
    private OpenAiApi.ChatCompletionMessage chatMessage;

}
