package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectAttachmentRepository extends JpaRepository<ProjectAttachment, Long> {
    @Modifying
    @Query("DELETE FROM ProjectAttachment pa WHERE pa.project.projectId = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
