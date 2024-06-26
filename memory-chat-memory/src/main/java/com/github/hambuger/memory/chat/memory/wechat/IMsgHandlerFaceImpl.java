package com.github.hambuger.memory.chat.memory.wechat;

import com.github.hambuger.memory.chat.memory.chat.ChatCompletionsApi;
import com.github.hambuger.memory.chat.memory.chat.dto.ChatResponse;
import com.github.hambuger.memory.chat.memory.chat.dto.ContentTypeEnum;
import com.github.hambuger.memory.chat.memory.chat.dto.ExtraBaseMemoryDTO;
import com.github.hambuger.memory.chat.memory.constants.CommonConstants;
import com.github.hambuger.memory.chat.memory.util.FileUtil;
import com.github.hambuger.memory.chat.wechat.api.ContactsTools;
import com.github.hambuger.memory.chat.wechat.api.MessageTools;
import com.github.hambuger.memory.chat.wechat.constant.WxReqParamsConstant;
import com.github.hambuger.memory.chat.wechat.constant.WxRespConstant;
import com.github.hambuger.memory.chat.wechat.core.Core;
import com.github.hambuger.memory.chat.wechat.entity.Message;
import com.github.hambuger.memory.chat.wechat.entity.Status;
import com.github.hambuger.memory.chat.wechat.service.IMsgHandlerFace;
import com.github.hambuger.memory.chat.wechat.utils.ExecutorServiceUtil;
import com.github.hambuger.memory.chat.wechat.utils.SleepUtils;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Log4j2
@Component
public class IMsgHandlerFaceImpl implements IMsgHandlerFace {


    //    @Resource
    //    private StatusMapper statusMapper;

    /**
     * autoChatUserNameList 包含 发送者：自动回复
     * 不包含：autoChatWithPersonal = true ：自动回复，false ：不回复
     */
    private boolean autoChatWithPersonal = false;

    /**
     * 自动聊天联系人列表，包括个人、群...
     */
    public final Set<String> autoChatUserNameList = new HashSet<>();

    //    @Resource
    //    private ChartUtil chartUtil;

    @Resource
    private ChatCompletionsApi chatCompletionsApi;


    /**
     * 已关闭防撤回联系人列表
     */
    public final Set<String> nonPreventUndoMsgUserName = new HashSet<>();


    //    @PostConstruct
    private void initSet() {
        //        log.info("11. 获取自动聊天列表及防撤回列表");
        //        List<Status> statuses = statusMapper.selectByExample(new StatusExample());
        //        for (Status status : statuses) {
        //            if (status.getAutoStatus() != null && status.getAutoStatus() == 1) {
        //                autoChatUserNameList.add(status.getName());
        //            }
        //            if (status.getUndoStatus() != null && status.getUndoStatus() == 2) {
        //                nonPreventUndoMsgUserName.add(status.getName());
        //            }
        //        }
    }


