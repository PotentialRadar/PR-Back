package com.potential_radar.PR.search.repository;

import com.potential_radar.PR.search.document.ProjectSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectSearchRepository extends ElasticsearchRepository<ProjectSearchDocument, String> {

}
