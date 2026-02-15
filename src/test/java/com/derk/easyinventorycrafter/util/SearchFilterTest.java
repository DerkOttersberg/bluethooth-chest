package com.derk.easyinventorycrafter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class SearchFilterTest {

    @Test
    void emptyQuery_returnsAllIndices() {
        List<String> names = Arrays.asList("Oak Log", "Iron Ore", "Diamond");
        List<Integer> result = SearchFilter.filterByName(names, "");
        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    void nullQuery_returnsAllIndices() {
        List<String> names = Arrays.asList("Oak Log", "Iron Ore");
        List<Integer> result = SearchFilter.filterByName(names, null);
        assertEquals(Arrays.asList(0, 1), result);
    }

    @Test
    void caseInsensitiveMatch() {
        List<String> names = Arrays.asList("Oak Log", "Iron Ore", "Diamond Pickaxe");
        List<Integer> result = SearchFilter.filterByName(names, "OAK");
        assertEquals(Collections.singletonList(0), result);
    }

    @Test
    void partialMatch() {
        List<String> names = Arrays.asList("Oak Log", "Dark Oak Planks", "Iron Ore");
        List<Integer> result = SearchFilter.filterByName(names, "oa");
        assertEquals(Arrays.asList(0, 1), result);
    }

    @Test
    void noMatch_returnsEmpty() {
        List<String> names = Arrays.asList("Oak Log", "Iron Ore", "Diamond");
        List<Integer> result = SearchFilter.filterByName(names, "emerald");
        assertTrue(result.isEmpty());
    }

    @Test
    void emptyList_returnsEmpty() {
        List<String> names = Collections.emptyList();
        List<Integer> result = SearchFilter.filterByName(names, "oak");
        assertTrue(result.isEmpty());
    }

    @Test
    void whitespaceQuery_treatedAsEmpty() {
        List<String> names = Arrays.asList("Oak Log", "Iron Ore");
        List<Integer> result = SearchFilter.filterByName(names, "   ");
        assertEquals(Arrays.asList(0, 1), result);
    }
}
