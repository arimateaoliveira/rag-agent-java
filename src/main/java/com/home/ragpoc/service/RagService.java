package com.home.ragpoc.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final PgVectorEmbeddingStore vectorStore;

    public String perguntar(String pergunta) {
        log.info("📝 Processando pergunta: {}", pergunta);

        // 1. Gera embedding da pergunta
        Embedding queryEmbedding = embeddingModel.embed(pergunta).content();
        log.info("✅ Embedding gerado - Dimensão: {}", queryEmbedding.dimension());

        // 2. Busca documentos relevantes no vector store
        List<EmbeddingMatch<TextSegment>> matches = vectorStore.findRelevant(queryEmbedding, 5);
        log.info("📚 Documentos relevantes encontrados: {}", matches.size());

        if (matches.isEmpty()) {
            log.warn("⚠️ Nenhum documento relevante encontrado. Usando conhecimento geral.");
            return chatModel.generate(pergunta);
        }

        // 3. Constrói contexto
        String contexto = matches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n"));

        log.info("📖 Contexto construído com {} caracteres", contexto.length());

        // 4. Prompt final com contexto
        String prompt = String.format("""
            Responda a pergunta abaixo baseado APENAS no contexto fornecido.
            Se a resposta não estiver no contexto, responda APENAS: "Não encontrei essa informação no currículo."
            
            Contexto:
            %s
            
            Pergunta: %s
            Resposta: 
            """, contexto, pergunta);

        // 5. Gera resposta
        String resposta = chatModel.generate(prompt);
        log.info("💬 Resposta gerada com {} caracteres", resposta.length());

        return resposta;
    }

    public void indexarDocumento(Document document) {
        log.info("Indexando documento: {}", document.metadata().getString("fileName"));

        // Divide em segmentos de 500 caracteres com 100 de sobreposição
        var splitter = DocumentSplitters.recursive(1500, 200);
        List<TextSegment> segments = splitter.split(document);

        // Gera embeddings para todos os segmentos
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // Armazena no pgvector
        for (int i = 0; i < segments.size(); i++) {
            vectorStore.add(embeddings.get(i), segments.get(i));
        }

        log.info("✅ Documento indexado com {} segmentos", segments.size());
    }
}