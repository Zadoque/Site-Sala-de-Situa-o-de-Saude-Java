package com.example.nss.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nss.user.application.port.UserGateway;
import com.example.nss.user.domain.UserAccount;
import com.example.nss.user.web.mapper.UserMapper;
import com.example.nss.user.web.response.UserResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserServiceTest {
  private final UserGateway repository = mock(UserGateway.class);
  private final FirstAccessService firstAccess = mock(FirstAccessService.class);
  private final UserService service =
      new UserService(repository, firstAccess, Mappers.getMapper(UserMapper.class));

  @Test
  void findsUserByNormalizedEmailAsResponseAndEntity() {
    UserAccount user = user("ana@example.com", true);
    when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

    UserResponse response = service.getByEmail(" ANA@EXAMPLE.COM ");
    UserAccount entity = service.entityByEmail(" ANA@EXAMPLE.COM ");

    assertEquals(user.getEmail(), response.email());
    assertEquals(user, entity);
  }

  @Test
  void rejectsUnknownUser() {
    when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> service.getByEmail("missing@example.com"));
    assertThrows(
        IllegalArgumentException.class, () -> service.entityByEmail("missing@example.com"));
  }

  @Test
  void requestsResetOnlyForAnActiveUser() {
    UserAccount active = user("active@example.com", true);
    UserAccount inactive = user("inactive@example.com", false);
    when(repository.findByEmail("active@example.com")).thenReturn(Optional.of(active));
    when(repository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactive));

    service.requestPasswordReset("active@example.com");
    service.requestPasswordReset("inactive@example.com");

    verify(firstAccess).issuePasswordReset(active);
    verify(firstAccess, never()).issuePasswordReset(inactive);
  }

  private static UserAccount user(String email, boolean active) {
    UserAccount user = new UserAccount();
    user.setId(1L);
    user.setEmail(email);
    user.setNome("Ana");
    user.setAtivo(active);
    return user;
  }
}
