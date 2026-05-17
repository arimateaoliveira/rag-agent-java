package com.home.ragpoc.controller;

import com.home.ragpoc.service.RagService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/documentos")
public class DocumentController {

    private final RagService ragService;
    private final S3Client s3Client;
    private final DocumentParser documentParser;

    @Autowired
    public DocumentController(RagService ragService, S3Client s3Client) {
        this(ragService, s3Client, new ApachePdfBoxDocumentParser());
    }

    public DocumentController(RagService ragService, S3Client s3Client, DocumentParser documentParser) {
        this.ragService = ragService;
        this.s3Client = s3Client;
        this.documentParser = documentParser;
    }

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String s3Key = "uploads/" + System.currentTimeMillis() + "_" + fileName;

            // 1. Upload para o S3
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
            System.out.println("✅ Arquivo salvo no S3: " + s3Key);

            // 2. Baixar do S3 para indexar
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            byte[] fileBytes = s3Client.getObject(getRequest).readAllBytes();

            // 3. Parse do PDF e indexação
            Document document = documentParser.parse(new ByteArrayInputStream(fileBytes));

            document.metadata().put("fileName", fileName);
            document.metadata().put("s3Key", s3Key);

            ragService.indexarDocumento(document);

            return ResponseEntity.ok("✅ Documento indexado com sucesso!\n" +
                    "   Arquivo: " + fileName + "\n" +
                    "   S3 Key: " + s3Key);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("❌ Erro ao processar: " + e.getMessage());
        }
    }
}