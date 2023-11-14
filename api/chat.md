## Chat Message

### JS to Java

```json
{
  "action": "sendMessage/request",
  "metadata": {
    "callback": "responseFunctionName",
    "parent": "b67937dd845afdf59dec505efafd2d18854cb2b147ae0cecd7f266d856373137"
  },
  "payload": {
    "context": [
      {
        "type": "code",
        "languageId": "python",
        "path": "xxx/a.py",
        "startLine": 1,
        "command": "",
        "content": "adkfjj\n"
      },
      {
        "type": "command",
        "languageId": "",
        "path": "",
        "startLine": 0,
        "command": "ls -l",
        "content": "adkfjj\n"
      }
    ],
    "message": "Hello"
  }
}
```

### Java to JS

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
