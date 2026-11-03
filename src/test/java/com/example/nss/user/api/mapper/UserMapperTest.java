package com.example.nss.user.web.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.nss.user.domain.AccountType;
import com.example.nss.user.domain.UserAccount;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserMapperTest {

  private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

  @Test
  void mapsUserToPublicResponse() {
    UserAccount user = user();

    var response = mapper.toResponse(user);

    assertEquals(user.getId(), response.id());
    assertEquals(user.getNome(), response.nome());
    assertEquals(user.getEmail(), response.email());
    assertTrue(response.ativo());
  }

  @Test
  void mapsUserToManagedResponse() {
    UserAccount user = user();

    var response = mapper.toManagedResponse(user, true);

    assertEquals(user.getId(), response.id());
    assertEquals(user.getNome(), response.name());
    assertEquals(user.getAccountType(), response.accountType());
    assertTrue(response.active());
    assertTrue(response.firstAccessPending());
  }

  private UserAccount user() {
    UserAccount user = new UserAccount();
    user.setId(7L);
    user.setNome("Ana Saúde");
    user.setEmail("ana@example.com");
    user.setAccountType(AccountType.ADMIN);
    user.setAtivo(true);
    return user;
  }
}
