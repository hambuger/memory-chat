package com.github.hambuger.memory.chat.memory.other.controller.page;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class CommentVO implements Serializable {
    @Serial
    private static final long serialVersionUID = -7132802406975347498L;

    private String id;

    @NotBlank
    private String userKey;

    private String userId;

    private String userName;

    private String userAvatarUrl;

    private Date commentTime;

    // PAGE  BLOCK
    @NotBlank
    private String commentType;

    @NotBlank
    private String pageId;

    @NotBlank
    private String commentId;

    @NotBlank
    private String commentText;

    @NotBlank
    private Integer commentEndIndex;

    @NotBlank
    private Integer commentStartIndex;

}
