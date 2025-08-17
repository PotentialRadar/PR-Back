package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.document.SearchHistoryDocument;
import com.potential_radar.PR.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    public void saveSearchHistory(Long userId, String searchType, String searchQuery, 
                                Integer resultCount, String userAgent, String ipAddress) {
        try {
            SearchHistoryDocument history = SearchHistoryDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .searchType(searchType)
                    .searchQuery(searchQuery)
                    .resultCount(resultCount)
                    .searchTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .userAgent(userAgent)
                    .ipAddress(ipAddress)
                    .build();

            searchHistoryRepository.save(history);
            log.info("Search history saved for user: {}, query: {}", userId, searchQuery);
        } catch (Exception e) {
            log.error("Failed to save search history for user: {}", userId, e);
        }
    }

    @Cacheable(value = "searchHistory", key = "#userId + '_recent'")
    public List<SearchHistoryDocument> getRecentSearchHistory(Long userId) {
        return searchHistoryRepository.findTop10ByUserIdOrderBySearchTimeDesc(userId);
    }

    public List<SearchHistoryDocument> getAllUserSearchHistory(Long userId) {
        return searchHistoryRepository.findByUserIdOrderBySearchTimeDesc(userId);
    }

    public List<SearchHistoryDocument> getSearchHistoryByType(String searchType) {
        return searchHistoryRepository.findBySearchTypeOrderBySearchTimeDesc(searchType);
    }

    public void deleteUserSearchHistory(Long userId) {
        try {
            List<SearchHistoryDocument> userHistory = searchHistoryRepository.findByUserIdOrderBySearchTimeDesc(userId);
            searchHistoryRepository.deleteAll(userHistory);
            log.info("Deleted search history for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete search history for user: {}", userId, e);
        }
    }
}