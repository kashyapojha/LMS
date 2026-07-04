package com.geeknito.LMS_backend.repository;

import com.geeknito.LMS_backend.entity.media.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Long> {
}
