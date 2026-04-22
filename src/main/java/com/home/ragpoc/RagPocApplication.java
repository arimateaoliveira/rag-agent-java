package com.home.ragpoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RagPocApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagPocApplication.class, args);
        System.out.println("RAG POC iniciada! Acesse: http://localhost:8080");
    }
}