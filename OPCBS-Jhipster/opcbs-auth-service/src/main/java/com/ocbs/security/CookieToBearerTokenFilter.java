package com.ocbs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter tự động trích xuất Token từ HttpOnly Cookie và chuyển đổi thành Header 'Authorization: Bearer <token>'
 * theo chuẩn BFF (Backend-For-Frontend) và Enterprise HttpOnly Architecture.
 * 
 * =========================================================================
 * VAI TRÒ VÀ CƠ CHẾ:
 * 1. Khi trình duyệt gửi request mang HttpOnly Cookie ('jhi-authenticationToken' hoặc 'id_token'),
 *    nếu request CHƯA có header 'Authorization' (do JavaScript không đọc được HttpOnly cookie):
 *    -> Filter này sẽ tự động đọc Cookie, bọc (wrap) request và inject header 'Authorization: Bearer <token>'.
 * 
 * 2. Nhờ đó, toàn bộ chuỗi bảo mật Spring Security (OAuth2 Resource Server / JwtDecoder) bên dưới
 *    hoạt động mượt mà mà không cần chỉnh sửa bất kỳ logic xác thực lõi nào!
 * 
 * 3. Hoạt động song song và tương thích 100% với Nginx Gateway BFF (Nếu Nginx đã inject Header thì Filter
 *    giữ nguyên; nếu chạy trực tiếp / test dev thì Filter tự động bọc).
 * =========================================================================
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class CookieToBearerTokenFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CookieToBearerTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String existingAuth = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Nếu request chưa có Authorization Header thì tìm kiếm trong HttpOnly Cookie
        if (existingAuth == null || existingAuth.isBlank() || !existingAuth.startsWith("Bearer ")) {
            String tokenFromCookie = extractTokenFromCookie(request);
            if (tokenFromCookie != null && !tokenFromCookie.isBlank()) {
                final String bearerHeader = "Bearer " + tokenFromCookie.trim();

                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                            return bearerHeader;
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                            return Collections.enumeration(List.of(bearerHeader));
                        }
                        return super.getHeaders(name);
                    }
                };

                LOG.debug(">>> [BFF FILTER] Tự động chuyển đổi HttpOnly Cookie thành Authorization Header cho URI: {}", request.getRequestURI());
                filterChain.doFilter(wrappedRequest, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("jhi-authenticationToken".equals(cookie.getName()) || "id_token".equals(cookie.getName())) {
                String val = cookie.getValue();
                if (val != null && !val.isBlank()) {
                    return val;
                }
            }
        }
        return null;
    }
}
