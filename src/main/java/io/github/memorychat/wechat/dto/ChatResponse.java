package io.github.memorychat.wechat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author hamburger
 * @since 2024/6/17
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {

    private String messageType;

    private String messageContent;

}
