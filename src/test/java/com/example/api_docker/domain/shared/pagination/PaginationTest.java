package com.example.api_docker.domain.shared.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginationTest {

    @Test
    @DisplayName("Deve aplicar valores padrão e travas de segurança no PaginationRequest")
    void shouldApplyDefaultsAndSafetyLimitsOnPaginationRequest() {
        PaginationRequest negativePage = PaginationRequest.of(-1, -5);
        assertEquals(0, negativePage.page());
        assertEquals(10, negativePage.size());
        assertEquals("ASC", negativePage.sortDirection());

        PaginationRequest excessiveSize = PaginationRequest.of(2, 500, "name", "desc");
        assertEquals(2, excessiveSize.page());
        assertEquals(100, excessiveSize.size());
        assertEquals("name", excessiveSize.sortBy());
        assertEquals("DESC", excessiveSize.sortDirection());

        PaginationRequest invalidDirection = PaginationRequest.of(0, 10, "email", "UNKNOWN");
        assertEquals("ASC", invalidDirection.sortDirection());
    }

    @Test
    @DisplayName("Deve calcular corretamente os metadados de paginação no PageResult")
    void shouldCalculateMetadataCorrectlyOnPageResult() {
        List<String> items = List.of("A", "B", "C");
        PageResult<String> firstPage = new PageResult<>(items, 0, 3, 10L, 4);

        assertTrue(firstPage.isFirst());
        assertFalse(firstPage.isLast());
        assertTrue(firstPage.hasNext());
        assertFalse(firstPage.hasPrevious());
        assertEquals(3, firstPage.items().size());

        PageResult<String> lastPage = new PageResult<>(items, 3, 3, 10L, 4);
        assertFalse(lastPage.isFirst());
        assertTrue(lastPage.isLast());
        assertFalse(lastPage.hasNext());
        assertTrue(lastPage.hasPrevious());

        // Teste do método .map()
        PageResult<Integer> mapped = firstPage.map(String::length);
        assertEquals(List.of(1, 1, 1), mapped.items());
        assertEquals(firstPage.page(), mapped.page());
        assertEquals(firstPage.totalElements(), mapped.totalElements());
    }

    @Test
    @DisplayName("Deve instanciar página vazia corretamente")
    void shouldCreateEmptyPageResult() {
        PaginationRequest req = PaginationRequest.of(0, 10);
        PageResult<String> empty = PageResult.empty(req);

        assertTrue(empty.items().isEmpty());
        assertEquals(0, empty.page());
        assertEquals(10, empty.size());
        assertEquals(0L, empty.totalElements());
        assertEquals(0, empty.totalPages());
        assertTrue(empty.isFirst());
        assertTrue(empty.isLast());
    }
}
