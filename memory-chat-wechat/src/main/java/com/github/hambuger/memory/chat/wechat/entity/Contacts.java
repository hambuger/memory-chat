package com.github.hambuger.memory.chat.wechat.entity;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;
import java.util.Objects;

import javax.swing.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @作者 Hamburger
 * @项目 AutoWechat
 * @创建时间 6/14/2021 7:41 PM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contacts {
        private String username;

        private String nickname;

        private String signature;

        private Byte sex;

        private String remarkname;

        private String province;

        private String city;

        private Double chatroomid;

        private Double attrstatus;

    /**
     * 等于0消息免打扰 1正常
     */
        private Double statues;

        private String pyquanpin;

        private String encrychatroomid;

        private String displayname;

        private Integer verifyflag;

        private Double unifriend;

        private Double contactflag;

    //@ExcelCollection(name="群成员")
    private List<Contacts> memberlist;

        private Double starfriend;

        private String headimgurl;

        private Double appaccountflag;

        private Double membercount;

        private String remarkpyinitial;


        private Double snsflag;

        private String alias;

        private String keyword;

        private Double hideinputbarflag;

        private String remarkpyquanpin;

        private Double uin;

        private Double owneruin;

        private Double isowner;

        private String pyinitial;

        private String ticket;


    /**
     * 联系人类型
     */
    public enum ContactsType{
        GROUP_USER((byte)1,"群组")
        ,PUBLIC_USER((byte)2,"公众号")
        ,SPECIAL_USER((byte)3,"特殊账号")
        ,ORDINARY_USER((byte)4,"普通用户");
        public final byte code;
        public final String desc ;

        ContactsType(byte code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
    private ContactsType type = ContactsType.ORDINARY_USER;
    /**
     * 是否为联系人(false则为群成员)
     */
    @JSONField(serialize = false)
    private Boolean iscontacts;
    /**
     * 头像
     */
    @JSONField(serialize = false)
    private ImageIcon avatarIcon;

    /**
     * 群id
     */
    @JSONField(serialize = false)
    private String groupName;
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Contacts contacts = (Contacts) o;
        return contacts.username.equals(username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }
}