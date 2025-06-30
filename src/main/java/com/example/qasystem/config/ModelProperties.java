package com.example.qasystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "spring.ai")
public class ModelProperties {
    private OpenAIConfig openai;     // 对应DeepSeek配置
    private DashscopeConfig dashscope; // 对应Qwen配置
    private ZhipuaiConfig zhipuai;    // 对应智谱配置

    // OpenAI兼容配置类（用于DeepSeek）
    @Data
    public static class OpenAIConfig {
        private String baseUrl;
        private String apiKey;
        private ChatConfig chat;
    }

    // 阿里云DashScope配置类（用于Qwen）
    @Data
    public static class DashscopeConfig {
        private String baseUrl;
        private String apiKey;
        private ChatConfig chat;
    }

    // 智谱AI配置类
    @Data
    public static class ZhipuaiConfig {
        private String baseUrl;
        private String apiKey;
        private ChatConfig chat;
    }

    // 聊天配置通用结构
    @Data
    public static class ChatConfig {
        private Options options;
    }

    // 模型选项配置
    @Data
    public static class Options {
        private String model;
        private Double temperature;
        private Integer maxTokens;
    }
}