    /**
     * 消息控制命令
     *
     * @param msg 消息
     * @return 回复消息
     */
    private List<Message> controlCommandHandler(Message msg) {
        String text = msg.getPlaintext().toLowerCase();
        List<Message> messages = new ArrayList<>();

        //=========================手动发送消息=====================
        String[] split = msg.getPlaintext().split("：");
        if (split.length >= 2 && msg.getFromUsername().equals(Core.getUserName())) {
            try {
                long sleep = 100;
                try {
                    sleep = Long.parseLong(split[2]);
                } catch (ArrayIndexOutOfBoundsException e) {

                }
                String s = split[1];
                int i = Integer.parseInt(s);
                messages.add(Message.builder().content("开始发送：" + i + "个" + split[0]).toUsername(msg.getToUsername()).msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).build());
                for (int j = 0; j < i; j++) {
                    messages.add(Message.builder().content(split[0]).msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).toUsername(msg.getToUsername()).build());
                }
                return messages;

            } catch (NumberFormatException e) {
            }
        }
        //============炸弹消息===================
        if (msg.getPlaintext().equals("[Bomb]") || msg.getPlaintext().equals("[炸弹]")) {
            String userName = Core.getUserSelf().getUsername();
            if (!msg.getFromUsername().equals(userName)) {
                for (int i = 0; i < 1; i++) {
                    messages.add(Message.builder().content("[Bomb]").msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).build());
                }
                return messages;
            }
        }

        /**
         * 自己发的消息
         * 回复时则发送给接收方，而不是消息发送者
         */
        /*String objectUserName = msg.getFromUserName();*/
        String toUserName = msg.getFromUsername();
        if (msg.getFromUsername().equals(Core.getUserName())) {
            toUserName = msg.getToUsername();
        }
        String remarkNameByGroupUserName = ContactsTools.getContactDisplayNameByUserName(toUserName);
        switch (text) {
            case "help":
            case "/h":
                if (msg.isGroup()) {
                    //群消息
                    messages.add(Message.builder().content("1、【oauto/cauto】\n\t开启/关闭群消息自动回复\n" + "2、【opundo/cpundo】\n\t开启/关闭群消息防撤回\n" + "3、【ggr】\n\t群成员性别比例图\n" + "4、【gpr】\n\t群成员省市分布图\n" + "5、【op/cp"
                            + "】\n\t开启/关闭全局个人用户消息自动回复\n" + "6、【gma10】\n\t群成员活跃度TOP10\n" + "7、【mf10】\n\t聊天消息关键词TOP10\n" + "8、【mft10】\n\t聊天消息类型TOP10\n").toUsername(toUserName).msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).build());

                }else {
                    //个人消息
                    messages.add(Message.builder().content("1、【oauto/cauto】\n\t开启/关闭当前联系人自动回复\n" + "2、【opundo/cpundo】\n\t开启/关闭当前联系人消息防撤回\n" + "3、【op/cp】\n\t开启/关闭全局个人用户消息自动回复\n" + "4、【mf10】\n\t" +
                            "聊天消息关键词TOP10\n" + "5、【gma10】\n\t活跃度TOP\n" + "6、【updateinfo】\n\t好友属性更新次数排行\n" + "7、【mft10】\n\t聊天消息类型TOP10\n").toUsername(toUserName).msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).build());
                }
                break;
            case "op":
                autoChatWithPersonal = true;
                messages.add(Message.builder().msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).content("已开启全局个人用户自动回复功能").toUsername(toUserName).build());
                log.info("已开启全局个人用户自动回复功能");
                break;
            case "cp":
                autoChatWithPersonal = false;
                messages.add(Message.builder().msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).content("已关闭全局个人用户自动回复功能").toUsername(toUserName).build());
                log.info("已关闭全局个人用户自动回复功能");
                break;
            case "oauto":
                String to = ContactsTools.getContactDisplayNameByUserName(toUserName);
                autoChatUserNameList.add(to);
                Status build = Status.builder().name(to).autoStatus((short) 1).build();
                //                statusMapper.insertOrUpdateSelectiveForSqlite(build);
                //                ChatPanelContainer.get(toUserName).getChatMessagePanel().getMessageEditorPanel().setUndoAndAutoLabel();

                messages.add(Message.builder().msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).content("已开启【" + remarkNameByGroupUserName + "】自动回复功能").toUsername(toUserName).build());
                log.info("已开启【" + remarkNameByGroupUserName + "】自动回复功能");
                break;
            case "cauto":
                to = ContactsTools.getContactDisplayNameByUserName(toUserName);
                autoChatUserNameList.remove(to);
                build = Status.builder().name(to).autoStatus((short) 2).build();
                //                statusMapper.insertOrUpdateSelectiveForSqlite(build);
                //                ChatPanelContainer.get(toUserName).getChatMessagePanel().getMessageEditorPanel().setUndoAndAutoLabel();
                messages.add(Message.builder().msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).content("已关闭【" + remarkNameByGroupUserName + "】自动回复功能").toUsername(toUserName).build());
                log.info("已关闭【" + remarkNameByGroupUserName + "】自动回复功能");
                break;
            case "opundo":
                to = ContactsTools.getContactDisplayNameByUserName(toUserName);
                nonPreventUndoMsgUserName.remove(to);
                build = Status.builder().name(to).undoStatus((short) 1).build();
                //                statusMapper.insertOrUpdateSelectiveForSqlite(build);
                //                ChatPanelContainer.get(toUserName).getChatMessagePanel().getMessageEditorPanel().setUndoAndAutoLabel();

                messages.add(Message.builder().msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).content("已开启【" + remarkNameByGroupUserName + "】防撤回功能").toUsername(toUserName).build());
                log.info("已开启【" + remarkNameByGroupUserName + "】防撤回功能");
                break;
            case "cpundo":
                to = ContactsTools.getContactDisplayNameByUserName(toUserName);
                build = Status.builder().name(to).undoStatus((short) 2).build();
                //                statusMapper.insertOrUpdateSelectiveForSqlite(build);
                //群消息
                nonPreventUndoMsgUserName.add(to);
                //                ChatPanelContainer.get(toUserName).getChatMessagePanel().getMessageEditorPanel().setUndoAndAutoLabel();

                messages.add(Message.builder().msgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode()).content("已关闭【" + remarkNameByGroupUserName + "】防撤回功能").toUsername(toUserName).build());
                log.info("已关闭【" + remarkNameByGroupUserName + "】防撤回功能");
                break;
            case "ggr":
                if (msg.isGroup()) {
                    //                    Optional<String> pathOptional = chartUtil.makeContactsAttrPieChartAsPng(toUserName, "sex", 1920, 1080);
                    //                    if (pathOptional.isPresent()) {
                    //                        //群消息
                    //                        messages.add(MessageTools.toPicMessage(pathOptional.get(), toUserName));
                    //                    }
                    log.info("计算群【" + remarkNameByGroupUserName + "】成员性别分布图");
                }

                break;
            case "gpr":
                if (msg.isGroup()) {
                    //                    Optional<String> pathOptional = chartUtil.makeContactsAttrPieChartAsPng(toUserName, "province", 1920, 1080);
                    //                    if (pathOptional.isPresent()) {
                    //                        //群消息
                    //                        messages.add(MessageTools.toPicMessage(pathOptional.get(), toUserName));
                    //                    }

                    log.info("计算群【" + remarkNameByGroupUserName + "】成员省份分布图");
                }
                break;
            case "gmt10":
                break;
            case "pmt10":
                break;
            case "gma10":
                //群成员活跃度排名
                if (msg.isGroup()) {
                    //                    String imgPath = chartUtil.makeWXMemberOfGroupActivityFile(toUserName);
                    //                    messages.add(MessageTools.toPicMessage(imgPath, toUserName));
                    log.info("计算【" + remarkNameByGroupUserName + "】成员活跃度");
                }else {
                    //                    String imgPath = chartUtil.makeWXUserActivityFile(toUserName);
                    //                    messages.add(MessageTools.toPicMessage(imgPath, toUserName));
                    log.info("计算聊天双方消息数");
                }
                break;
            case "mf10": {
                //聊天词语频率排名
                //                String imgPath = chartUtil.makeWXGroupMessageTopFile(toUserName);
                //                messages.add(MessageTools.toPicMessage(imgPath, toUserName));
                log.info("计算【" + remarkNameByGroupUserName + "】聊天关键词");
                break;
            }
            case "mft10": {
                //聊天词语频率排名
                //                String imgPath = chartUtil.makeWXGroupMessageTypeTopFile(toUserName);
                //                messages.add(MessageTools.toPicMessage(imgPath, toUserName));
                log.info("计算【" + remarkNameByGroupUserName + "】聊天类型");
                break;
            }
            case "updateinfo": {
                //生成自己的聊天类型
                //                List<String> imgs = chartUtil.makeWXContactUpdateAttrBarChart();
                //                for (String s : imgs) {
                //                    //群消息
                //                    messages.add(MessageTools.toPicMessage(s, toUserName));
                //                }

                log.info("计算【" + Core.getUserName() + "】所有好友聊天类型及关键词");
                break;
            }
            case "不要问了":
            case "不要问我":
                if (msg.getFromUsername().equals(Core.getUserName())) {
                    messages.add(Message.builder().msgType(WxReqParamsConstant.WXSendMsgCodeEnum.VOICE.getCode()).filePath("D:/weixin/MSGTYPE_VOICE/dont_ask.mp3").toUsername(toUserName).build());
                }
                break;
            default:
                break;

        }
        //延迟撤回消息，text:1  延迟一秒
        if (msg.getFromUsername().equals(Core.getUserName())) {
            try {
                String replace = msg.getPlaintext();
                int i = replace.indexOf("&amp;");
                if (i != -1) {
                    long sleep = Long.parseLong(replace.substring(i + 5));
                    final long relay = sleep == 0 ? 2 * 60 * 1000 : sleep * 1000;
                    ExecutorServiceUtil.getGlobalExecutorService().execute(() -> {
                        SleepUtils.sleep(relay);
                        MessageTools.sendRevokeMsgByUserId(msg.getToUsername(), msg.getMsgId(), msg.getMsgId() + "");
                    });
                }
            } catch (Exception e) {

            }

        }
        if (text.startsWith("attr_rate") && msg.isGroup()) {
            String substring = msg.getPlaintext().substring(msg.getPlaintext().indexOf(":") + 1);
            //            Optional<String> pathOptional = chartUtil.makeContactsAttrPieChartAsPng(toUserName, substring, 1920, 1080);
            //            if (pathOptional.isPresent()) {
            //                //群消息
            //                messages.add(MessageTools.toPicMessage(pathOptional.get(), toUserName));
            //            }
            log.info("计算群【" + remarkNameByGroupUserName + "】成员" + substring + "比例");
        }
        return messages;
    }


    @Override
    public List<Message> textMsgHandle(Message msg) {
        return dealNewMsg(msg);
        //处理控制命令
        //        List<Message> messages = controlCommandHandler(msg);
        //        if (messages.size() > 0) {
        //            return messages;
        //        }
        //        try {
        //            //是否需要自动回复
        //            String to = ContactsTools.getContactDisplayNameByUserName(msg.getFromUsername());
        //            if (autoChatUserNameList.contains(to)) {
        //                messages = autoReply(text, msg);
        //            }else if (autoChatWithPersonal && !msg.isGroup()) {
        //                messages = autoReply(text, msg);
        //            }else if (text.startsWith("；") && msg.getFromUsername().equals(Core.getUserName())) {
        //                messages = autoReply(text.substring(1), msg);
        //                for (Message message : messages) {
        //                    message.setToUsername(msg.getToUsername());
        //                }
        //            }
        //        } catch (NullPointerException | IOException e) {
        //            e.printStackTrace();
        //        }
        //        Message message = new Message();
        //        message.setToUsername(msg.getFromUsername());
        //        message.setMsgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode());
        //        message.setContent(text);
        //        messages.add(message);
        //        return messages;
    }


    @Nullable
    private List<Message> dealNewMsg(Message msg) {
        if (msg.getIsSend()) {
            return null;
        }
        ExtraBaseMemoryDTO baseMemoryDTO = new ExtraBaseMemoryDTO();
        baseMemoryDTO.setMessageContent(msg.getContent());
        ContentTypeEnum sendMsgContentTypeEnum = ContentTypeEnum.getByWxType(msg.getMsgType());
        if(sendMsgContentTypeEnum == null){
            // 不支持类型处理
            return null;
        }
        baseMemoryDTO.setMessageContentType(sendMsgContentTypeEnum.getType());
        // 对于语音和图片,表情，特殊处理文件路径
        if (sendMsgContentTypeEnum == ContentTypeEnum.AUDIO || sendMsgContentTypeEnum == ContentTypeEnum.PICTURE || sendMsgContentTypeEnum == ContentTypeEnum.EMOJI) {
            baseMemoryDTO.setMessageContent(msg.getFilePath());
        }
        baseMemoryDTO.setMessageCreatorName(StringUtils.isNoneBlank(msg.getFromRemarkname()) ? msg.getFromRemarkname() : msg.getFromNickname());
        baseMemoryDTO.setGroupMsgFlag(msg.isGroup() ? CommonConstants.YES_STR : CommonConstants.NO_STR);
        baseMemoryDTO.setRealCreatorId(StringUtils.isNoneBlank(msg.getFromMemberOfGroupNickname()) ? msg.getFromMemberOfGroupNickname() : msg.getFromMemberOfGroupDisplayname());
        baseMemoryDTO.setRealCreatorName(baseMemoryDTO.getRealCreatorId());
        baseMemoryDTO.setFromUserName(msg.getFromUsername());
        ChatResponse response = chatCompletionsApi.chat(baseMemoryDTO);
        if (response == null || CollectionUtils.isEmpty(response.getSendMessageList())) {
            return null;
        }
        for (SendMessage sendMessage : response.getSendMessageList()) {
            Message message = new Message();
            message.setToUsername(msg.getFromUsername());
            message.setContent(sendMessage.getMessageContent());
            ContentTypeEnum contentTypeEnum = ContentTypeEnum.getByType(sendMessage.getMessageContentType());
            message.setMsgType(contentTypeEnum == null ? ContentTypeEnum.TEXT.getMsgType() : contentTypeEnum.getMsgType());
            if (contentTypeEnum == ContentTypeEnum.PICTURE) {
                String filePath = FileUtil.downloadImage(sendMessage.getMessageContent());
                message.setFilePath(filePath);
                message.setContent(null);
            }
            MessageTools.sendMsgByUserId(message);
        }
        return null;
    }


    /**
     * 自动回复
     *
     * @param text
     * @param msg
     * @return
     * @throws IOException
     */
    private List<Message> autoReply(String text, Message msg) throws IOException {
        return null;
    }


    /**
     * 图片消息(non-Javadoc)
     *
     * @see
     */
    @Override
    public List<Message> picMsgHandle(Message msg) {

        return dealNewMsg(msg);
    }


    /**
     * 语音消息(non-Javadoc)
     *
     * @see
     */
    @Override
    public List<Message> voiceMsgHandle(Message msg) {

        return dealNewMsg(msg);
    }


    @Override
    public List<Message> videoMsgHandle(Message msg) {

        return null;
    }


    @Override
    public List<Message> undoMsgHandle(Message msg) {
        return null;
    }


    @Override
    public List<Message> addFriendMsgHandle(Message msg) {

        return null;
    }


    @Override
    public List<Message> systemMsgHandle(Message msg) {
        return null;
    }


    @Override
    public List<Message> emotionMsgHandle(Message msg) {
        return dealNewMsg(msg);
    }


    @Override
    public List<Message> appMsgHandle(Message msg) {
        switch (WxRespConstant.WXReceiveMsgCodeOfAppEnum.getByCode(msg.getAppMsgType())) {
            case OTHER:
                break;
            case LINK:
                break;
            case FILE:
                break;
            case PROGRAM:
                break;
        }
        return null;
    }


    @Override
    public List<Message> verifyAddFriendMsgHandle(Message msg) {
        return null;
    }


    @Override
    public List<Message> mapMsgHandle(Message msg) {
        return null;
    }


    @Override
    public List<Message> nameCardMsgHandle(Message msg) {

        return null;
    }

}
