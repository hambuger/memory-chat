# MEMORY-CHAT
## A chat bot named Andrew with the ability to remember, plan, and learn.
### Special thanks to [Generative Agents](https://github.com/joonspk-research/generative_agents), without which this project would not have happened.
- Implementing AI Chat Using OpenAI API,Using SpringAi as the java framework.
- Using itchat4j code to implement WeChat as chat interface.


## Startup
- If a proxy is required, JVM startup parameters: -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890
- Copy the template.yaml file as application.ymal, modify the configuration, and run the Application class.
- After waiting for the project to start, scan the WeChat login QR code, add the logged-in WeChat account as a friend, and then chat.
    ### Required
    - Redis is needed to store data such as conversation status and conversation cache.
    - Required elasticsearch index:chat_memory to keep memory.
    - Required a github repository access token for picture bed.
    - Weather and wolframalpha key are optional.
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


### Features List

- [x] Supports incoming files

- [x] Supports replying to images

- [x] Unified processing of multiple messages

- [x] Supports user-defined system messages

- [x] Supports AI to decide whether to reply, and initiate messages based on hot news

- [x] Exponential backoff to check whether to initiate a message

- [x] Supports video message reply

- [x] Supports web search

- [x] Supports replying emoticons

- [x] Supports accepting friend requests

- [x] Plan time plans, modify plans, modify personality, and actively initiate messages

- [x] Memory update logic, memory organization, and memory obsolescence.

- [x] AI learns new skills and persists

- [x] Personalized modification settings

- [x] Learning and persisting non-declarative memory

- [x] More colloquial

- [x] Weather change perception






