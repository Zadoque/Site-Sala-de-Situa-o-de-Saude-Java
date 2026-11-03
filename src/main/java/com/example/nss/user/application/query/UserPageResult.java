package com.example.nss.user.application.query;

import java.util.List;

public record UserPageResult(
    List<? extends ManagedUserView> items, int page, int size, boolean hasNext, long totalItems)
    implements UserPageView {}
