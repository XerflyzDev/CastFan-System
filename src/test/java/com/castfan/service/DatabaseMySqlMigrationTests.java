package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DatabaseMetaData;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledIfEnvironmentVariable(named = "CASTFAN_MYSQL_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
	"spring.profiles.active=prod",
	"spring.datasource.url=${CASTFAN_MYSQL_TEST_URL}",
	"spring.datasource.username=${CASTFAN_MYSQL_TEST_USERNAME:castfan}",
	"spring.datasource.password=${CASTFAN_MYSQL_TEST_PASSWORD:castfan_test}"
})
class DatabaseMySqlMigrationTests {

	private static final List<String> EXPECTED_TABLES = List.of(
		"account_avatars", "cast_content", "cast_content_images", "cast_follows", "cast_profiles", "flyway_schema_history",
		"member_point_transactions", "member_profiles", "store_memberships", "shift_schedules", "store_cast", "store_event_images", "store_events",
		"store_promotion_images", "store_promotions", "stores", "user_accounts"
	);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Flyway flyway;

	@Test
	void migrationCreatesSchemaAndPassesHibernateValidationOnMySql() throws Exception {
		try (var connection = dataSource.getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			assertThat(metadata.getDatabaseProductName()).isEqualTo("MySQL");
		}

		var jdbc = new JdbcTemplate(dataSource);
		var tables = jdbc.queryForList("""
			select lower(table_name)
			from information_schema.tables
			where table_schema = database() and table_type = 'BASE TABLE'
			order by table_name
			""", String.class);

		assertThat(tables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
		assertThat(tables).doesNotContain("member_tiers", "tier_memberships");
		var contentColumns = jdbc.queryForList("""
			select lower(column_name)
			from information_schema.columns
			where table_schema = database() and table_name = 'cast_content'
			""", String.class);
		assertThat(contentColumns)
			.contains("minimum_store_tier_rank")
			.doesNotContain("minimum_tier_id");
		var promotionColumns = jdbc.queryForList("""
			select lower(column_name)
			from information_schema.columns
			where table_schema = database() and table_name = 'store_promotions'
			""", String.class);
		assertThat(promotionColumns).contains("status").doesNotContain("active");
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("12");
		assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
	}
}
