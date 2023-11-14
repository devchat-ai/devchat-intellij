## List models

### JS to Java

```json
{
  "action": "listModels/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": null
}
```

### Java to JS

```json
{
  "action": "listModels/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": {
    "models": [
      "gpt-3.5-turbo",
      "gpt-4"
    ]
  }
}
```
