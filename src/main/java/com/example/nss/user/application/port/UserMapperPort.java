package com.example.nss.user.application.port;

import com.example.nss.user.application.query.ManagedUserView;
import com.example.nss.user.application.query.UserView;
import com.example.nss.user.infrastructure.persistence.User;

public interface UserMapperPort {
  UserView toResponse(User user);
  ManagedUserView toManagedResponse(User user, boolean firstAccessPending);
}
