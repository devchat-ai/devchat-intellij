## Set or Update key

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
