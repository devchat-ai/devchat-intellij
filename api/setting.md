## Get Setting

### JS to Java

```json
{
  "action": "getSetting/request",
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
  "action": "getSetting/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": {
    "setting": {
      "apiKey": "DC.abcdef123456",
      "apiBase": "https://api.example.com/v1",
      "currentModel": "gpt-4"
    }
  }
}
```

- failed

```json
{
  "action": "getSetting/response",
  "metadata": {
    "status": "Failed",
    "error": "Failed to get setting."
  },
  "payload": null
}
```

## Set or Update Setting

### JS to Java

```json
{
  "action": "updateSetting/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": {
    "setting": {
      "currentModel": "gpt-3.5-turbo"
    }
  }
}
```

### Java to JS

- success

```json
{
  "action": "updateSetting/response",
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
  "action": "updateSetting/response",
  "metadata": {
    "status": "Failed",
    "error": "Failed to update setting."
  },
  "payload": null
}
```
