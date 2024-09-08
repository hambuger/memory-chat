package com.github.hambuger.memory.chat.memory.chat.model;

import com.github.hambuger.memory.chat.memory.chat.message.SendMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * @author hamburger
 * @since 2024/6/17
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {

    List<SendMessage> sendMessageList;

}
