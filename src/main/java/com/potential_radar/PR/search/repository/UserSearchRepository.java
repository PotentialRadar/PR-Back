package com.potential_radar.PR.search.repository;

import com.potential_radar.PR.search.document.UserSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSearchRepository extends ElasticsearchRepository<UserSearchDocument, String> {
    Iterable<UserSearchDocument> findByNickname(String nickname);
    Iterable<UserSearchDocument> findByNicknameContaining(String nickname);
    Iterable<UserSearchDocument> findByTechPart(String techPart);
    Iterable<UserSearchDocument> findByTechStacksContaining(String techStack);
    Iterable<UserSearchDocument> findByExperienceRange(String experienceRange);
    void deleteByUserId(Long userId);
}
