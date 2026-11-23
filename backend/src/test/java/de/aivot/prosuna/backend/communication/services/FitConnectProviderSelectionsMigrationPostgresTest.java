package de.aivot.prosuna.backend.communication.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "PROSUNA_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class FitConnectProviderSelectionsMigrationPostgresTest {
    private static final String DEFINITION_KEY = "de.aivot.core.fit_connect_zbp_communication_provider";
    private static final String TEST_ENVIRONMENT = "\"environment\":{\"type\":\"Literal\",\"value\":\"TEST\"}";
    private static final String ONLINE_SERVICE = "\"senderDestinationType\":{\"type\":\"Literal\",\"value\":\"ONLINE_SERVICE\"}";
    private static final String DEFAULT_SELECTIONS = "{" + TEST_ENVIRONMENT + "," + ONLINE_SERVICE + "}";

    @Test
    void backfillsFormerDefaultsWithoutChangingExplicitValuesOrOtherProviders() throws Exception {
        var cases = List.of(
                new ConfigurationCase("Missing selections", "{}", DEFAULT_SELECTIONS),
                new ConfigurationCase("Null entries", "{\"environment\":null,\"senderDestinationType\":null}", DEFAULT_SELECTIONS),
                new ConfigurationCase("Null literal values", """
                        {"environment":{"type":"Literal","value":null},"senderDestinationType":{"type":"Literal","value":null}}
                        """, DEFAULT_SELECTIONS),
                new ConfigurationCase("Missing literal values", """
                        {"environment":{"type":"Literal"},"senderDestinationType":{"type":"Literal"}}
                        """, DEFAULT_SELECTIONS),
                new ConfigurationCase("Existing environment", """
                        {"environment":{"type":"Literal","value":"STAGE"}}
                        """, "{\"environment\":{\"type\":\"Literal\",\"value\":\"STAGE\"}," + ONLINE_SERVICE + "}"),
                new ConfigurationCase("Existing destination type", """
                        {"senderDestinationType":{"type":"Literal","value":"ADMINISTRATION"}}
                        """, "{" + TEST_ENVIRONMENT + ",\"senderDestinationType\":{\"type\":\"Literal\",\"value\":\"ADMINISTRATION\"}}"),
                new ConfigurationCase("Unrelated configuration", "{\"other\":{\"type\":\"Literal\",\"value\":\"keep\"}}",
                        "{" + TEST_ENVIRONMENT + "," + ONLINE_SERVICE + ",\"other\":{\"type\":\"Literal\",\"value\":\"keep\"}}"),
                unchanged("Explicit selections", """
                        {"environment":{"type":"Literal","value":"PROD"},"senderDestinationType":{"type":"Literal","value":"ADMINISTRATION"}}
                        """),
                unchanged("Empty selections", """
                        {"environment":{"type":"Literal","value":""},"senderDestinationType":{"type":"Literal","value":""}}
                        """),
                unchanged("Whitespace selections", """
                        {"environment":{"type":"Literal","value":" "},"senderDestinationType":{"type":"Literal","value":" "}}
                        """),
                unchanged("Unsupported selections", """
                        {"environment":{"type":"Literal","value":"UNKNOWN"},"senderDestinationType":{"type":"Literal","value":"UNKNOWN"}}
                        """),
                unchanged("Dynamic envelopes", """
                        {"environment":{"type":"Javascript"},"senderDestinationType":{"type":"NoCode"}}
                        """),
                unchanged("Malformed envelopes", "{\"environment\":{},\"senderDestinationType\":\"ADMINISTRATION\"}"),
                unchanged("Non-object configuration", "null"),
                new ConfigurationCase("Different definition", "{}", "{}", "de.aivot.core.mail_communication_provider", 1),
                new ConfigurationCase("Different version", "{}", "{}", DEFINITION_KEY, 2)
        );

        var properties = new Properties();
        for (var key : new String[]{"user", "password"}) {
            var value = System.getenv("PROSUNA_TEST_POSTGRES_" + key.toUpperCase(Locale.ROOT));
            if (value != null) properties.setProperty(key, value);
        }

        // Use only a temporary table and roll back, even when an existing database is supplied.
        try (var connection = DriverManager.getConnection(System.getenv("PROSUNA_TEST_POSTGRES_URL"), properties)) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.createStatement()) {
                    statement.execute("set local search_path = pg_temp");
                    statement.execute("""
                            create temporary table communication_providers (
                                id integer primary key,
                                communication_provider_definition_key text not null,
                                communication_provider_definition_version integer not null,
                                configuration jsonb not null,
                                name text not null,
                                is_enabled boolean not null,
                                is_test_provider boolean not null,
                                lock_version bigint not null
                            )
                            """);
                }
                try (var insert = connection.prepareStatement("""
                        insert into communication_providers values (?, ?, ?, ?::jsonb, ?, ?, ?, 17)
                        """)) {
                    for (int index = 0; index < cases.size(); index++) {
                        var testCase = cases.get(index);
                        insert.setInt(1, index);
                        insert.setString(2, testCase.definitionKey());
                        insert.setInt(3, testCase.definitionVersion());
                        insert.setString(4, testCase.configuration());
                        insert.setString(5, testCase.name());
                        insert.setBoolean(6, index % 2 == 0);
                        insert.setBoolean(7, index % 3 == 0);
                        insert.executeUpdate();
                    }
                }

                var migration = new ClassPathResource("db/migration/V1_13_0__require_fit_connect_provider_selections.sql");
                ScriptUtils.executeSqlScript(connection, migration);
                assertProviders(connection, cases);
                ScriptUtils.executeSqlScript(connection, migration);
                assertProviders(connection, cases);
            } finally {
                connection.rollback();
            }
        }
    }

    private static void assertProviders(Connection connection, List<ConfigurationCase> cases) throws Exception {
        try (var select = connection.prepareStatement("""
                select configuration = ?::jsonb as matches_configuration,
                       communication_provider_definition_key, communication_provider_definition_version,
                       name, is_enabled, is_test_provider, lock_version
                from communication_providers where id = ?
                """)) {
            for (int index = 0; index < cases.size(); index++) {
                var testCase = cases.get(index);
                select.setString(1, testCase.expectedConfiguration());
                select.setInt(2, index);
                try (var row = select.executeQuery()) {
                    assertTrue(row.next(), testCase.name());
                    assertTrue(row.getBoolean("matches_configuration"), testCase.name());
                    assertEquals(testCase.definitionKey(), row.getString("communication_provider_definition_key"), testCase.name());
                    assertEquals(testCase.definitionVersion(), row.getInt("communication_provider_definition_version"), testCase.name());
                    assertEquals(testCase.name(), row.getString("name"));
                    assertEquals(index % 2 == 0, row.getBoolean("is_enabled"), testCase.name());
                    assertEquals(index % 3 == 0, row.getBoolean("is_test_provider"), testCase.name());
                    assertEquals(17L, row.getLong("lock_version"), testCase.name());
                    assertFalse(row.next());
                }
            }
        }
    }

    private static ConfigurationCase unchanged(String name, String configuration) {
        return new ConfigurationCase(name, configuration, configuration);
    }

    private record ConfigurationCase(String name, String configuration, String expectedConfiguration,
                                     String definitionKey, int definitionVersion) {
        private ConfigurationCase(String name, String configuration, String expectedConfiguration) {
            this(name, configuration, expectedConfiguration, DEFINITION_KEY, 1);
        }
    }
}
