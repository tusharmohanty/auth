package com.portfolio.auth.controller;

import com.portfolio.auth.service.KiteAuthService;
import com.portfolio.auth.service.TokenService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth/kite")
public class KiteAuthController {

    private final KiteAuthService kiteAuthService;
    private final TokenService tokenService;

    @GetMapping("/login")
    public RedirectView login(@RequestParam(required = false) String returnTo, HttpSession session) {
        // only allow local paths to prevent open-redirect attacks
        if (returnTo != null && returnTo.startsWith("/")) {
            session.setAttribute("KITE_RETURN_TO", returnTo);
        }
        return new RedirectView(kiteAuthService.loginUrl());
    }

    @GetMapping("/callback")
    public Object callback(
            @RequestParam("request_token") String requestToken,
            HttpServletRequest request,
            HttpSession session
    ) {
        try {
            kiteAuthService.exchangeAndStore(requestToken);

            // ✅ redirect to where login started (fallback to "/")
            String returnTo = (String) session.getAttribute("KITE_RETURN_TO");
            session.removeAttribute("KITE_RETURN_TO");

            if (returnTo == null || !returnTo.startsWith("/")) {
                returnTo = "/";
            }

            if (looksLikeBrowser(request)) {
                return new RedirectView(returnTo);
            }

            return ResponseEntity.ok("Login successful. Token stored on castle.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Token exchange failed: " + e.getMessage());
        }
    }

    @GetMapping("/success")
    public ResponseEntity<?> success() {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Token stored successfully"));
    }

    /**
     * Main endpoint you asked for:
     * 1) If token exists for today AND is valid -> return it
     * 2) else if latest token exists AND is valid -> return it
     * 3) else force login (browser redirect) OR return loginUrl (API)
     */
    @GetMapping(value = "/token")
    public Object token(HttpServletRequest request) {

        // 1) Today token?
        var todayOpt = tokenService.findToday();
        if (todayOpt.isPresent()) {
            String token = todayOpt.get().getAccessToken();
            if (kiteAuthService.isTokenValid(token)) {
                return okToken(token);
            }
        }

        // 2) Latest token?
        var latestOpt = tokenService.findLatest();
        if (latestOpt.isPresent()) {
            String token = latestOpt.get().getAccessToken();
            if (kiteAuthService.isTokenValid(token)) {
                return okToken(token);
            }
        }

        // 3) Need login
        String loginUrl = kiteAuthService.loginUrl();

        if (looksLikeBrowser(request)) {
            // Redirect to our /login endpoint (which redirects to Kite)
            return new RedirectView("/auth/kite/login");
        }

        // API client: return login URL
        return ResponseEntity.status(409)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "error", "LOGIN_REQUIRED",
                        "loginUrl", loginUrl
                ));
    }

    private ResponseEntity<String> okToken(String token) {
        return ResponseEntity.ok(token);
    }

    private boolean looksLikeBrowser(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept == null) return false;
        return accept.contains("text/html") || accept.contains("application/xhtml+xml");
    }
}