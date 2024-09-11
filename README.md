<p align="center">
        English</a>&nbsp ｜ &nbsp<a href="README_ZH.md">中文</a>
</p>

---
# MEMORY-CHAT
## About The Project
This system supports a variety of functions, including handling incoming files, replying to images, and unified processing of multiple messages. 
It also enables user-defined system messages, AI decision-making based on hot news, and exponential backoff for initiating messages. 
The system can respond to video messages, emoticons, and web searches, while also accepting friend requests. Additionally, it allows users to manage time plans, modify personalities, and proactively send messages.
Memory-related features include updating, organizing, and obsolescence, as well as learning new skills and persisting non-declarative memory. 
It supports personalized settings, a more colloquial style, and weather change perception.

- Implementing Memory Chat Using OpenAI LLM API
- The memory-chat-memory module is the entire logic content, and memory-chat-wechat is an example of the use of this module
<p align="center">
    <img src="Memory-chat.png" width="400"/>
<p>

## Access method
By calling CommonMessageHandler in your own code to push messages and register receiving methods, you can access all the functions of Memory-Chat
### Method 1

- If you use Java to write, introduce CommonMessageHandler in the code and call receiveNewMsg to send a message to get the return, registerSendTool to register the method to receive messages (optional)

### Method 2

- Provide the http method of the above steps at the same time, send a message through /chat/message to get the return, /chat/send/register to register the method to receive messages (optional)

## Scene

1. Imitate someone who has existed and chat with you or your own chat avatar
   There is getCustomChatModel in the code, which can generate a fine-tuning model that matches someone's style through a fine-tuning file

2. Custom chat partner
   Input /change ... can generate a chat model with specific role settings

3. Personal daily assistant
   Access through voice, WeChat or other chat methods

## Startup
- Copy the template.yaml file as application.yaml, modify the configuration, and run the Application class.
- After waiting for the project to start, scan the WeChat login QR code, add the logged-in WeChat account as a friend, and then chat.
    ### Required
    - Redis is needed to store data such as conversation status and conversation cache.
    - Required a github repository access token for picture bed.Or your custom image hosting address telegraph_url.
    - Weather and wolframalpha key are optional.
    - Required elasticsearch index:chat_memory to keep memory.
    ```json
    {
      "chat_memory": {
        "mappings": {
          "properties": {
            "aiResponseFlag": {
              "type": "keyword"
            },
            "chatMessage": {
              "type": "object"
            },
            "dealFileFlag": {
              "type": "boolean"
            },
            "emotion": {
              "type": "text",
              "fields": {
                "keyword": {
                  "type": "keyword",
                  "ignore_above": 256
                }
              }
            },
            "groupMsgFlag": {
              "type": "keyword"
            },
            "isDeleted": {
              "type": "text",
              "fields": {
                "keyword": {
                  "type": "keyword",
                  "ignore_above": 256
                }
              }
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
              }
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
            "summaryWords": {
              "type": "text",
              "fields": {
                "keyword": {
                  "type": "keyword",
                  "ignore_above": 256
                }
              }
            },
            "useToken": {
              "type": "integer"
            }
          }
        }
      }
    }
    ```


## Acknowledgements

### Special thanks to [Generative Agents](https://github.com/joonspk-research/generative_agents), without which this project would not have happened.

* Wechat Channel: https://github.com/yaphone/itchat4j
* Spring AI: https://github.com/spring-projects/spring-ai
* OpenAI: https://platform.openai.com/


