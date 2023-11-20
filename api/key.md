## Set or Update Key

### JS to Java

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

### Java to JS

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

## Get Key

### JS to Java

```json
{
  "action": "getKey/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": null
}
```

### Java to JS

- success

```json
{
  "action": "getKey/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": {
    "key": "DC.abcdef123456"
  }
}
```

- failed

```json
{
  "action": "getKey/response",
  "metadata": {
    "status": "Failed",
    "error": "Failed to get key."
  },
  "payload": null
}
```
