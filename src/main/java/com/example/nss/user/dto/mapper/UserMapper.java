package com.example.nss.user.dto.mapper;

import com.example.nss.user.dto.response.InvitationResponse;
import com.example.nss.user.dto.response.ManagedUserResponse;
import com.example.nss.user.dto.response.UserPageResponse;
import com.example.nss.user.dto.response.UserResponse;
import com.example.nss.user.entity.InvitationView;
import com.example.nss.user.entity.ManagedUserView;
import com.example.nss.user.entity.User;
import com.example.nss.user.entity.UserPageView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserResponse toResponse(User user);

  @Mapping(target = "name", source = "user.nome")
  @Mapping(target = "active", source = "user.ativo")
  @Mapping(target = "firstAccessPending", source = "firstAccessPending")
  ManagedUserResponse toManagedResponse(User user, boolean firstAccessPending);

  default ManagedUserResponse toManagedResponse(ManagedUserView value) {
    return new ManagedUserResponse(
        value.id(),
        value.email(),
        value.name(),
        value.accountType(),
        value.active(),
        value.firstAccessPending(),
        value.passwordCreatedAt());
  }

  default UserPageResponse toPageResponse(UserPageView value) {
    return new UserPageResponse(
        value.items().stream().map(this::toManagedResponse).toList(),
        value.page(),
        value.size(),
        value.hasNext(),
        value.totalItems());
  }

  default InvitationResponse toInvitationResponse(InvitationView value) {
    return new InvitationResponse(
        value.id(),
        value.userId(),
        value.email(),
        value.accountType(),
        value.status(),
        value.sentAt(),
        value.expiresAt());
  }
}
