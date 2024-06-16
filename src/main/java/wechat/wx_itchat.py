import base64
import os

import itchat
import requests
from itchat import content


# 定义消息体的类
class Message:
    def __init__(self, creator_name, content_type, content):
        self.messageCreatorName = creator_name
        self.messageContentType = content_type
        self.messageContent = content

    def to_dict(self):
        return {
            # "messageCreatorId": self.messageCreatorId,
            "messageCreatorName": self.messageCreatorName,
            # "messageReceiveId": self.messageReceiveId,
            # "messageReceiveName": self.messageReceiveName,
            "messageContentType": self.messageContentType,
            "messageContent": self.messageContent
        }


@itchat.msg_register([content.TEXT, content.PICTURE], isFriendChat=True)
def handle_msg(msg):
    try:
        msg_type = msg['MsgType']
        if msg['ToUserName'] != msg['User']['UserName']:
            # 构造消息体
            # print("get other msg" + json.dumps(msg))
            creator_id = msg['FromUserName']
            creator_name = msg['User']['NickName']
            remark_name = msg['User']['RemarkName']
            receiver_id = msg['ToUserName']
            receiver_name = 'Andrew'
            content_type = 'TEXT' if msg_type == 1 else 'PICTURE'
            if msg_type == 3:
                image_file = msg['Text']
                image_file((msg['FileName']))
                try:
                    with open(msg['FileName'], 'rb') as f:
                        image_data = f.read()
                        content_text = base64.b64encode(image_data).decode('utf-8')
                finally:
                    # Delete the local image file after processing
                    if os.path.exists(msg['FileName']):
                        os.remove(msg['FileName'])
            else:
                content_text = msg['Text']
            message = Message(
                # creator_id=creator_id,
                creator_name=remark_name if remark_name else creator_name,
                # receiver_id=receiver_id,
                # receiver_name=receiver_name,  # 机器人的昵称
                content_type=content_type,  # 假设消息内容类型为文本
                content=content_text
            )

            # 将消息体转换为字典
            message_data = message.to_dict()

            # 发送消息到服务器
            server_url = 'http://localhost:8080/chat/wechat'  # 替换为你的服务器API端点
            response = requests.post(server_url, json=message_data)

            # 处理服务器响应
            if response.status_code == 200 and response.text:
                itchat.send(response.text, toUserName=msg['FromUserName'])
            else:
                print("消息处理失败或者是消息叠加了")
    except Exception as e:
        print(e)


def login_wx():
    itchat.auto_login(
        hotReload=True,
        exitCallback=login_wx)  # hotReload = True, 保持在线，下次运行代码可自动登录,可以添加enableCmdQR=True参数，让二维码显示到命令行上，另外部分系统可能字符宽度有出入，可以通过把enableCmdQR赋值为特定的倍数进行调整。如设置值为2
    itchat.run(blockThread=False)


if __name__ == '__main__':
    try:
        login_wx()
        while True:
            pass
    except Exception as e:
        print(e)
