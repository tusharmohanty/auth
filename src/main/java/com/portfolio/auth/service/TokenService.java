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

    /**
     * Stores today's access token, overwriting any token already held for today.
     *
     * <p>Update-in-place, not insert. A Kite access token is valid for exactly one trading day, so
     * "one row per calendar day" is the real invariant and a re-login is an overwrite, not a new
     * fact. This used to insert blindly, which on 2026-08-13 put two rows on the same
     * {@code created_date} and broke the whole integration for a day: {@link #findToday()} is typed
     * {@link Optional}, so the second row made it throw {@code NonUniqueResultException}, taking
     * {@code GET /auth/kite/token} to HTTP 500 until midnight. The portfolio EOD job catches that as
     * "Kite unavailable" and continues, so it silently synced neither trades nor holdings.
     *
     * <p>{@code portfolio.kite_tokens} now carries a unique index on {@code created_date}
     * (migration V254 in the portfolio-backend repo), so a regression here fails loudly at the
     * database instead of silently degrading the next day's EOD run.
     */
    @Transactional
    public void save(String accessToken) {
        LocalDate today = LocalDate.now();
        KiteToken t = repo.findByCreatedDate(today).orElseGet(KiteToken::new);
        t.setAccessToken(accessToken);
        t.setCreatedDate(today);
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