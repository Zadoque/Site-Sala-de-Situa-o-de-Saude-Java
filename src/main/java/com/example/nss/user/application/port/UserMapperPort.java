package com.example.nss.user.application.port;

import com.example.nss.user.application.query.ManagedUserView;
import com.example.nss.user.application.query.UserView;
import com.example.nss.user.domain.UserAccount;

public interface UserMapperPort {
  UserView toResponse(UserAccount user);

  ManagedUserView toManagedResponse(UserAccount user, boolean firstAccessPending);
}
