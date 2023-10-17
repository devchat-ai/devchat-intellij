package ai.devchat.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.util.Map;

/*
 * default_model: gpt-3.5-turbo
 * models:
 *   gpt-3.5-turbo:
 *     provider: devchat.ai
 *     stream: true
 */
public class DevChatConfig {
    private String default_model;
    private Map<String, ModelConfig> models;

    static class ModelConfig {
        private String provider;
        private boolean stream;

        // getters and setters
        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public boolean isStream() {
            return stream;
        }

        public void setStream(boolean stream) {
            this.stream = stream;
        }
    }


    //Getters and Setters
    public String getDefault_model() {
        return default_model;
    }

    public void setDefault_model(String default_model) {
        this.default_model = default_model;
    }

    public Map<String, ModelConfig> getModels() {
        return models;
    }

    public void setModels(Map<String, ModelConfig> models) {
        this.models = models;
    }

    public static DevChatConfig load(String path) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            DevChatConfig config = mapper.readValue(new File(path), DevChatConfig.class);
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    public void save(String path) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.writeValue(new File(path), this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }
}
