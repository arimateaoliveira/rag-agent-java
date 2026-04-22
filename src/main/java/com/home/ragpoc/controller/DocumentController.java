package com.home.ragpoc.controller;

import com.home.ragpoc.service.RagService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentController {

    private final RagService ragService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // Parse diretamente do InputStream
            Document document = new ApachePdfBoxDocumentParser().parse(file.getInputStream());
            document.metadata().put("fileName", file.getOriginalFilename());

            // Indexa no banco de dados
            ragService.indexarDocumento(document);

            return ResponseEntity.ok("✅ Documento indexado com sucesso: " + file.getOriginalFilename());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("❌ Erro ao processar: " + e.getMessage());
        }
    }
}