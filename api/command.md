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
