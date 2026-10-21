package com.yms.yms;

import com.yms.yms.domain.finance.entity.ReportSchedule;
import com.yms.yms.domain.finance.repository.ReportScheduleRepository;
import com.yms.yms.domain.partner.entity.Partner;
import com.yms.yms.domain.partner.repository.PartnerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgresDatabaseIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        properties.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        properties.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ReportScheduleRepository schedules;

    @Autowired
    private PartnerRepository partners;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void migrationsConnectToPostgresAndPersistNewReportSchedule() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getDatabaseProductName().contains("PostgreSQL"));
        }

        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-19T02:00:00Z");
        ReportSchedule schedule = new ReportSchedule();
        schedule.setId(id);
        schedule.setReportType("MONTHLY");
        schedule.setRecipientEmail("db-integration-" + id + "@example.test");
        schedule.setActive(true);
        schedule.setCreatedAt(createdAt);

        schedules.saveAndFlush(schedule);

        ReportSchedule saved = schedules.findById(id).orElseThrow();
        assertEquals("MONTHLY", saved.getReportType());
        assertEquals(schedule.getRecipientEmail(), saved.getRecipientEmail());
        assertTrue(saved.isActive());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM report_schedules WHERE id = ? AND report_type = ?",
                Integer.class, id, "MONTHLY"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void savesPartnerThroughJpaAndReadsThePersistedBusinessFieldsFromPostgres() {
        UUID id = UUID.randomUUID();
        String code = "PG-IT-" + id;
        Partner partner = new Partner();
        partner.setId(id);
        partner.setCode(code);
        partner.setName("PostgreSQL integration partner");
        partner.setCountryCode("VN");
        partner.setPartnerType("A");
        partner.setContactEmail("partner-" + id + "@example.test");

        partners.saveAndFlush(partner);

        Partner saved = partners.findByCode(code).orElseThrow();
        assertEquals(id, saved.getId());
        assertEquals("PostgreSQL integration partner", saved.getName());
        assertEquals(partner.getContactEmail(), saved.getContactEmail());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM partners WHERE id = ? AND code = ? AND contact_email = ?",
                Integer.class, id, code, partner.getContactEmail()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void partnerCreateApiCommitsDataThatIsVisibleInPostgres() throws Exception {
        String code = "PG-API-" + UUID.randomUUID();
        String email = "partner-" + UUID.randomUUID() + "@example.test";
        String request = """
                {
                  "code": "%s",
                  "name": "Partner created through API",
                  "countryCode": "VN",
                  "partnerType": "A",
                  "contactEmail": "%s"
                }
                """.formatted(code, email);

        mockMvc.perform(post("/api/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(code));

        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM partners WHERE code = ? AND contact_email = ?",
                Integer.class, code, email));
    }
}
