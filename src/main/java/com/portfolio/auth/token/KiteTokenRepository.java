package com.portfolio.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface KiteTokenRepository extends JpaRepository<KiteToken, Long> {

    Optional<KiteToken> findByCreatedDate(LocalDate createdDate);

    @Query("select t from KiteToken t order by t.createdDate desc, t.id desc limit 1")
    Optional<KiteToken> findLatest();
}