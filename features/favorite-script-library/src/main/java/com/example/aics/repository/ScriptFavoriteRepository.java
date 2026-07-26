package com.example.aics.repository;

import com.example.aics.entity.ScriptFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScriptFavoriteRepository extends JpaRepository<ScriptFavorite, UUID> {

    long countByStaffId(String staffId);

    List<ScriptFavorite> findByStaffId(String staffId);

    Optional<ScriptFavorite> findByStaffIdAndSourceTalkId(String staffId, String sourceTalkId);

    Optional<ScriptFavorite> findByStaffIdAndContentHash(String staffId, String contentHash);
}
