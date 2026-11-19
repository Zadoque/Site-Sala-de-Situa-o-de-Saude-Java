package com.example.nss.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nss.user.dto.mapper.UserMapper;
import com.example.nss.user.dto.request.AccountInvitationRequest;
import com.example.nss.user.dto.request.ActivationRequest;
import com.example.nss.user.dto.response.InvitationResponse;
import com.example.nss.user.dto.response.ManagedUserResponse;
import com.example.nss.user.dto.response.UserPageResponse;
import com.example.nss.user.entity.AccountInvitationCommand;
import com.example.nss.user.entity.AccountType;
import com.example.nss.user.entity.InvitationView;
import com.example.nss.user.entity.ManagedUserView;
import com.example.nss.user.entity.UserPageView;
import com.example.nss.user.service.AdminUserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {
  @Mock AdminUserService service;
  @Mock Authentication authentication;
  @Mock UserMapper mapper;
  @InjectMocks AdminUserController controller;

  @Test
  void searchesWithValidatedPagination() {
    UserPageView page = new UserPageView(List.of(), 0, 10, false, 0);
    when(service.search("ana", 0, 10)).thenReturn(page);
    when(mapper.toPageResponse(page)).thenReturn(new UserPageResponse(List.of(), 0, 10, false, 0));

    assertEquals(new UserPageResponse(List.of(), 0, 10, false, 0), controller.search("ana", 0, 10));
    verify(service).search("ana", 0, 10);
  }

  @Test
  void rejectsInvalidPaginationBeforeCallingService() {
    assertThrows(IllegalArgumentException.class, () -> controller.search(null, -1, 10));
    assertThrows(IllegalArgumentException.class, () -> controller.search(null, 0, 61));
  }

  @Test
  void createsInvitationWithCreatedStatus() {
    AccountInvitationRequest request =
        new AccountInvitationRequest("ana@example.com", AccountType.USER);
    ManagedUserView view =
        new ManagedUserView(1L, request.email(), "Ana", AccountType.USER, true, true, null);
    ManagedUserResponse response = mapper.toManagedResponse(view);
    when(service.invite(new AccountInvitationCommand(request.email(), request.accountType())))
        .thenReturn(view);
    when(mapper.toManagedResponse(view)).thenReturn(response);

    var result = controller.invite(request);

    assertEquals(HttpStatus.CREATED, result.getStatusCode());
    assertEquals(response, result.getBody());
  }

  @Test
  void delegatesActivationWithAuthenticatedActor() {
    ActivationRequest request = new ActivationRequest(false);
    ManagedUserView view =
        new ManagedUserView(1L, "ana@example.com", "Ana", AccountType.USER, false, false, null);
    ManagedUserResponse response = mapper.toManagedResponse(view);
    when(authentication.getName()).thenReturn("admin@example.com");
    when(service.setActive(1L, false, "admin@example.com")).thenReturn(view);
    when(mapper.toManagedResponse(view)).thenReturn(response);

    assertEquals(response, controller.activation(1L, request, authentication));
  }

  @Test
  void delegatesResetAndInvitationOperations() {
    InvitationView view =
        new InvitationView(2L, 1L, "ana@example.com", AccountType.USER, "PENDING", null, null);
    InvitationResponse invitation =
        new InvitationResponse(2L, 1L, "ana@example.com", AccountType.USER, "PENDING", null, null);
    when(service.invitations()).thenReturn(List.of(view));
    when(service.renewInvitation(2L)).thenReturn(view);
    when(mapper.toInvitationResponse(view)).thenReturn(invitation);

    controller.reset(1L);
    controller.cancel(2L);

    assertEquals(List.of(invitation), controller.invitations());
    assertEquals(invitation, controller.renew(2L));
    verify(service).resetPassword(1L);
    verify(service).cancelInvitation(2L);
  }
}
