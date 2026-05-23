package com.blue.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-context integration tests for catalog-service.
 *
 * Test environment:
 *   - Embedded H2 in-memory database (independent from production data).
 *   - Schema created by Hibernate (ddl-auto: create-drop) at context startup.
 *   - Test data loaded from src/test/resources/catalog-test-data.sql via the
 *     spring.sql.init.data-locations override (replaces the production data.sql).
 *
 * Seed: 4 categories, 14 products (5 Books / 5 Coffee Mugs / 2 Mouse Pads / 2 Luggage Tags).
 */
@SpringBootTest(properties = {
        "spring.sql.init.data-locations=classpath:catalog-test-data.sql"
})
@AutoConfigureMockMvc
class CatalogServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ─── Product Categories ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/product-category")
    class GetProductCategories {

        @Test
        @DisplayName("returns 200 with all 4 seeded categories")
        void returns200WithAllCategories() throws Exception {
            mockMvc.perform(get("/api/product-category"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.productCategory").isArray())
                    .andExpect(jsonPath("$._embedded.productCategory", hasSize(4)));
        }

        @Test
        @DisplayName("every category exposes id and categoryName required by the sidebar nav")
        void everyCategoryHasRequiredFields() throws Exception {
            mockMvc.perform(get("/api/product-category"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.productCategory[*].id",
                            everyItem(notNullValue())))
                    .andExpect(jsonPath("$._embedded.productCategory[*].categoryName",
                            everyItem(notNullValue())));
        }

        @Test
        @DisplayName("returns exactly the four seeded category names")
        void containsExactlySeededCategoryNames() throws Exception {
            mockMvc.perform(get("/api/product-category"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.productCategory[*].categoryName",
                            containsInAnyOrder("Books", "Coffee Mugs", "Mouse Pads", "Luggage Tags")));
        }
    }

    // ─── Products collection with pagination ─────────────────────────────────

    @Nested
    @DisplayName("GET /api/products")
    class GetProducts {

        @Test
        @DisplayName("returns 200 with a page of products and correct HAL structure")
        void returns200WithPagedProducts() throws Exception {
            mockMvc.perform(get("/api/products").param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.products").isArray())
                    .andExpect(jsonPath("$._embedded.products", hasSize(14)));
        }

        @Test
        @DisplayName("pagination block carries size / totalElements / totalPages / number")
        void paginationMetadataIsComplete() throws Exception {
            mockMvc.perform(get("/api/products")
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.size").value(5))
                    .andExpect(jsonPath("$.page.totalElements").value(14))
                    .andExpect(jsonPath("$.page.totalPages").value(3))
                    .andExpect(jsonPath("$.page.number").value(0));
        }

        @Test
        @DisplayName("every product in the collection exposes all cart-required fields")
        void everyProductExposesCartRequiredFields() throws Exception {
            mockMvc.perform(get("/api/products").param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.products[*].id",
                            everyItem(notNullValue())))
                    .andExpect(jsonPath("$._embedded.products[*].sku",
                            everyItem(notNullValue())))
                    .andExpect(jsonPath("$._embedded.products[*].name",
                            everyItem(notNullValue())))
                    .andExpect(jsonPath("$._embedded.products[*].unitPrice",
                            everyItem(notNullValue())))
                    .andExpect(jsonPath("$._embedded.products[*].imageUrl",
                            everyItem(notNullValue())))
                    .andExpect(jsonPath("$._embedded.products[*].unitsInStock",
                            everyItem(notNullValue())));
        }

        @Test
        @DisplayName("second page returns the correct remaining products")
        void secondPageReturnsRemainingProducts() throws Exception {
            mockMvc.perform(get("/api/products")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.products", hasSize(4)))
                    .andExpect(jsonPath("$.page.number").value(2));
        }
    }

    // ─── Single product — core field serialization ───────────────────────────

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetProductById {

        @Test
        @DisplayName("price, stock, active flag and SKU are serialized correctly")
        void coreFieldsSerializedCorrectly() throws Exception {
            mockMvc.perform(get("/api/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.sku").value("BOOK-TECH-1000"))
                    .andExpect(jsonPath("$.name").value("JavaScript - The Fun Parts"))
                    .andExpect(jsonPath("$.unitPrice").value(19.99))
                    .andExpect(jsonPath("$.unitsInStock").value(100))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("category is exposed as a HAL link so the client can fetch it independently")
        void categoryExposedAsHalLink() throws Exception {
            mockMvc.perform(get("/api/products/1"))
                    .andExpect(status().isOk())
                    // Spring Data REST externalises @ManyToOne relationships as _links entries
                    .andExpect(jsonPath("$._links.category.href").exists())
                    .andExpect(jsonPath("$._links.category.href",
                            containsString("/api/products/1/category")));
        }

        @Test
        @DisplayName("following the category link returns the correct category name and id")
        void categoryLinkResolvesToCorrectCategory() throws Exception {
            mockMvc.perform(get("/api/products/1/category"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.categoryName").value("Books"));
        }

        @Test
        @DisplayName("returns 404 for a non-existent id — boundary test")
        void returns404ForNonExistentProduct() throws Exception {
            mockMvc.perform(get("/api/products/9999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── Search: findByCategoryId ────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/products/search/findByCategoryId")
    class FindByCategoryId {

        @Test
        @DisplayName("returns 5 Books for category id=1 with pagination metadata")
        void returnsBooksByCategoryWithPagination() throws Exception {
            mockMvc.perform(get("/api/products/search/findByCategoryId")
                            .param("id", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.products", hasSize(5)))
                    .andExpect(jsonPath("$.page.totalElements").value(5))
                    .andExpect(jsonPath("$._embedded.products[?(@.sku == 'BOOK-TECH-1000')]").exists());
        }

        @Test
        @DisplayName("returns 5 Coffee Mugs for category id=2")
        void returnsMugsByCategory() throws Exception {
            mockMvc.perform(get("/api/products/search/findByCategoryId")
                            .param("id", "2")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.products", hasSize(5)));
        }

        @Test
        @DisplayName("returns empty products array for unknown category — boundary test")
        void returnsEmptyForUnknownCategory() throws Exception {
            mockMvc.perform(get("/api/products/search/findByCategoryId")
                            .param("id", "999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.products").isArray())
                    .andExpect(jsonPath("$._embedded.products", hasSize(0)));
        }
    }

    // ─── Search: findByNameContaining ────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/products/search/findByNameContaining")
    class FindByNameContaining {

        @Test
        @DisplayName("returns matching products for keyword 'Spring' including the Spring book")
        void returnsProductsMatchingSpring() throws Exception {
            mockMvc.perform(get("/api/products/search/findByNameContaining")
                            .param("name", "Spring")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.products",
                            hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath(
                            "$._embedded.products[?(@.sku == 'BOOK-TECH-1001')]").exists());
        }

        @Test
        @DisplayName("returns empty products array for non-matching keyword — boundary test")
        void returnsEmptyForNonMatchingKeyword() throws Exception {
            mockMvc.perform(get("/api/products/search/findByNameContaining")
                            .param("name", "ZZZNOMATCH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.products").isArray())
                    .andExpect(jsonPath("$._embedded.products", hasSize(0)));
        }
    }

    // ─── Write-method lockdown ───────────────────────────────────────────────

    @Nested
    @DisplayName("Write methods are disabled (CatalogDataRestConfig)")
    class WriteMethodsDisabled {

        @Test
        @DisplayName("POST /api/products returns 405 — catalog is read-only")
        void postToProductsReturns405() throws Exception {
            mockMvc.perform(post("/api/products")
                            .contentType("application/json")
                            .content("{\"sku\":\"TEST\",\"name\":\"Hacked\"}"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("DELETE /api/products/{id} returns 405 — catalog is read-only")
        void deleteProductReturns405() throws Exception {
            mockMvc.perform(delete("/api/products/1"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("PUT /api/product-category/{id} returns 405 — categories are read-only")
        void putCategoryReturns405() throws Exception {
            mockMvc.perform(put("/api/product-category/1")
                            .contentType("application/json")
                            .content("{\"categoryName\":\"Hacked\"}"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
