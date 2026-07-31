package com.example.aics.repository;

import com.example.aics.entity.ScriptFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScriptFavoriteRepository extends JpaRepository<ScriptFavorite, Long> {

    long countByStaffId(Long staffId);

    List<ScriptFavorite> findByStaffId(Long staffId);

    Optional<ScriptFavorite> findByStaffIdAndSourceTalkId(Long staffId, String sourceTalkId);

    Optional<ScriptFavorite> findByStaffIdAndContentHash(Long staffId, String contentHash);
}
