package com.aedstudio.dto;

import com.aedstudio.service.TopicCatalog.TopicDefinition;
import com.aedstudio.service.TopicCatalog.TrackDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicCatalogResponse {
    private Integer totalTopics;
    private List<TrackDefinition> tracks;
    private List<TopicDefinition> topics;
}
