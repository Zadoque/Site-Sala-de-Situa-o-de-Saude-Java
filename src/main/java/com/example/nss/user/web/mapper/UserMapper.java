package com.example.nss.user.web.mapper;

import com.example.nss.user.infrastructure.persistence.User;
import com.example.nss.user.application.port.UserMapperPort;
import com.example.nss.user.web.response.ManagedUserResponse;
import com.example.nss.user.web.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper extends UserMapperPort {

  UserResponse toResponse(User user);

  @Mapping(target = "name", source = "user.nome")
  @Mapping(target = "active", source = "user.ativo")
  @Mapping(target = "firstAccessPending", source = "firstAccessPending")
  ManagedUserResponse toManagedResponse(User user, boolean firstAccessPending);
}
