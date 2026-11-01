package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
	"spring.profiles.active=test",
	"spring.datasource.url=jdbc:h2:mem:castfan-migration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
	"spring.jpa.hibernate.ddl-auto=validate",
	"spring.flyway.enabled=true",
	"spring.flyway.locations=classpath:db/migration/mysql",
	"spring.flyway.validate-migration-naming=true"
})
class DatabaseMigrationTests {

	private static final List<String> EXPECTED_TABLES = List.of(
		"account_avatars", "cast_content", "cast_content_images", "cast_follows", "cast_profiles", "flyway_schema_history",
		"member_point_transactions", "member_profiles", "member_tiers", "shift_schedules", "store_cast", "store_event_images", "store_events",
		"store_promotion_images", "store_promotions", "stores", "tier_memberships", "user_accounts"
	);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Flyway flyway;

	@Test
	void migrationCreatesTheSchemaAndPassesHibernateValidation() {
		var jdbc = new JdbcTemplate(dataSource);
		var tables = jdbc.queryForList("""
			select lower(table_name)
			from information_schema.tables
			where table_schema = 'public' and table_type = 'BASE TABLE'
			order by table_name
			""", String.class);

		assertThat(tables).containsAll(EXPECTED_TABLES);
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("8");
		assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
	}
}
