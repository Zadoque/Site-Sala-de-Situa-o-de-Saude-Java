package com.example.nss.DTO.mapper;

import com.example.nss.DTO.response.ManagedUserResponse;
import com.example.nss.DTO.response.UserResponse;
import com.example.nss.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserResponse toResponse(User user);

  @Mapping(target = "name", source = "user.nome")
  @Mapping(target = "active", source = "user.ativo")
  @Mapping(target = "firstAccessPending", source = "firstAccessPending")
  ManagedUserResponse toManagedResponse(User user, boolean firstAccessPending);
}
