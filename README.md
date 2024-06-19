# memory-chat
a chat bot with long memory,with Java.

启动参数：
-Dhttps.proxyHost=127.0.0.1 
-Dhttps.proxyPort=7890 
-DOPENAI_API_KEY=sk-xxx
TODO:
1.支持传入文件
传入的文件场景应该是什么？
文件修改还是文件理解，后续两者都支持

2.支持回复图片

考虑加入function_call来解决1和2

3.有可能一个消息回触发多条回复
需要提供一个send方法以供调用

4.支持微信语音发送
itchat办不到，只能通过wechaty,但它收费
如果要使用wechaty考虑接入openai audio或者chattts

5.要支持用户自定义system message

6.支持系统消息处理

7.支持ai决定是否回复

8.使用Java版本itchat4j