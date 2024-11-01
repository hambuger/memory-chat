package com.github.hambuger.memory.chat.memory.other.controller.page;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

@Data
public class LikeVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 4720454534757542401L;

    @NotBlank
    private String userKey;

    // 1:喜欢 2：取消
    @NotNull
    private Integer likeOperate;

    @NotBlank
    private String pageId;

    @NotBlank
    private String likeId;

}
