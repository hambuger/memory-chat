package com.github.hambuger.memory.chat.memory.chat.dto;

import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;

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

    private OpenAiApi.ChatCompletionMessage chatMessage;

}
