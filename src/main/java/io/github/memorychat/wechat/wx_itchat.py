import base64
import os

import itchat
import requests
from itchat import content
from concurrent.futures import ThreadPoolExecutor


class Message:
    def __init__(self, creator_name, content_type, message_content):
        self.messageCreatorName = creator_name
        self.messageContentType = content_type
        self.messageContent = message_content

    def to_dict(self):
        return {
            "messageCreatorName": self.messageCreatorName,
            "messageContentType": self.messageContentType,
            "messageContent": self.messageContent
        }


pool = ThreadPoolExecutor(max_workers=20)


def message_handler(msg):
    try:
        msg_type = msg['MsgType']
        if msg['ToUserName'] != msg['User']['UserName']:
            creator_name = msg['User']['NickName']
            remark_name = msg['User']['RemarkName']
            if msg_type == 1:
                content_type = 'TEXT'
            elif msg_type == 3:
                content_type = 'PICTURE'
            elif msg_type == 34:
                content_type = 'AUDIO'
            else:
                return
            if msg_type == 3 or msg_type == 34:
                image_file = msg['Text']
                image_file((msg['FileName']))
                try:
                    with open(msg['FileName'], 'rb') as f:
                        file_data = f.read()
                        content_text = base64.b64encode(file_data).decode('utf-8')
                finally:
                    # Delete the local file after processing
                    if os.path.exists(msg['FileName']):
                        os.remove(msg['FileName'])
            else:
                content_text = msg['Text']
            message = Message(
                creator_name=remark_name if remark_name else creator_name,
                content_type=content_type,
                message_content=content_text
            )

            # 将消息体转换为字典
            message_data = message.to_dict()

            # 发送消息到服务器
            server_url = 'http://localhost:8080/chat/wechat'
            response = requests.post(server_url, json=message_data)

            # 处理服务器响应
            failFlag = True
            if response.status_code == 200:
                # 解析JSON格式的响应体
                try:
                    json_data = response.json()
                    if json_data and json_data.get('messageContent'):
                        failFlag = False
                        if json_data.get('messageType') == 'TEXT':
                            itchat.send(json_data.get('messageContent'), toUserName=msg['FromUserName'])
                        elif json_data.get('messageType') == 'PICTURE':
                            itchat.send_msg(json_data.get('messageContent'), toUserName=msg['FromUserName'])
                except ValueError:
                    print("响应不是有效的JSON格式")
            if failFlag:
                print("消息处理失败或者是消息叠加了")
    except Exception as ex:
        print(ex)


@itchat.msg_register([content.TEXT, content.PICTURE, content.VOICE], isFriendChat=True)
def handle_msg(msg):
    pool.submit(message_handler, msg)


def login_wx():
    itchat.auto_login(
        hotReload=True,
        # hotReload = True, 保持在线，下次运行代码可自动登录,可以添加enableCmdQR=True参数，让二维码显示到命令行上，另外部分系统可能字符宽度有出入，可以通过把enableCmdQR赋值为特定的倍数进行调整。如设置值为2
        exitCallback=login_wx)
    itchat.run()


if __name__ == '__main__':
    try:
        login_wx()
    except Exception as e:
        print(e)
