package com.home.ragpoc.controller;

import com.home.ragpoc.model.ChatRequest;
import com.home.ragpoc.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    @PostMapping
    public ResponseEntity<Map<String, String>> perguntar(@RequestBody ChatRequest request) {
        String resposta = ragService.perguntar(request.getPergunta());
        return ResponseEntity.ok(Map.of("resposta", resposta));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RAG POC está rodando!");
    }
}