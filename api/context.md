## Add Context(Add to DevChat)

### Java to JS (notify)

```json
{
  "action": "addContext/notify",
  "metadata": null,
  "payload": {
    "path": "src/main/Hello.java",
    "startLine": 1,
    "languageId": "Java",
    "content": "public static void main(..."
  }
}
```

## List Contexts

### JS to Java (command)

```json
{
  "action": "listContexts/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": null
}
```

### Java to JS

```json
{
  "action": "listContexts/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": {
    "contexts": [
      {
        "command": "git diff -cached",
        "description": "xxx."
      },
      {
        "command": "git diff HEAD",
        "description": "xxx."
      }
    ]
  }
}
```

## Get Context with Command

### JS to Java

```json
{
  "action": "addContext/request",
  "metadata": {
    "callback": "responseFunctionName"
  },
  "payload": {
    "command": "git diff --cached"
  }
}
```

### Java to JS

```json
{
  "action": "addContext/response",
  "metadata": {
    "status": "success",
    "error": ""
  },
  "payload": {
    "command": "git diff --cached",
    "content": "xxx"
  }
}
```
