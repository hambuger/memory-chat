package com.github.hambuger.memory.chat.memory.memory.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * @author hamburger
 * @since 2024/6/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class MemoryDTO extends BaseMemoryDTO {

    /**
     * 消息ID
     */
    private String messageId;


    /**
     * 信息深度
     */
    private Integer memoryLeafDepth;


    /**
     * 消息重要分
     */
    private Double messageImportanceScore;

    /**
     * 消息最后读取时间
     */
    private String messageLastAccessTime;


    /**
     * 消息向量
     */
    private List<Double> messageContentVector;


    /**
     * 消耗token数
     */
    private Integer useToken;

    /**
     * 消息父id
     */
    private List<String> messageParentIds;

    /**
     * 是否AI回复，1:是 0:否
     */
    private String aiResponseFlag;

}
