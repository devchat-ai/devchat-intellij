package ai.devchat.devchat.handler;

import ai.devchat.cli.DevChatWrapper;
import ai.devchat.common.DevChatPathUtil;
import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;
import ai.devchat.idea.storage.DeletedTopicsState;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public class ListTopicsRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public ListTopicsRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling list topics request");

        String callbackFunc = metadata.getString("callback");
        try {
            DevChatWrapper devchatWrapper = new DevChatWrapper(DevChatPathUtil.getDevchatBinPath());
            /*
             * topics format:
             * [
             * {
             * "root_prompt": {
             * "user": "Daniel Hu <tao.hu@merico.dev>",
             * "date": 1698828624,
             * "context": [
             * {
             * "content":
             * "{\"languageId\":\"python\",\"path\":\"a.py\",\"startLine\":0,\"content\":\"adkfjj\\n\"}",
             * "role": "system"
             * }
             * ],
             * "request": "hello",
             * "responses": [
             * "Hi there! How can I assist you with Python today?"
             * ],
             * "request_tokens": 46,
             * "response_tokens": 22,
             * "hash": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b",
             * "parent": null
             * },
             * "latest_time": 1698828867,
             * "title": null,
             * "hidden": false
             * }
             * ]
             */
            JSONArray topics = devchatWrapper.listTopics();

            // Get deleted topics hash list
            DeletedTopicsState deletedTopicsState = DeletedTopicsState.getInstance();
            List<String> deletedTopicHashes = deletedTopicsState.deletedTopicHashes;

            // Filter out deleted topics
            JSONArray filteredTopics = new JSONArray();
            // remove request_tokens and response_tokens in the topics object, then update
            // title field.
            for (int i = 0; i < topics.size(); i++) {
                JSONObject topic = topics.getJSONObject(i);
                JSONObject rootPrompt = topic.getJSONObject("root_prompt");
                String topicHash = rootPrompt.getString("hash");

                if (!deletedTopicHashes.contains(topicHash)) {
                    // set title = root_prompt.request + "-" + root_prompt.responses[0]
                    String title = rootPrompt.getString("request") + "-"
                            + rootPrompt.getJSONArray("responses").getString(0);
                    rootPrompt.put("title", title);

                    filteredTopics.add(topic);
                }
            }

            devChatActionHandler.sendResponse(DevChatActions.LIST_TOPICS_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");

                payload.put("topics", topics);
            });
        } catch (Exception e) {
            Log.error("Exception occrred while executing DevChat command. Exception message: " + e.getMessage());

            devChatActionHandler.sendResponse(DevChatActions.LIST_TOPICS_RESPONSE, callbackFunc, (metadata, payload) -> {
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
