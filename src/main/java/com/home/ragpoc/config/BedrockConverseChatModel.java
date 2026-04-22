package com.home.ragpoc.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;

public class BedrockConverseChatModel implements ChatLanguageModel {

    private final BedrockRuntimeClient client;
    private final String modelId;

    public BedrockConverseChatModel(BedrockRuntimeClient client, String modelId) {
        this.client = client;
        this.modelId = modelId;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        // Pega a última mensagem do usuário
        String userMessage = messages.get(messages.size() - 1).text();

        Message message = Message.builder()
                .role("user")
                .content(ContentBlock.fromText(userMessage))
                .build();

        ConverseRequest request = ConverseRequest.builder()
                .modelId(modelId)
                .messages(message)
                .build();

        ConverseResponse response = client.converse(request);
        String responseText = response.output().message().content().get(0).text();

        return Response.from(AiMessage.from(responseText));
    }
}