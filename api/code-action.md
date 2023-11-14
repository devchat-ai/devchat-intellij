## Insert Code

### JS to Java

```json
{
  "action": "insertCode/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": {
    "content": "adkfjj\n"
  }
}
```

### Java to JS

```json
{
  "action": "insertCode/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": null
}
```

## Replace All Content

### JS to Java

```json
{
  "action": "replaceFileContent/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": {
    "content": "adkfjj\n"
  }
}
```

### Java to JS

```json
{
  "action": "replaceFileContent/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": null
}
```

## View Diff

### JS to Java

```json
{
  "action": "viewDiff/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": {
    "content": "package ai.devchat"
  }
}
```
