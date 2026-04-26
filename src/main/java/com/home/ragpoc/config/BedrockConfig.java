package com.home.ragpoc.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class BedrockConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.bedrock.model-id}")
    private String modelId;

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public ChatLanguageModel chatModel(BedrockRuntimeClient client) {
        return new BedrockConverseChatModel(client, modelId);
    }

    @Bean
    public EmbeddingModel embeddingModel(BedrockRuntimeClient client) {
        return new BedrockCohereEmbeddingModel(client, "cohere.embed-english-v3");
    }
}