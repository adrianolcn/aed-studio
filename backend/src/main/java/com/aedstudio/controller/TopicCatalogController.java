package com.aedstudio.controller;

import com.aedstudio.dto.TopicCatalogResponse;
import com.aedstudio.service.TopicCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class TopicCatalogController {

    private final TopicCatalog topicCatalog;

    @GetMapping("/topics")
    public ResponseEntity<TopicCatalogResponse> topics() {
        return ResponseEntity.ok(TopicCatalogResponse.builder()
                .totalTopics(topicCatalog.totalTopics())
                .tracks(topicCatalog.tracks())
                .topics(topicCatalog.topics())
                .build());
    }
}
