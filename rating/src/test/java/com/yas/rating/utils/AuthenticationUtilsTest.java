package com.yas.rating.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticationUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractUserId_whenAuthenticated_returnUserId() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-id-123");
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        String userId = AuthenticationUtils.extractUserId();
        assertThat(userId).isEqualTo("user-id-123");
    }

    @Test
    void extractUserId_whenAnonymous_throwAccessDeniedException() {
        AnonymousAuthenticationToken authentication = mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertThrows(AccessDeniedException.class, AuthenticationUtils::extractUserId);
    }

    @Test
    void testPrivateConstructor() throws Exception {
        java.lang.reflect.Constructor<AuthenticationUtils> constructor = AuthenticationUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        AuthenticationUtils instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
