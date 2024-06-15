package memory.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * @author hamburger
 * @since 2024/6/14
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BaseMemoryDTO {

    /**
     * 消息创建者id
     */
    private String messageCreatorId;

    /**
     * 消息创建者名称
     */
    private String messageCreatorName;

    /**
     * 消息创建者类型
     */
    private String messageCreatorType;

    /**
     * 消息接受者id
     */
    private String messageReceiveId;

    /**
     * 消息接受者名称
     */
    private String messageReceiveName;

    /**
     * 消息接受者类型
     */
    private String messageReceiveType;

    /**
     * 消息拥有者id
     */
    private String messageOwnerId;

    /**
     * 消息拥有者名称
     */
    private String messageOwnerName;

    /**
     * 消息拥有者类型
     */
    private String messageOwnerType;

    /**
     * 消息发送时间
     */
    private Date messageCreateAt;

    /**
     * 消息内容类型
     */
    private String messageContentType;


    /**
     * 消息内容
     */
    private String messageContent;

}
