package com.castfan.dto;

import java.util.List;

/** One bounded result page and its count/navigation metadata. */
public record OwnerMemberDirectoryPage(List<OwnerMemberSearchResult> members, long totalResults,
	int currentPage, int totalPages, int pageSize) {
	public boolean hasPrevious() { return currentPage > 0; }
	public boolean hasNext() { return currentPage + 1 < totalPages; }
}
