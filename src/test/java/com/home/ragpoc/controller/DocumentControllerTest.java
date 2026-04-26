package com.home.ragpoc.controller;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import com.home.ragpoc.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private RagService ragService;

    @Mock
    private S3Client s3Client;

    @Mock
    private DocumentParser documentParser;

    private DocumentController documentController;

    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        documentController = new DocumentController(ragService, s3Client, documentParser);
        file = new MockMultipartFile(
                "file",
                "curriculo.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "conteudo do pdf".getBytes()
        );
    }

    @Test
    void testUploadDocument_Success() throws Exception {
        byte[] pdfBytes = file.getBytes();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        ResponseInputStream<GetObjectResponse> mockStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(pdfBytes)));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);

        when(documentParser.parse(any())).thenReturn(Document.from("conteudo"));
        doNothing().when(ragService).indexarDocumento(any());

        var response = documentController.uploadDocument(file);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("✅"));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Client).getObject(any(GetObjectRequest.class));
        verify(ragService).indexarDocumento(any());
    }

    @Test
    void testUploadDocument_ErroProcessamento() throws Exception {
        // Arrange
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("Erro S3"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> documentController.uploadDocument(file));
        verify(ragService, never()).indexarDocumento(any());
    }
}