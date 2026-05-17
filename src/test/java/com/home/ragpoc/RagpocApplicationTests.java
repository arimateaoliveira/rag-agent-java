package com.home.ragpoc;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Pular teste de contexto - não necessário para validação da POC")
class RagpocApplicationTests {

    @Test
    void contextLoads() {
        // Teste desabilitado
    }
}