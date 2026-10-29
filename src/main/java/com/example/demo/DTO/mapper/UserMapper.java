package com.example.demo.DTO.mapper;

import com.example.demo.DTO.response.ManagedUserResponse;
import com.example.demo.DTO.response.UserResponse;
import com.example.demo.entity.User;
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
