package com.example.nss.user.entity;

import java.util.List;

public record UserPageView(
    List<ManagedUserView> items, int page, int size, boolean hasNext, long totalItems) {}
