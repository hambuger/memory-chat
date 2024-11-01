package com.github.hambuger.memory.chat.memory.other.controller.page;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CommentInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 2246695379319217679L;


    private Integer commentEndIndex;
    private String commentId;
    private Integer commentStartIndex;
    private String commentText;
    private String commentTime;
    private String commentType;
    private String commentUserId;
    private String commenterId;
    private String parentCommentId;
    private String commentStatus;




}
