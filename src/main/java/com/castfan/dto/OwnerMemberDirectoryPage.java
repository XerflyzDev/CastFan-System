package com.castfan.dto;

import java.util.List;

/** One bounded result page and its count/navigation metadata. */
public record OwnerMemberDirectoryPage(List<OwnerMemberSearchResult> members, long totalResults,
	int currentPage, int totalPages, int pageSize) {
	public boolean hasPrevious() { return currentPage > 0; }
	public boolean hasNext() { return currentPage + 1 < totalPages; }
	public long firstResult() { return totalResults == 0 ? 0 : (long) currentPage * pageSize + 1; }
	public long lastResult() { return Math.min((long) (currentPage + 1) * pageSize, totalResults); }
}
