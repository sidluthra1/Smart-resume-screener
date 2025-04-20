package com.yourname.backend.filters;

import com.yourname.backend.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * A Spring Security filter that intercepts incoming requests once.
 * It checks for a JWT in the Authorization header, validates it,
 * and sets the authentication context if the token is valid.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String path = request.getServletPath();

        // 1) Skip filtering for authentication endpoints (e.g., /auth/login, /auth/signup)
        //    These endpoints should not require a JWT.
        if (path.startsWith("/auth/")) {
            log.trace("Skipping JWT filter for authentication path: {}", path);
            filterChain.doFilter(request, response); // Continue to the next filter or controller
            return; // Stop processing this filter for this request
        }

        log.trace("Applying JWT filter for path: {}", path);

        // 2) Try to extract the JWT token from the "Authorization: Bearer <token>" header
        final String token = extractTokenFromHeader(request);
        String username = null;

        if (token != null) {
            try {
                // 3) Validate the token (checks signature and expiry) and get the username (subject)
                username = jwtUtil.validateTokenAndGetUsername(token);
                log.debug("JWT token validated successfully for user: {}", username);

            } catch (ExpiredJwtException ex) {
                log.warn("JWT token has expired: {}", ex.getMessage());
                // Token is expired. The request will proceed without authentication.
                // Subsequent security checks (e.g., @PreAuthorize) will likely deny access.
                // Alternatively, you could immediately send a 401 Unauthorized response:
                // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                // response.getWriter().write("{\"error\": \"Token expired\"}"); // Example JSON response
                // return;

            } catch (Exception ex) {
                // Handles various other JWT exceptions (malformed, signature invalid, etc.)
                log.error("Invalid JWT token: {}", ex.getMessage());
                // Token is invalid. Proceed without authentication or send 401.
            }
        } else {
            // No token found in the header for a path that requires authentication.
            log.warn("Authorization header missing or does not start with 'Bearer ' for path: {}", path);
        }

        // 4) If a valid username was extracted and the user is not already authenticated
        //    in the current security context, set the authentication.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Create an authentication token.
            // In a real application, you would typically load UserDetails (including roles/authorities)
            // from a UserDetailsService based on the username extracted from the token.
            // For this example, we'll create a simple token without authorities.
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username, // The principal (who the user is)
                    null,     // Credentials (password) - not needed for token-based auth
                    Collections.emptyList() // Authorities (roles/permissions) - should be loaded
            );

            // Set additional details from the request (e.g., IP address, session ID)
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set the authentication token in the SecurityContext. Spring Security will now
            // recognize the user as authenticated for the duration of the request.
            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("User '{}' authenticated via JWT and SecurityContext updated.", username);

        } else {
            if (username == null) {
                log.trace("No valid JWT token found or token invalid/expired, proceeding without setting SecurityContext for path: {}", path);
            } else {
                // This means SecurityContextHolder already has an Authentication object,
                // possibly set by a previous filter (e.g., session management).
                log.trace("User '{}' already authenticated via other means, proceeding.", username);
            }
        }

        // 5) Continue the filter chain. The request proceeds to the next filter or the target controller.
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization header.
     * Expects the header format: "Authorization: Bearer <token>"
     *
     * @param request The incoming HttpServletRequest.
     * @return The JWT token string (without the "Bearer " prefix) or null if the header
     * is missing, malformed, or doesn't start with "Bearer ".
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            // Return the token part after "Bearer "
            return authHeader.substring(7);
        }
        // No valid Bearer token found
        return null;
    }
}