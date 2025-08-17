package com.potential_radar.PR.search.repository;

import com.potential_radar.PR.search.document.SearchHistoryDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends ElasticsearchRepository<SearchHistoryDocument, String> {
    List<SearchHistoryDocument> findByUserIdOrderBySearchTimeDesc(Long userId);
    List<SearchHistoryDocument> findTop10ByUserIdOrderBySearchTimeDesc(Long userId);
    List<SearchHistoryDocument> findBySearchTypeOrderBySearchTimeDesc(String searchType);
}