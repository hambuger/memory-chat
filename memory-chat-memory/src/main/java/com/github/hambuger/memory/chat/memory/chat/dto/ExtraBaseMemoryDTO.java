package com.github.hambuger.memory.chat.memory.chat.dto;

import com.github.hambuger.memory.chat.memory.memory.model.BaseMemoryDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ExtraBaseMemoryDTO extends BaseMemoryDTO {

    private String fromUserName;
}
