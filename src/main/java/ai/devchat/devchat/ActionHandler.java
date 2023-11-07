package ai.devchat.devchat;

import com.alibaba.fastjson.JSONObject;

public interface ActionHandler {
    void setMetadata(JSONObject metadata);
    void setPayload(JSONObject payload);
    void executeAction();
}
