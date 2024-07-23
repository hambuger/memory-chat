# memory-chat
a chat bot with long memory,with Java.

启动参数：
-Dhttps.proxyHost=127.0.0.1 
-Dhttps.proxyPort=7890 
-DOPENAI_API_KEY=sk-xxx

需要的es索引
```json
{
  "chat_memory": {
    "mappings": {
      "properties": {
        "aiResponseFlag": {
          "type": "keyword"
        },
        "groupMsgFlag": {
          "type": "keyword"
        },
        "memoryLeafDepth": {
          "type": "integer"
        },
        "messageContent": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          },
          "analyzer": "ik_max_word"
        },
        "messageContentType": {
          "type": "keyword"
        },
        "messageContentVector": {
          "type": "dense_vector",
          "dims": 1536
        },
        "messageCreateAt": {
          "type": "date",
          "format": "yyyy-MM-dd HH:mm:ss"
        },
        "messageCreatorId": {
          "type": "keyword"
        },
        "messageCreatorName": {
          "type": "keyword"
        },
        "messageCreatorType": {
          "type": "keyword"
        },
        "messageId": {
          "type": "keyword"
        },
        "messageImportanceScore": {
          "type": "double"
        },
        "messageLastAccessTime": {
          "type": "date",
          "format": "yyyy-MM-dd HH:mm:ss"
        },
        "messageOwnerId": {
          "type": "keyword"
        },
        "messageOwnerName": {
          "type": "keyword"
        },
        "messageOwnerType": {
          "type": "keyword"
        },
        "messageParentIds": {
          "type": "keyword"
        },
        "messageReceiveId": {
          "type": "keyword"
        },
        "messageReceiveName": {
          "type": "keyword"
        },
        "messageReceiveType": {
          "type": "keyword"
        },
        "realCreatorId": {
          "type": "keyword"
        },
        "realCreatorName": {
          "type": "keyword"
        },
        "useToken": {
          "type": "integer"
        }
      }
    }
  }
}
```

### Todo

- [ ] 1.支持传入文件
传入的文件场景应该是什么？
文件修改还是文件理解，后续两者都支持

- [x] 2.支持回复图片
考虑加入function_call来解决1和2

- [x] 3.有可能一个消息回触发多条回复
需要提供一个send方法以供调用

- [ ] 4.支持微信语音发送
itchat办不到，只能通过wechaty,但它收费
如果要使用wechaty考虑接入openai audio或者chattts

- [ ] 5.要支持用户自定义system message

- [x] 6.支持系统消息处理

- [x] 7.支持ai决定是否回复

- [x] 8.使用Java版本itchat4j

- [x] 9.指数退避去检查是否要发起消息

- [x] 10.支持video消息回复
- 
- [x] 11.支持网页搜索
- 
- [x] 12.支持回复表情

- [x] 13.支持接受好友请求

- [ ] 14.规划时间计划，修改计划，修改个性，主动发起消息

记忆更新逻辑
夜间记忆整理
记忆过时，重要性更新
创建记忆时，分类，如短期，长期。方便后续整理记忆。

