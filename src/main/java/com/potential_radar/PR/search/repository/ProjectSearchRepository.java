package com.potential_radar.PR.search.repository;

import com.potential_radar.PR.search.document.ProjectSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectSearchRepository extends ElasticsearchRepository<ProjectSearchDocument, String> {
    List<ProjectSearchDocument> findByTechStacksIn(List<String> techStacks);
    List<ProjectSearchDocument> findByTechStacksContaining(String techStack);
    List<ProjectSearchDocument> findByTechPartsIn(List<String> techParts);
    void deleteByProjectId(Long projectId);
}