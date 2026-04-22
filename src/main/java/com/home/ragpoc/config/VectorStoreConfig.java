package com.home.ragpoc.config;

import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUser;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(databaseUrl);
        dataSource.setUsername(databaseUser);
        dataSource.setPassword(databasePassword);
        return dataSource;
    }

    @Bean
    public PgVectorEmbeddingStore vectorStore() {
        return PgVectorEmbeddingStore.builder()
                .host(extractHost(databaseUrl))
                .port(extractPort(databaseUrl))
                .database(extractDatabase(databaseUrl))
                .user(databaseUser)
                .password(databasePassword)
                .table("embeddings")
                .dimension(1024)
                .build();
    }

    private String extractHost(String url) {
        return url.split("//")[1].split(":")[0];
    }

    private int extractPort(String url) {
        String hostPort = url.split("//")[1].split("/")[0];
        return Integer.parseInt(hostPort.split(":")[1]);
    }

    private String extractDatabase(String url) {
        return url.split("/")[3];
    }

}