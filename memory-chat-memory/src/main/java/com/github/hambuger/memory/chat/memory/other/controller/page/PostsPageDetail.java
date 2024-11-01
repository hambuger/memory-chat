package com.github.hambuger.memory.chat.memory.other.controller.page;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class PostsPageDetail extends  PostsPage implements Serializable {
    @Serial
    private static final long serialVersionUID = 4432700602888036285L;

    private List<CommentVO> commentVOList;
}
