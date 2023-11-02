package ai.devchat.devchat.handler;

import ai.devchat.cli.DevChatWrapper;
import ai.devchat.common.DevChatPathUtil;
import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class ListConversationsRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public ListConversationsRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling list conversations request");

        String callbackFunc = metadata.getString("callback");
        String topicHash = metadata.getString("topicHash");
        try {
            DevChatWrapper devchatWrapper = new DevChatWrapper(DevChatPathUtil.getDevchatBinPath());
            /* conversations' format:
            [
              {
                "user": "Daniel Hu <tao.hu@merico.dev>",
                "date": 1698828867,
                "context": [
                  {
                    "content": "{\"command\":\"ls -l\",\"content\":\"total 8\\n-rw-r--r--@ 1 danielhu  staff  7 Nov  1 16:49 a.py\\n\"}",
                    "role": "system"
                  }
                ],
                "request": "hello again",
                "responses": [
                  "Hello! How can I assist you today?"
                ],
                "request_tokens": 135,
                "response_tokens": 19,
                "hash": "ccbbd3f8c892277d3ea566545bb64b68ba3e34257f9b324551a52449f8f19e17",
                "parent": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b"
              },
              {
                "user": "Daniel Hu <tao.hu@merico.dev>",
                "date": 1698828624,
                "context": [
                  {
                    "content": "{\"languageId\":\"python\",\"path\":\"a.py\",\"startLine\":0,\"content\":\"adkfjj\\n\"}",
                    "role": "system"
                  }
                ],
                "request": "hello",
                "responses": [
                  "Hi there! How can I assist you with Python today?"
                ],
                "request_tokens": 46,
                "response_tokens": 22,
                "hash": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b",
                "parent": null
              }
            ]
            */
            JSONArray conversations = devchatWrapper.listConversationsInOneTopic(topicHash);
            // remove request_tokens and response_tokens in the conversations object
            for (int i = 0; i < conversations.size(); i++) {
                JSONObject conversation = conversations.getJSONObject(i);
                conversation.remove("request_tokens");
                conversation.remove("response_tokens");
            }

            devChatActionHandler.sendResponse(DevChatActions.LIST_CONVERSATIONS_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");

                payload.put("conversations", conversations);
            });
        } catch (Exception e) {
            Log.error("Exception occrred while executing DevChat command. Exception message: " + e.getMessage());

            devChatActionHandler.sendResponse(DevChatActions.LIST_CONVERSATIONS_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "error");
                metadata.put("error", e.getMessage());
            });
        }
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
