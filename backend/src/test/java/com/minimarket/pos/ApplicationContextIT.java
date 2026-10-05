package com.minimarket.pos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minimarket.pos.cash.application.CashSessionService;
import com.minimarket.pos.identity.application.UserAccountRepository;
import com.minimarket.pos.identity.domain.UserAccount;
import com.minimarket.pos.identity.domain.UserRole;
import com.minimarket.pos.identity.domain.UserStatus;
import com.minimarket.pos.inventory.application.InventoryService;
import com.minimarket.pos.inventory.domain.AdjustmentDirection;
import com.minimarket.pos.sales.application.CancelSaleCommand;
import com.minimarket.pos.sales.application.SaleCancellationService;
import com.minimarket.pos.sales.domain.PaymentMethod;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Testcontainers
@SpringBootTest(
        properties = {
            "pos.identity.bootstrap.username=admin",
            "pos.identity.bootstrap.password=Temporal-admin-2026",
            "pos.identity.bootstrap.display-name=Administración"
        })
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
class ApplicationContextIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18.4-alpine")
                    .withDatabaseName("pos_test")
                    .withUsername("pos_test")
                    .withPassword("pos_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserAccountRepository repository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CashSessionService cashSessionService;

    @Autowired
    private SaleCancellationService saleCancellationService;

    @Test
    @Order(1)
    void startsAgainstCleanPostgresAppliesMigrationsAndBootstrapsAdministrator() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_metadata WHERE metadata_key = ?",
                Integer.class,
                "schema-purpose");

        assertThat(count).isEqualTo(1);
        UserAccount administrator = repository.findByUsername("admin").orElseThrow();
        assertThat(administrator.role()).isEqualTo(UserRole.ADMIN);
        assertThat(administrator.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(administrator.passwordChangeRequired()).isTrue();
        assertThat(administrator.passwordHash()).startsWith("$argon2id$");
        assertThat(administrator.passwordHash()).doesNotContain("Temporal-admin-2026");
    }

    @Test
    @Order(2)
    void protectsRequestsWithCsrfAndGenericAuthenticationErrors() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"incorrecta"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .with(request -> remoteAddress(request, "10.0.0.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-failed"))
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña inválidos."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .with(request -> remoteAddress(request, "10.0.0.11"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"desconocido","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña inválidos."));
    }

    @Test
    @Order(3)
    void forcesTemporaryPasswordChangeAndRevokesTheOldSession() throws Exception {
        Cookie temporarySession = login("admin", "Temporal-admin-2026", "10.0.0.20")
                .getResponse()
                .getCookie("POS_SESSION");
        assertThat(temporarySession).isNotNull();

        mockMvc.perform(get("/api/v1/auth/session").cookie(temporarySession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.passwordChangeRequired").value(true));

        mockMvc.perform(get("/api/v1/admin/not-implemented").cookie(temporarySession))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/password")
                        .cookie(temporarySession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Temporal-admin-2026","newPassword":"Definitiva-admin-2026"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("POS_SESSION", 0));

        mockMvc.perform(get("/api/v1/auth/session").cookie(temporarySession))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .with(request -> remoteAddress(request, "10.0.0.21"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Temporal-admin-2026"}
                                """))
                .andExpect(status().isUnauthorized());

        Cookie finalSession = login("admin", "Definitiva-admin-2026", "10.0.0.22")
                .getResponse()
                .getCookie("POS_SESSION");
        assertThat(finalSession).isNotNull();
        mockMvc.perform(get("/api/v1/admin/not-implemented").cookie(finalSession))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    void enforcesSellerRoleAndLogoutRevokesItsSession() throws Exception {
        createUser("vendedor", "Vendedor-seguro-2026", UserRole.SELLER, UserStatus.ACTIVE, false);
        Cookie session = login("vendedor", "Vendedor-seguro-2026", "10.0.0.30")
                .getResponse()
                .getCookie("POS_SESSION");

        mockMvc.perform(get("/api/v1/admin/not-implemented").cookie(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/logout").cookie(session).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("POS_SESSION", 0));
        mockMvc.perform(get("/api/v1/auth/session").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void rejectsInactiveUsersAndLimitsRepeatedAuthenticationFailures() throws Exception {
        createUser("inactivo", "Inactivo-seguro-2026", UserRole.SELLER, UserStatus.INACTIVE, false);
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .with(request -> remoteAddress(request, "10.0.0.40"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"inactivo","password":"Inactivo-seguro-2026"}
                                """))
                .andExpect(status().isUnauthorized());

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .with(request -> remoteAddress(request, "10.0.0.41"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"objetivo","password":"incorrecta"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .with(request -> remoteAddress(request, "10.0.0.41"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"objetivo","password":"incorrecta"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("authentication-rate-limited"));

        Integer rateLimited = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM authentication_audit_event WHERE event_type = 'LOGIN_RATE_LIMITED'",
                Integer.class);
        assertThat(rateLimited).isEqualTo(1);
        assertThatThrownBy(() -> repository.updatePassword("ausente", "hash", false))
                .isInstanceOf(DataAccessException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @Order(6)
    void administersCategoriesWithoutGrantingAccessToSellers() throws Exception {
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.50")
                .getResponse()
                .getCookie("POS_SESSION");
        Cookie seller = login("vendedor", "Vendedor-seguro-2026", "10.0.0.51")
                .getResponse()
                .getCookie("POS_SESSION");

        mockMvc.perform(post("/api/v1/admin/catalog/categories")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Sin permiso"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/catalog/categories")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   Bebidas frías   "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bebidas frías"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        UUID categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_category WHERE name = ?", UUID.class, "Bebidas frías");
        assertThat(categoryId).isNotNull();

        mockMvc.perform(put("/api/v1/admin/catalog/categories/{id}", categoryId)
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bebidas"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bebidas"));

        mockMvc.perform(patch("/api/v1/admin/catalog/categories/{id}/status", categoryId)
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/admin/catalog/categories").cookie(administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/admin/catalog/categories")
                        .param("includeInactive", "true")
                        .cookie(administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(categoryId.toString()))
                .andExpect(jsonPath("$[0].status").value("INACTIVE"));

        mockMvc.perform(post("/api/v1/admin/catalog/categories")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        mockMvc.perform(put("/api/v1/admin/catalog/categories/{id}", UUID.randomUUID())
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ausente"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("La categoría solicitada no existe."));
    }

    @Test
    @Order(7)
    void administersAndSearchesProductsWithZeroBalanceAndExactTextualCodes() throws Exception {
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.60")
                .getResponse()
                .getCookie("POS_SESSION");
        Cookie seller = login("vendedor", "Vendedor-seguro-2026", "10.0.0.61")
                .getResponse()
                .getCookie("POS_SESSION");

        mockMvc.perform(post("/api/v1/admin/catalog/categories")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Abarrotes"}
                                """))
                .andExpect(status().isCreated());
        UUID categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_category WHERE name = ?", UUID.class, "Abarrotes");

        String productBody = """
                {
                  "code":"  00123  ",
                  "name":"  Café   Molido  ",
                  "categoryId":"%s",
                  "purchasePrice":1200,
                  "salePrice":1590.50,
                  "minimumStock":3
                }
                """.formatted(categoryId);

        mockMvc.perform(post("/api/v1/admin/catalog/products")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/catalog/products")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("00123"))
                .andExpect(jsonPath("$.name").value("Café Molido"))
                .andExpect(jsonPath("$.purchasePrice").value(1200.00))
                .andExpect(jsonPath("$.salePrice").value(1590.50))
                .andExpect(jsonPath("$.currentStock").value(0))
                .andExpect(jsonPath("$.available").value(false));

        UUID productId = jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_product WHERE code = ?", UUID.class, "00123");
        Integer initialBalance = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory_balance WHERE product_id = ?",
                Integer.class,
                productId);
        assertThat(initialBalance).isZero();

        mockMvc.perform(post("/api/v1/admin/catalog/products")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un producto con ese código."));

        String productWithoutCode = """
                {
                  "code":"  ",
                  "name":"Azúcar",
                  "categoryId":"%s",
                  "purchasePrice":800,
                  "salePrice":990,
                  "minimumStock":2
                }
                """.formatted(categoryId);
        mockMvc.perform(post("/api/v1/admin/catalog/products")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productWithoutCode))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.currentStock").value(0));

        jdbcTemplate.update(
                "UPDATE inventory_balance SET quantity = 5 WHERE product_id = ?", productId);
        String editedBody = """
                {
                  "code":"00123",
                  "name":"Café Premium",
                  "categoryId":"%s",
                  "purchasePrice":1300,
                  "salePrice":1690,
                  "minimumStock":4
                }
                """.formatted(categoryId);
        mockMvc.perform(put("/api/v1/admin/catalog/products/{id}", productId)
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editedBody.replace("\"minimumStock\":4", "\"minimumStock\":4,\"currentStock\":99")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("malformed-body"));

        mockMvc.perform(put("/api/v1/admin/catalog/products/{id}", productId)
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editedBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Café Premium"))
                .andExpect(jsonPath("$.currentStock").value(5))
                .andExpect(jsonPath("$.available").value(true));

        mockMvc.perform(get("/api/v1/catalog/products/by-code")
                        .param("code", " 00123 ")
                        .cookie(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00123"))
                .andExpect(jsonPath("$.salePrice").value(1690.00))
                .andExpect(jsonPath("$.currentStock").value(5))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.purchasePrice").doesNotExist());

        mockMvc.perform(get("/api/v1/catalog/products/search")
                        .param("name", "CAFE pre")
                        .cookie(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(productId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        Long beforeUnknown = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM catalog_product", Long.class);
        mockMvc.perform(get("/api/v1/catalog/products/by-code")
                        .param("code", "desconocido")
                        .cookie(seller))
                .andExpect(status().isNotFound());
        Long afterUnknown = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM catalog_product", Long.class);
        assertThat(afterUnknown).isEqualTo(beforeUnknown);

        mockMvc.perform(patch("/api/v1/admin/catalog/products/{id}/status", productId)
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/catalog/products/by-code")
                        .param("code", "00123")
                        .cookie(seller))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/catalog/products/search")
                        .param("name", "cafe")
                        .cookie(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        mockMvc.perform(get("/api/v1/admin/catalog/products")
                        .param("includeInactive", "true")
                        .param("name", "café")
                        .cookie(administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/admin/catalog/products")
                        .param("size", "101")
                        .cookie(administrator))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation"));
    }

    @Test
    @Order(8)
    void recordsAuditableInventoryEntriesAndAdjustmentsWithoutAllowingHistoryMutation()
            throws Exception {
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.70")
                .getResponse()
                .getCookie("POS_SESSION");
        Cookie seller = login("vendedor", "Vendedor-seguro-2026", "10.0.0.71")
                .getResponse()
                .getCookie("POS_SESSION");
        UUID productId = createInventoryProduct(administrator, "INV-001", "Arroz inventario");

        String entry = """
                {"productId":"%s","quantity":8,"reason":"Compra inicial","reference":"FAC-100"}
                """.formatted(productId);
        mockMvc.perform(post("/api/v1/admin/inventory/entries")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entry))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/inventory/entries")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entry))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ENTRY"))
                .andExpect(jsonPath("$.previousBalance").value(0))
                .andExpect(jsonPath("$.resultingBalance").value(8))
                .andExpect(jsonPath("$.actorDisplayName").value("Administración"));

        adjust(administrator, productId, "POSITIVE", 1, "Conteo encontró una unidad", 9);
        adjust(administrator, productId, "NEGATIVE", 2, "Merma detectada", 7);

        mockMvc.perform(post("/api/v1/admin/inventory/adjustments")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","direction":"NEGATIVE","quantity":1,"reason":" "}
                                """.formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation"));
        mockMvc.perform(post("/api/v1/admin/inventory/adjustments")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","direction":"NEGATIVE","quantity":8,"reason":"Error de conteo"}
                                """.formatted(productId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("conflict"));

        mockMvc.perform(get("/api/v1/admin/inventory/products/{id}", productId)
                        .cookie(seller))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/inventory/products/{id}", productId)
                        .param("size", "101")
                        .cookie(administrator))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation"));
        mockMvc.perform(get("/api/v1/admin/inventory/products/{id}", productId)
                        .cookie(administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(7))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.movements.totalElements").value(3))
                .andExpect(jsonPath("$.movements.items[0].type").value("ADJUSTMENT_OUT"))
                .andExpect(jsonPath("$.movements.items[2].type").value("ENTRY"));

        Integer movementDelta = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(delta), 0) FROM inventory_movement WHERE product_id = ?",
                Integer.class,
                productId);
        Integer balance = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory_balance WHERE product_id = ?", Integer.class, productId);
        assertThat(movementDelta).isEqualTo(balance).isEqualTo(7);

        UUID movementId = jdbcTemplate.queryForObject(
                "SELECT id FROM inventory_movement WHERE product_id = ? ORDER BY occurred_at LIMIT 1",
                UUID.class,
                productId);
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE inventory_movement SET reason = ? WHERE id = ?", "Alterado", movementId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM inventory_movement WHERE id = ?", movementId))
                .isInstanceOf(DataAccessException.class);
        Long movementCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movement WHERE product_id = ?", Long.class, productId);
        assertThat(movementCount).isEqualTo(3);
    }

    @Test
    @Order(9)
    void serializesConcurrentAdjustmentsSoTheLastUnitCannotBeConsumedTwice() throws Exception {
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.80")
                .getResponse()
                .getCookie("POS_SESSION");
        UUID productId = createInventoryProduct(administrator, "INV-CONC", "Producto concurrente");
        mockMvc.perform(post("/api/v1/admin/inventory/entries")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":1,"reference":"INICIAL-CONC"}
                                """.formatted(productId)))
                .andExpect(status().isCreated());

        UUID actorId = repository.findByUsername("admin").orElseThrow().id();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> consumeLastUnit = () -> attemptConcurrentAdjustment(productId, actorId, ready, start);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(consumeLastUnit);
            var second = executor.submit(consumeLastUnit);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }

        Integer balance = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory_balance WHERE product_id = ?", Integer.class, productId);
        Long movements = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movement WHERE product_id = ?", Long.class, productId);
        assertThat(balance).isZero();
        assertThat(movements).isEqualTo(2);
    }

    @Test
    @Order(10)
    void opensOneCashSessionForEitherRoleAndRejectsConcurrentOpenings() throws Exception {
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.90")
                .getResponse()
                .getCookie("POS_SESSION");
        Cookie seller = login("vendedor", "Vendedor-seguro-2026", "10.0.0.91")
                .getResponse()
                .getCookie("POS_SESSION");

        mockMvc.perform(get("/api/v1/cash/sessions/current"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/cash/sessions/current").cookie(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session").doesNotExist());
        mockMvc.perform(post("/api/v1/cash/sessions")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"openingAmount":25000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.openingAmount").value(25000))
                .andExpect(jsonPath("$.openedByDisplayName").value("vendedor"))
                .andExpect(jsonPath("$.status").value("OPEN"));
        mockMvc.perform(get("/api/v1/cash/sessions/current").cookie(administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.openingAmount").value(25000))
                .andExpect(jsonPath("$.session.status").value("OPEN"));
        mockMvc.perform(post("/api/v1/cash/sessions")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"openingAmount":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe una sesión de caja abierta."));
        mockMvc.perform(post("/api/v1/cash/sessions")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"openingAmount":0.5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation"));

        mockMvc.perform(post("/api/v1/cash/sessions/current/close")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"countedCash":25000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedCash").value(25000))
                .andExpect(jsonPath("$.difference").value(0));
        UUID administratorId = repository.findByUsername("admin").orElseThrow().id();
        UUID sellerId = repository.findByUsername("vendedor").orElseThrow().id();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> administratorOpening = () -> attemptConcurrentCashOpening(
                administratorId, "Administración", "ROLE_ADMIN", ready, start);
        Callable<Boolean> sellerOpening = () ->
                attemptConcurrentCashOpening(sellerId, "Vendedor", "ROLE_SELLER", ready, start);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(administratorOpening);
            var second = executor.submit(sellerOpening);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }

        Long openSessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cash_session WHERE status = 'OPEN'", Long.class);
        BigDecimal storedAmount = jdbcTemplate.queryForObject(
                "SELECT opening_amount FROM cash_session WHERE status = 'OPEN'", BigDecimal.class);
        assertThat(openSessions).isEqualTo(1);
        assertThat(storedAmount).isEqualByComparingTo("10000");
    }

    @Test
    @Order(11)
    void recordsAndReconcilesManualCashMovementsThenClosesAnImmutableSession()
            throws Exception {
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.100")
                .getResponse()
                .getCookie("POS_SESSION");
        Cookie seller = login("vendedor", "Vendedor-seguro-2026", "10.0.0.101")
                .getResponse()
                .getCookie("POS_SESSION");

        String income = """
                {
                  "direction":"INCOME",
                  "category":"CASH_REPLENISHMENT",
                  "amount":5000,
                  "reason":"Refuerzo para cambio"
                }
                """;
        mockMvc.perform(post("/api/v1/admin/cash/movements")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(income))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/cash/movements")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(income))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("MANUAL_INCOME"))
                .andExpect(jsonPath("$.category").value("CASH_REPLENISHMENT"))
                .andExpect(jsonPath("$.actorDisplayName").value("Administración"));
        mockMvc.perform(post("/api/v1/admin/cash/movements")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "direction":"WITHDRAWAL",
                                  "category":"OPERATING_EXPENSE",
                                  "amount":3000,
                                  "reason":"Compra de bolsas"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("MANUAL_WITHDRAWAL"));
        mockMvc.perform(post("/api/v1/admin/cash/movements")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "direction":"INCOME",
                                  "category":"SUPPLIER_PAYMENT",
                                  "amount":1,
                                  "reason":"Categoría incorrecta"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation"));
        mockMvc.perform(post("/api/v1/admin/cash/movements")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "direction":"WITHDRAWAL",
                                  "category":"OTHER_WITHDRAWAL",
                                  "amount":12001,
                                  "reason":"Monto excesivo"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/cash/sessions/current/summary")
                        .cookie(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.openingAmount").value(10000))
                .andExpect(jsonPath("$.totals.cashSales").value(0))
                .andExpect(jsonPath("$.totals.manualIncome").value(5000))
                .andExpect(jsonPath("$.totals.manualWithdrawals").value(3000))
                .andExpect(jsonPath("$.totals.cardSales").value(0))
                .andExpect(jsonPath("$.totals.transferSales").value(0))
                .andExpect(jsonPath("$.expectedCash").value(12000))
                .andExpect(jsonPath("$.movements.totalElements").value(2));
        mockMvc.perform(get("/api/v1/cash/sessions/current/summary")
                        .param("size", "101")
                        .cookie(administrator))
                .andExpect(status().isBadRequest());

        UUID movementId = jdbcTemplate.queryForObject(
                "SELECT id FROM cash_movement ORDER BY occurred_at LIMIT 1", UUID.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE cash_movement SET reason = ? WHERE id = ?", "Alterado", movementId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM cash_movement WHERE id = ?", movementId))
                .isInstanceOf(DataAccessException.class);

        mockMvc.perform(post("/api/v1/cash/sessions/current/close")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"countedCash":11500}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedCash").value(12000))
                .andExpect(jsonPath("$.countedCash").value(11500))
                .andExpect(jsonPath("$.difference").value(-500));

        mockMvc.perform(get("/api/v1/cash/sessions/current").cookie(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session").doesNotExist());
        mockMvc.perform(post("/api/v1/admin/cash/movements")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(income))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/cash/sessions/current/close")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"countedCash":0}
                                """))
                .andExpect(status().isConflict());

        UUID closedId = jdbcTemplate.queryForObject(
                "SELECT id FROM cash_session WHERE status = 'CLOSED' ORDER BY closed_at DESC LIMIT 1",
                UUID.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE cash_session SET status = 'OPEN' WHERE id = ?", closedId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM cash_session WHERE id = ?", closedId))
                .isInstanceOf(DataAccessException.class);
        BigDecimal storedDifference = jdbcTemplate.queryForObject(
                "SELECT cash_difference FROM cash_session WHERE id = ?", BigDecimal.class, closedId);
        assertThat(storedDifference).isEqualByComparingTo("-500");
    }

    @Test
    @Order(12)
    void confirmsIdempotentSalesWithReceiptHistoryInventoryAndCashEffects() throws Exception {
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.110")
                .getResponse()
                .getCookie("POS_SESSION");
        Cookie seller = login("vendedor", "Vendedor-seguro-2026", "10.0.0.111")
                .getResponse()
                .getCookie("POS_SESSION");

        mockMvc.perform(post("/api/v1/cash/sessions")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"openingAmount":10000}
                                """))
                .andExpect(status().isCreated());
        UUID productId = createInventoryProduct(
                administrator, "SALE-001", "Producto para venta");
        jdbcTemplate.update(
                "UPDATE catalog_product SET sale_price = 203 WHERE id = ?", productId);
        mockMvc.perform(post("/api/v1/admin/inventory/entries")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":5,"reference":"INICIAL-VENTA"}
                                """.formatted(productId)))
                .andExpect(status().isCreated());

        UUID cashKey = UUID.fromString("10000000-0000-0000-0000-000000000001");
        String cashSale = saleBody(cashKey, productId, 2, "CASH", "1000");
        mockMvc.perform(post("/api/v1/sales")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cashSale))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio").value("V-000001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.total").value(406))
                .andExpect(jsonPath("$.lines[0].productName").value("Producto para venta"))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.payment.method").value("CASH"))
                .andExpect(jsonPath("$.payment.amount").value(406))
                .andExpect(jsonPath("$.payment.cashPayable").value(410))
                .andExpect(jsonPath("$.payment.roundingAdjustment").value(4))
                .andExpect(jsonPath("$.payment.cashReceived").value(1000))
                .andExpect(jsonPath("$.payment.change").value(590))
                .andExpect(jsonPath("$.notice").value("Comprobante interno no tributario."));

        UUID saleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sale WHERE idempotency_key = ?", UUID.class, cashKey);
        mockMvc.perform(post("/api/v1/sales")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cashSale))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saleId.toString()))
                .andExpect(jsonPath("$.folio").value("V-000001"));
        mockMvc.perform(post("/api/v1/sales")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(cashKey, productId, 1, "CASH", "1000")))
                .andExpect(status().isConflict());

        UUID cardKey = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID transferKey = UUID.fromString("10000000-0000-0000-0000-000000000003");
        mockMvc.perform(post("/api/v1/sales")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(cardKey, productId, 1, "CARD", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.amount").value(203))
                .andExpect(jsonPath("$.payment.cashPayable").doesNotExist())
                .andExpect(jsonPath("$.payment.roundingAdjustment").doesNotExist())
                .andExpect(jsonPath("$.payment.cashReceived").doesNotExist());
        mockMvc.perform(post("/api/v1/sales")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(transferKey, productId, 1, "TRANSFER", null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sales")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(UUID.randomUUID(), productId, 1, "CASH", "100")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/sales")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(UUID.randomUUID(), productId, 2, "CARD", null)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/sales").cookie(seller))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/sales").cookie(administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items[0].folio").value("V-000003"));
        mockMvc.perform(get("/api/v1/sales/{id}", saleId).cookie(administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].productCode").value("SALE-001"));

        mockMvc.perform(get("/api/v1/cash/sessions/current/summary")
                        .cookie(administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.cashSales").value(410))
                .andExpect(jsonPath("$.totals.cardSales").value(203))
                .andExpect(jsonPath("$.totals.transferSales").value(203))
                .andExpect(jsonPath("$.expectedCash").value(10410));

        Integer balance = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory_balance WHERE product_id = ?",
                Integer.class,
                productId);
        Long saleOuts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movement WHERE product_id = ? AND movement_type = 'SALE_OUT'",
                Long.class,
                productId);
        Long sales = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sale", Long.class);
        assertThat(balance).isEqualTo(1);
        assertThat(saleOuts).isEqualTo(3);
        assertThat(sales).isEqualTo(3);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE sale SET total = total + 1 WHERE id = ?", saleId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "DELETE FROM payment WHERE sale_id = ?", saleId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @Order(13)
    void cancelsSalesOnceWithAdministrativeInventoryAndRefundEffects() throws Exception {
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.120")
                .getResponse()
                .getCookie("POS_SESSION");
        Cookie seller = login("vendedor", "Vendedor-seguro-2026", "10.0.0.121")
                .getResponse()
                .getCookie("POS_SESSION");
        UUID cashSaleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sale WHERE idempotency_key = ?",
                UUID.class,
                UUID.fromString("10000000-0000-0000-0000-000000000001"));
        UUID cardSaleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sale WHERE idempotency_key = ?",
                UUID.class,
                UUID.fromString("10000000-0000-0000-0000-000000000002"));

        String cashCancellation = """
                {"refundMethod":"CASH","reason":"Cobro duplicado"}
                """;
        mockMvc.perform(post("/api/v1/admin/sales/{saleId}/cancellations", cashSaleId)
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cashCancellation))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/sales/{saleId}/cancellations", cashSaleId)
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refundMethod\":\"CASH\",\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/admin/sales/{saleId}/cancellations", cashSaleId)
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cashCancellation))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VOIDED"))
                .andExpect(jsonPath("$.cancellation.refundMethod").value("CASH"))
                .andExpect(jsonPath("$.cancellation.amount").value(406))
                .andExpect(jsonPath("$.cancellation.cashPayable").value(410))
                .andExpect(jsonPath("$.cancellation.roundingAdjustment").value(4))
                .andExpect(jsonPath("$.cancellation.reason").value("Cobro duplicado"));
        mockMvc.perform(post("/api/v1/admin/sales/{saleId}/cancellations", cashSaleId)
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cashCancellation))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/admin/sales/{saleId}/cancellations", cardSaleId)
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refundMethod":"CARD","reason":"Pago revertido"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VOIDED"))
                .andExpect(jsonPath("$.cancellation.amount").value(203))
                .andExpect(jsonPath("$.cancellation.cashPayable").doesNotExist())
                .andExpect(jsonPath("$.cancellation.roundingAdjustment").doesNotExist());

        mockMvc.perform(get("/api/v1/cash/sessions/current/summary")
                        .cookie(administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.cashSales").value(410))
                .andExpect(jsonPath("$.totals.cashRefunds").value(410))
                .andExpect(jsonPath("$.expectedCash").value(10000));

        UUID productId = jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_product WHERE code = 'SALE-001'", UUID.class);
        Integer balance = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory_balance WHERE product_id = ?",
                Integer.class,
                productId);
        Long reversals = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movement WHERE product_id = ? AND movement_type = 'SALE_REVERSAL'",
                Long.class,
                productId);
        Long cancellations =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sale_cancellation", Long.class);
        assertThat(balance).isEqualTo(4);
        assertThat(reversals).isEqualTo(2);
        assertThat(cancellations).isEqualTo(2);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE sale_cancellation SET reason = 'Alterado' WHERE sale_id = ?",
                        cashSaleId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE sale SET status = 'CONFIRMED' WHERE id = ?", cashSaleId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @Order(14)
    void serializesConcurrentSaleCancellationsSoEffectsAreAppliedOnce() throws Exception {
        UUID transferSaleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sale WHERE idempotency_key = ?",
                UUID.class,
                UUID.fromString("10000000-0000-0000-0000-000000000003"));
        UUID actorId = repository.findByUsername("admin").orElseThrow().id();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> cancelTransfer =
                () -> attemptConcurrentCancellation(transferSaleId, actorId, ready, start);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(cancelTransfer);
            var second = executor.submit(cancelTransfer);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }

        UUID productId = jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_product WHERE code = 'SALE-001'", UUID.class);
        Integer balance = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory_balance WHERE product_id = ?",
                Integer.class,
                productId);
        Long cancellations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sale_cancellation WHERE sale_id = ?",
                Long.class,
                transferSaleId);
        Long reversals = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movement WHERE product_id = ? AND movement_type = 'SALE_REVERSAL'",
                Long.class,
                productId);
        assertThat(balance).isEqualTo(5);
        assertThat(cancellations).isEqualTo(1);
        assertThat(reversals).isEqualTo(3);
    }

    @Test
    @Order(15)
    void rollsBackEveryCancellationEffectWhenTheFinalTransitionFails() throws Exception {
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.130")
                .getResponse()
                .getCookie("POS_SESSION");
        UUID productId = jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_product WHERE code = 'SALE-001'", UUID.class);
        UUID idempotencyKey = UUID.fromString("10000000-0000-0000-0000-000000000004");
        mockMvc.perform(post("/api/v1/sales")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(idempotencyKey, productId, 1, "CASH", "1000")))
                .andExpect(status().isCreated());
        UUID saleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sale WHERE idempotency_key = ?", UUID.class, idempotencyKey);

        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION fail_test_sale_void_transition()
                RETURNS trigger AS $$
                BEGIN
                    IF NEW.id = '%s'::uuid THEN
                        RAISE EXCEPTION 'injected cancellation failure';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """.formatted(saleId));
        jdbcTemplate.execute("""
                CREATE TRIGGER a_fail_test_sale_void_transition
                BEFORE UPDATE ON sale
                FOR EACH ROW EXECUTE FUNCTION fail_test_sale_void_transition()
                """);
        try {
            mockMvc.perform(post("/api/v1/admin/sales/{saleId}/cancellations", saleId)
                            .cookie(administrator)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refundMethod":"CASH","reason":"Falla controlada"}
                                    """))
                    .andExpect(status().isInternalServerError());
        } finally {
            jdbcTemplate.execute("DROP TRIGGER a_fail_test_sale_void_transition ON sale");
            jdbcTemplate.execute("DROP FUNCTION fail_test_sale_void_transition()");
        }

        String saleStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM sale WHERE id = ?", String.class, saleId);
        Integer balance = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory_balance WHERE product_id = ?",
                Integer.class,
                productId);
        Long cancellationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sale_cancellation WHERE sale_id = ?", Long.class, saleId);
        Long reversalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movement WHERE product_id = ? AND movement_type = 'SALE_REVERSAL'",
                Long.class,
                productId);
        BigDecimal cashRefunds = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM cash_movement WHERE movement_type = 'CASH_REFUND'",
                BigDecimal.class);
        assertThat(saleStatus).isEqualTo("CONFIRMED");
        assertThat(balance).isEqualTo(4);
        assertThat(cancellationCount).isZero();
        assertThat(reversalCount).isEqualTo(3);
        assertThat(cashRefunds).isEqualByComparingTo("410");
    }

    @Test
    @Order(16)
    void exposesAReadOnlyFilteredAuditProjectionOnlyToAdministrators() throws Exception {
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.140")
                .getResponse()
                .getCookie("POS_SESSION");
        Cookie seller = login("vendedor", "Vendedor-seguro-2026", "10.0.0.141")
                .getResponse()
                .getCookie("POS_SESSION");

        mockMvc.perform(get("/api/v1/admin/audit-events"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/audit-events").cookie(seller))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .cookie(administrator)
                        .param("type", "SALE_VOIDED")
                        .param("reference", "V-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].type").value("SALE_VOIDED"))
                .andExpect(jsonPath("$.items[0].reference").value("V-000001"))
                .andExpect(jsonPath("$.items[0].summary").value("Venta anulada por $406. Motivo: Cobro duplicado"))
                .andExpect(jsonPath("$.items[0].actorDisplayName").isNotEmpty())
                .andExpect(jsonPath("$.items[0].sourceAddress").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .cookie(administrator)
                        .param("type", "LOGIN_SUCCEEDED")
                        .param("reference", "admin")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.items[0].summary").value("Inicio de sesi\u00f3n exitoso"));
        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .cookie(administrator)
                        .param("reference", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .cookie(administrator)
                        .param("from", "2026-08-06")
                        .param("to", "2026-08-05"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation"));
        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .cookie(administrator)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation"));
    }

    @Test
    @Order(17)
    void reconcilesOperationalReportsAndRestrictsThemToAdministrators() throws Exception {
        String businessDate = LocalDate.now(ZoneId.of("America/Santiago")).toString();
        Cookie administrator = login("admin", "Definitiva-admin-2026", "10.0.0.150")
                .getResponse()
                .getCookie("POS_SESSION");
        Cookie seller = login("vendedor", "Vendedor-seguro-2026", "10.0.0.151")
                .getResponse()
                .getCookie("POS_SESSION");
        UUID productId = jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_product WHERE code = 'SALE-001'", UUID.class);

        mockMvc.perform(get("/api/v1/admin/reports/sales"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/reports/sales").cookie(seller))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/reports/sales")
                        .cookie(administrator)
                        .param("from", businessDate)
                        .param("to", businessDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordedSales").value(4))
                .andExpect(jsonPath("$.recordedAmount").value(1015))
                .andExpect(jsonPath("$.voidedSales").value(3))
                .andExpect(jsonPath("$.voidedAmount").value(812))
                .andExpect(jsonPath("$.netSales").value(1))
                .andExpect(jsonPath("$.netAmount").value(203))
                .andExpect(jsonPath("$.estimatedGrossProfit").value(103.0))
                .andExpect(jsonPath("$.topProducts[0].code").value("SALE-001"))
                .andExpect(jsonPath("$.topProducts[0].quantity").value(1))
                .andExpect(jsonPath("$.refunds.length()").value(3));

        mockMvc.perform(get("/api/v1/admin/reports/inventory")
                        .cookie(administrator)
                        .param("from", businessDate)
                        .param("to", businessDate)
                        .param("type", "SALE_OUT")
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals[0].type").value("SALE_OUT"))
                .andExpect(jsonPath("$.totals[0].movements").value(4))
                .andExpect(jsonPath("$.totals[0].quantity").value(5))
                .andExpect(jsonPath("$.totals[0].netDelta").value(-5))
                .andExpect(jsonPath("$.movements.totalElements").value(4))
                .andExpect(jsonPath("$.movements.items[0].productCode").value("SALE-001"));

        mockMvc.perform(get("/api/v1/admin/reports/cash")
                        .cookie(administrator)
                        .param("from", businessDate)
                        .param("to", businessDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openedSessions").isNumber())
                .andExpect(jsonPath("$.sessions.items").isArray());

        mockMvc.perform(get("/api/v1/admin/reports/cash")
                        .cookie(administrator)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation"));
    }

    @Test
    @Order(18)
    void restoresACompleteBackupIntoAnIsolatedPostgresAndReconcilesCriticalData()
            throws Exception {
        Path backup = Files.createTempFile("pos-backup-restore-", ".dump");
        String sourceDump = "/tmp/pos-backup-restore.dump";

        try (PostgreSQLContainer<?> restored =
                new PostgreSQLContainer<>("postgres:18.4-alpine")
                        .withDatabaseName("pos_restore")
                        .withUsername("pos_restore")
                        .withPassword("pos_restore")) {
            assertContainerCommandSucceeded(POSTGRES.execInContainer(
                    "pg_dump",
                    "--username=" + POSTGRES.getUsername(),
                    "--dbname=" + POSTGRES.getDatabaseName(),
                    "--format=custom",
                    "--no-owner",
                    "--no-acl",
                    "--file=" + sourceDump));
            POSTGRES.copyFileFromContainer(sourceDump, backup.toString());

            restored.start();
            restored.copyFileToContainer(
                    MountableFile.forHostPath(backup), "/tmp/pos-backup-restore.dump");
            assertContainerCommandSucceeded(restored.execInContainer(
                    "pg_restore",
                    "--username=" + restored.getUsername(),
                    "--dbname=" + restored.getDatabaseName(),
                    "--no-owner",
                    "--no-acl",
                    "--exit-on-error",
                    "/tmp/pos-backup-restore.dump"));

            JdbcTemplate restoredJdbc = new JdbcTemplate(new DriverManagerDataSource(
                    restored.getJdbcUrl(), restored.getUsername(), restored.getPassword()));
            assertThat(operationalDataSnapshot(restoredJdbc))
                    .isEqualTo(operationalDataSnapshot(jdbcTemplate));
        } finally {
            POSTGRES.execInContainer("rm", "-f", sourceDump);
            Files.deleteIfExists(backup);
        }
    }

    private OperationalDataSnapshot operationalDataSnapshot(JdbcOperations jdbc) {
        return new OperationalDataSnapshot(
                requiredLong(jdbc, "SELECT COUNT(*) FROM flyway_schema_history WHERE success"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM app_metadata"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM app_user"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM authentication_audit_event"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM catalog_product"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM inventory_movement"),
                requiredLong(jdbc, "SELECT COALESCE(SUM(quantity), 0) FROM inventory_balance"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM cash_session"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM cash_movement"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM sale"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM sale_line"),
                requiredLong(jdbc, "SELECT COALESCE(SUM(total), 0) FROM sale"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM payment"),
                requiredLong(jdbc, "SELECT COUNT(*) FROM sale_cancellation"));
    }

    private long requiredLong(JdbcOperations jdbc, String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        if (value == null) {
            throw new IllegalStateException("La consulta de reconciliación no entregó un valor.");
        }
        return value;
    }

    private void assertContainerCommandSucceeded(
            org.testcontainers.containers.Container.ExecResult result) {
        assertThat(result.getExitCode())
                .withFailMessage("Falló comando PostgreSQL: %s", result.getStderr())
                .isZero();
    }

    private record OperationalDataSnapshot(
            long migrations,
            long metadata,
            long users,
            long authenticationEvents,
            long products,
            long inventoryMovements,
            long inventoryQuantity,
            long cashSessions,
            long cashMovements,
            long sales,
            long saleLines,
            long salesTotal,
            long payments,
            long cancellations) {}

    private String saleBody(
            UUID idempotencyKey,
            UUID productId,
            int quantity,
            String method,
            String cashReceived) {
        String received = cashReceived == null ? "" : ",\"cashReceived\":" + cashReceived;
        return """
                {
                  "idempotencyKey":"%s",
                  "lines":[{"productId":"%s","quantity":%d}],
                  "payment":{"method":"%s"%s}
                }
                """.formatted(idempotencyKey, productId, quantity, method, received);
    }

    private MvcResult login(String username, String password, String sourceAddress) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .with(request -> remoteAddress(request, sourceAddress))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private UUID createInventoryProduct(Cookie administrator, String code, String name) throws Exception {
        UUID categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_category WHERE name = ?", UUID.class, "Abarrotes");
        mockMvc.perform(post("/api/v1/admin/catalog/products")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"%s",
                                  "name":"%s",
                                  "categoryId":"%s",
                                  "purchasePrice":100,
                                  "salePrice":200,
                                  "minimumStock":1
                                }
                                """.formatted(code, name, categoryId)))
                .andExpect(status().isCreated());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_product WHERE code = ?", UUID.class, code);
    }

    private void adjust(
            Cookie administrator,
            UUID productId,
            String direction,
            int quantity,
            String reason,
            int expectedBalance)
            throws Exception {
        mockMvc.perform(post("/api/v1/admin/inventory/adjustments")
                        .cookie(administrator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","direction":"%s","quantity":%d,"reason":"%s"}
                                """.formatted(productId, direction, quantity, reason)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultingBalance").value(expectedBalance));
    }

    private boolean attemptConcurrentAdjustment(
            UUID productId, UUID actorId, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "concurrency-test",
                "not-used",
                AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent inventory test did not start in time");
            }
            inventoryService.recordAdjustment(
                    productId,
                    AdjustmentDirection.NEGATIVE,
                    1,
                    "Consumo concurrente",
                    actorId,
                    "Administración");
            return true;
        } catch (ApplicationException exception) {
            assertThat(exception.problemType()).isEqualTo(ProblemType.CONFLICT);
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean attemptConcurrentCashOpening(
            UUID actorId,
            String displayName,
            String authority,
            CountDownLatch ready,
            CountDownLatch start)
            throws InterruptedException {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "cash-concurrency-test",
                "not-used",
                AuthorityUtils.createAuthorityList(authority));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent cash test did not start in time");
            }
            cashSessionService.open(new BigDecimal("10000"), actorId, displayName);
            return true;
        } catch (ApplicationException exception) {
            assertThat(exception.problemType()).isEqualTo(ProblemType.CONFLICT);
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean attemptConcurrentCancellation(
            UUID saleId, UUID actorId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "sale-cancellation-concurrency-test",
                "not-used",
                AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent cancellation test did not start in time");
            }
            saleCancellationService.cancel(
                    saleId,
                    new CancelSaleCommand(PaymentMethod.TRANSFER, "AnulaciÃ³n concurrente"),
                    actorId,
                    "AdministraciÃ³n");
            return true;
        } catch (ApplicationException exception) {
            assertThat(exception.problemType()).isEqualTo(ProblemType.CONFLICT);
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private <T extends org.springframework.mock.web.MockHttpServletRequest> T remoteAddress(
            T request, String sourceAddress) {
        request.setRemoteAddr(sourceAddress);
        return request;
    }

    private void createUser(
            String username,
            String password,
            UserRole role,
            UserStatus status,
            boolean passwordChangeRequired) {
        repository.create(new UserAccount(
                UUID.randomUUID(),
                username,
                username,
                passwordEncoder.encode(password),
                role,
                status,
                passwordChangeRequired));
    }
}
