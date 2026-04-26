package com.home.ragpoc.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import java.util.ArrayList;
import java.util.List;

public class BedrockCohereEmbeddingModel implements EmbeddingModel {

    private final BedrockRuntimeClient client;
    private final String modelId;
    private final Gson gson = new Gson();

    public BedrockCohereEmbeddingModel(BedrockRuntimeClient client, String modelId) {
        this.client = client;
        this.modelId = modelId;
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<String> texts = new ArrayList<>();
        for (TextSegment segment : textSegments) {
            texts.add(segment.text());
        }

        JsonObject requestBody = new JsonObject();
        JsonArray textsArray = new JsonArray();
        for (String text : texts) {
            textsArray.add(text);
        }
        requestBody.add("texts", textsArray);
        requestBody.addProperty("input_type", "search_document");

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromUtf8String(gson.toJson(requestBody)))
                .build();

        InvokeModelResponse response = client.invokeModel(request);
        String responseBody = response.body().asUtf8String();

        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
        JsonArray embeddingsArray = jsonResponse.getAsJsonArray("embeddings");

        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < embeddingsArray.size(); i++) {
            JsonArray embeddingArray = embeddingsArray.get(i).getAsJsonArray();
            float[] floatArray = new float[embeddingArray.size()];
            for (int j = 0; j < embeddingArray.size(); j++) {
                floatArray[j] = embeddingArray.get(j).getAsFloat();
            }
            embeddings.add(Embedding.from(floatArray));
        }

        return Response.from(embeddings);
    }
}