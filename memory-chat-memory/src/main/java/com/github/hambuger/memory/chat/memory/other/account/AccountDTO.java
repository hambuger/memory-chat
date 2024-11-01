package com.github.hambuger.memory.chat.memory.other.account;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;

@Data
public class AccountDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 429107310047651154L;


    private String userId;

    @NotBlank
    private String userEmail;
    @NotBlank
    private String userName;
    private String userKey;
    private String accountStatus;
    private String creationDate;
    private String accountPermissions;

    private String userAvatarBase64;
    private String userAvatarUrl;

}
