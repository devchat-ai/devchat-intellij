# JS <--> Java

## Set or Update key

### Request

```json
{
  "action": "setOrUpdateKey/request",
  "metadata": null,
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
