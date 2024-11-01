package com.github.hambuger.memory.chat.memory.other.controller.page;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class PostsPage implements Serializable {

    @Serial
    private static final long serialVersionUID = -3054527401282498669L;
    private String id;
    @NotBlank
    private String authKey;
    private String author;
    private int comments;
    @NotBlank
    private String content;
    private String createdAt;
    private String image;
    private int likes;
    private String status;
    @NotBlank
    private String title;
    private String updatedAt;
    private String updater;
    private List<String> tags;
    private Integer likeStatus;
}
