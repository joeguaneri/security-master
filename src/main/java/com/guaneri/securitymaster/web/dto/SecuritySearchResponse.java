package com.guaneri.securitymaster.web.dto;

import com.guaneri.securitymaster.domain.PagedResult;
import com.guaneri.securitymaster.domain.Security;
import java.util.List;

public record SecuritySearchResponse(List<SecurityResponse> items, int page, int size, long totalCount) {

  public static SecuritySearchResponse from(PagedResult<Security> page) {
    return new SecuritySearchResponse(
        page.items().stream().map(SecurityResponse::from).toList(), page.page(), page.size(), page.totalCount());
  }
}
