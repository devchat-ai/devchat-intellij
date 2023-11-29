## Query Topic List

### JS to Java

```json
{
  "action": "listTopics/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": null
}
```

### Java to JS

```json
{
  "action": "listTopics/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": {
    "topics": [
      {
        "root_prompt": {
          "user": "Daniel Hu <tao.hu@merico.dev>",
          "date": 1698828624,
          "context": [
            {
              "content": "{\"languageId\":\"python\",\"path\":\"a.py\",\"startLine\":0,\"content\":\"adkfjj\\n\"}",
              "role": "system"
            }
          ],
          "request": "hello",
          "responses": [
            "Hi there! How can I assist you with Python today?"
          ],
          "hash": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b",
          "parent": null
        },
        "latest_time": 1698828867,
        "title": "hello - Hi there! How can I assist you with Python today?"
      }
    ]
  }
}
```

## Delete Topic

### JS to Java

```json
{
  "action": "deleteTopic/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": {
    "topicHash": "xxx"
  }
}
```

### Java to JS

- success

```json
{
  "action": "deleteTopic/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": {
    "topicHash": "xxx"
  }
}
```

- failed

```json
{
  "action": "deleteTopic/response",
  "metadata": {
    "status": "failed",
    "error": "xxx"
  },
  "payload": {
    "topicHash": "xxx"
  }
}
```

## Query Topic History Conversations

### JS to Java

```json
{
  "action": "listConversations/request",
  "metadata": {
    "topicHash": "xxx",
    "callback": "responseFunctionName"
  },
  "payload": null
}
```

### Java to JS

```json
{
  "action": "listConversations/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": {
    "conversations": [
      {
        "user": "Daniel Hu <tao.hu@merico.dev>",
        "date": 1686727177,
        "context": [
          {
            "content": "{\"command\":\"ls -l\",\"content\":\"total 8\\n-rw-r--r--@ 1 danielhu  staff  7 Nov  1 16:49 a.py\\n\"}",
            "role": "system"
          },
          {
            "content": "{\"languageId\":\"python\",\"path\":\"a.py\",\"startLine\":0,\"content\":\"adkfjj\\n\"}",
            "role": "system"
          }
        ],
        "request": "hello",
        "responses": [
          "world"
        ],
        "hash": "44871db06eaabbbeabaa262d1666481b7ea89ce6a4d30649cf3575fa13bf3c42",
        "parent": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b"
      }
    ]
  }
}
```

## Delete Last Conversation in One Topic

### JS to Java

```json
{
  "action": "deleteLastConversation/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": {
    "promptHash": "xxx"
  }
}
```

### Java to JS

- success

```json
{
  "action": "deleteLastConversation/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": {
    "promptHash": "xxx"
  }
}
```
