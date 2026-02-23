package com.portfolio.auth.service;

import com.portfolio.auth.token.KiteToken;
import com.portfolio.auth.token.KiteTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final KiteTokenRepository repo;

    @Transactional
    public void save(String accessToken) {
        KiteToken t = new KiteToken();
        t.setAccessToken(accessToken);
        t.setCreatedDate(LocalDate.now());
        repo.save(t);
    }

    @Transactional(readOnly = true)
    public Optional<KiteToken> findToday() {
        return repo.findByCreatedDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Optional<KiteToken> findLatest() {
        return repo.findLatest();
    }

    @Transactional(readOnly = true)
    public String getActiveToken() {
        return repo.findByCreatedDate(LocalDate.now())
                .or(() -> repo.findLatest())
                .orElseThrow(() -> new RuntimeException("No token found. Login required."))
                .getAccessToken();
    }
}