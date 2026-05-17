package com.home.ragpoc.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import dev.langchain4j.data.document.Document;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private PgVectorEmbeddingStore vectorStore;

    @Mock
    private ChatLanguageModel chatModel;

    @InjectMocks
    private RagService ragService;

    private Embedding mockEmbedding;
    private TextSegment mockTextSegment;

    @BeforeEach
    void setUp() {
        // Configura objetos mock
        float[] mockVector = new float[1024];
        mockVector[0] = 0.1f;
        mockEmbedding = Embedding.from(mockVector);

        mockTextSegment = TextSegment.from("Experiência com AWS Lambda, EC2, S3, DynamoDB, Kinesis, SQS, SNS e outras tecnologias cloud.");
    }

    @Test
    void testPerguntar_ComDocumentosRelevantes_DeveRetornarRespostaDoContexto() {
        // Arrange
        String pergunta = "Qual minha experiência com AWS?";

        // Mock do embedding da pergunta
        Response<Embedding> embedResponse = Response.from(mockEmbedding);
        when(embeddingModel.embed(pergunta)).thenReturn(embedResponse);

        // Mock da busca no vector store
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.95, "1", mockEmbedding, mockTextSegment);
        when(vectorStore.findRelevant(eq(mockEmbedding), eq(5))).thenReturn(List.of(match));

        // Mock da resposta do chat
        String respostaEsperada = "Experiência com AWS Lambda, EC2, S3, DynamoDB, Kinesis, SQS, SNS e outras tecnologias cloud.";
        when(chatModel.generate(anyString())).thenReturn(respostaEsperada);

        // Act
        String resultado = ragService.perguntar(pergunta);

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.contains("Lambda") || resultado.contains("EC2"));
        verify(embeddingModel, times(1)).embed(pergunta);
        verify(vectorStore, times(1)).findRelevant(eq(mockEmbedding), eq(5));
        verify(chatModel, times(1)).generate(anyString());
    }

    @Test
    void testPerguntar_SemDocumentosRelevantes_DeveUsarConhecimentoGeral() {
        // Arrange
        String pergunta = "O que é programação?";

        // Mock do embedding da pergunta
        Response<Embedding> embedResponse = Response.from(mockEmbedding);
        when(embeddingModel.embed(pergunta)).thenReturn(embedResponse);

        // Mock da busca vazia no vector store
        when(vectorStore.findRelevant(eq(mockEmbedding), eq(5))).thenReturn(Collections.emptyList());

        // Mock da resposta do chat (conhecimento geral)
        String respostaEsperada = "Programação é o processo de criar instruções para computadores.";
        when(chatModel.generate(pergunta)).thenReturn(respostaEsperada);

        // Act
        String resultado = ragService.perguntar(pergunta);

        // Assert
        assertNotNull(resultado);
        assertEquals(respostaEsperada, resultado);
        verify(embeddingModel, times(1)).embed(pergunta);
        verify(vectorStore, times(1)).findRelevant(eq(mockEmbedding), eq(5));
        verify(chatModel, times(1)).generate(pergunta);
    }

    @Test
    void testPerguntar_QuandoEmbeddingFalha_DeveLancarExcecao() {
        // Arrange
        String pergunta = "Pergunta de teste";

        // Mock de falha no embedding
        when(embeddingModel.embed(pergunta)).thenThrow(new RuntimeException("Erro ao gerar embedding"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ragService.perguntar(pergunta);
        });

        assertTrue(exception.getMessage().contains("Erro ao gerar embedding"));
        verify(embeddingModel, times(1)).embed(pergunta);
        verify(vectorStore, never()).findRelevant(any(), anyInt());
        verify(chatModel, never()).generate(anyString());
    }

    @Test
    void testIndexarDocumento_DeveSalvarSegmentosEembeddings() throws Exception {
        // Arrange
        // Cria um documento com conteúdo real
        dev.langchain4j.data.document.Document document = dev.langchain4j.data.document.Document.from(
                "Este é um documento de teste com conteúdo sobre AWS Lambda, EC2, S3 e DynamoDB."
        );

        // Adiciona metadata
        document.metadata().put("fileName", "teste.pdf");

        // Mock dos embeddings
        when(embeddingModel.embedAll(anyList())).thenReturn(Response.from(List.of(mockEmbedding, mockEmbedding)));

        // Act
        ragService.indexarDocumento(document);

        // Assert
        verify(vectorStore, atLeastOnce()).add(any(Embedding.class), any(TextSegment.class));
    }
}