## Commit

### JS to Java

```json
{
  "action": "commitCode/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": {
    "message": "Commit message"
  }
}
```

### Java to JS

- success

```json
{
  "action": "commitCode/response",
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
  "action": "commitCode/response",
  "metadata": {
    "status": "Failed",
    "error": "Failed to commit."
  },
  "payload": null
}
```
