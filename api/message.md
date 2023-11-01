# JS <--> Java

## Set or Update key

### Request

```json
{
  "action": "setOrUpdateKey/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": {
    "key": "DC.abcdef123456"
  }
}
```

### Response

- success

```json
{
  "action": "setOrUpdateKey/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": null
}
```

- failed

```json
{
  "action": "setOrUpdateKey/response",
  "metadata": {
    "status": "Failed",
    "error": "Failed to set key."
  },
  "payload": null
}
```

## Chat Message

### Request

```json
{
  "action": "sendMessage/request",
  "metadata": {
    "callback": "responseFunctionName",
    "parent": "b67937dd845afdf59dec505efafd2d18854cb2b147ae0cecd7f266d856373137"
  },
  "payload": {
    "context": "Hello!",
    "message": "Hello"
  }
}
```

### Response

- success

**round 1**

```json
{
  "action": "sendMessage/response",
  "metadata": {
    "currentChunkId": 1,
    "isFinalChunk": false,
    "finishReason": "",
    "error": ""
  },
  "payload": {
    "message": "Hello!",
    "user": "Daniel Hu <tao.hu@merico.dev>",
    "date": "Wed Oct 18 09:02:42 2023 +0800",
    "promptHash": ""
  }
}
```

**round 2**

```json
{
  "action": "sendMessage/response",
  "metadata": {
    "currentChunkId": 2,
    "isFinalChunk": false,
    "finishReason": "",
    "error": ""
  },
  "payload": {
    "message": "Hello!\nHow can I assist you?",
    "user": "Daniel Hu <tao.hu@merico.dev>",
    "date": "Wed Oct 18 09:02:42 2023 +0800",
    "promptHash": ""
  }
}
```

**round 3**

```json
{
  "action": "sendMessage/response",
  "metadata": {
    "currentChunkId": 3,
    "isFinalChunk": true,
    "finishReason": "success",
    "error": ""
  },
  "payload": {
    "message": "Hello!\nHow can I assist you?",
    "user": "Daniel Hu <tao.hu@merico.dev>",
    "date": "Wed Oct 18 09:02:42 2023 +0800",
    "promptHash": "b67937dd845afdf59dec505efafd2d18854cb2b147ae0cecd7f266d856373137"
  }
}
```

- failed

```json
{
  "action": "sendMessage/response",
  "metadata": {
    "currentChunkId": 0,
    "isFinalChunk": true,
    "finishReason": "error",
    "error": "Network error."
  },
  "payload": null
}
```

## Add Context(Add to DevChat)

### Java to JS

```json
{
  "action": "addContext/request",
  "metadata": null,
  "payload": {
    "file": "/Users/xxx/yyy/Hello.java",
    "content": "public static void main(..."
  }
}
```

### JS to Java

```json
{
  "action": "addContext/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": null
}
```

```json
{
  "action": "addContext/response",
  "metadata": {
    "status": "error",
    "error": "some error message here"
  },
  "payload": null
}
```

## Query Command List

### JS to Java

```json
{
  "action": "listCommands/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": null
}
```

### Java to JS

```json
{
  "action": "listCommands/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": {
    "commands": [
      {
        "name": "code",
        "description": "Generate code with a general template embedded into the prompt."
      },
      {
        "name": "release_note",
        "description": "Generate a release note for the given commit log."
      }
    ]
  }
}
```

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
