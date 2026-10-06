package com.example.demo.service;

import com.example.demo.entity.RefreshSession;
import com.example.demo.entity.User;
import com.example.demo.repository.RefreshSessionRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshSessionServiceTest {
    @Test
    void rejectsRefreshForAnInactiveUserAndRevokesTheStoredSession() {
        RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
        UserRepository users = mock(UserRepository.class);
        RefreshSessionService service = new RefreshSessionService(sessions, users);
        setExpiration(service, 60_000L);
        User user = new User();
        user.setAtivo(false);
        RefreshSession session = new RefreshSession();
        session.setUser(user);
        session.setExpiresAt(Instant.now().plusSeconds(60));
        when(sessions.findByTokenHashForUpdate(any())).thenReturn(Optional.of(session));

        assertThrows(BadCredentialsException.class, () -> service.rotateWithToken("opaque-refresh"));
        verify(sessions).save(session);
    }

    private void setExpiration(RefreshSessionService service, long value) {
        try {
            var field = RefreshSessionService.class.getDeclaredField("expirationMillis");
            field.setAccessible(true);
            field.set(service, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
