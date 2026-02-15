package com.derk.easyinventorycrafter.util;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SearchFilter {
	private SearchFilter() {
	}

	/**
	 * Filters a list of named entries by a search query (case-insensitive contains).
	 *
	 * @param names the list of item names
	 * @param query the search query (may be empty to return all)
	 * @return list of indices matching the query
	 */
	public static List<Integer> filterByName(List<String> names, String query) {
		String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		java.util.List<Integer> result = new java.util.ArrayList<>();
		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i).toLowerCase(Locale.ROOT);
			if (normalizedQuery.isEmpty() || name.contains(normalizedQuery)) {
				result.add(i);
			}
		}
		return result;
	}
}
