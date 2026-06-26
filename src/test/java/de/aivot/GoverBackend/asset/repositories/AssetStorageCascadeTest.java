package de.aivot.GoverBackend.asset.repositories;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetStorageCascadeTest {
    @Test
    void providerScopedStorageIndexDeleteCascadesAssetsOnlyWithinSelectedProvider() throws SQLException {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");

        try (var connection = dataSource.getConnection()) {
            createSchema(connection);

            insertStorageProvider(connection, 1);
            insertStorageProvider(connection, 2);

            insertStorageIndexItem(connection, 1, "/images/", true);
            insertStorageIndexItem(connection, 1, "/images/provider-a.png", false);
            insertStorageIndexItem(connection, 1, "/documents/", true);
            insertStorageIndexItem(connection, 1, "/documents/readme.txt", false);

            insertStorageIndexItem(connection, 2, "/images/", true);
            insertStorageIndexItem(connection, 2, "/images/provider-b.png", false);

            insertAsset(connection, 1, "/images/provider-a.png");
            insertAsset(connection, 1, "/documents/readme.txt");
            insertAsset(connection, 2, "/images/provider-b.png");

            deleteFolderTree(connection, 1, "/images/");

            assertFalse(storageIndexItemExists(connection, 1, "/images/"));
            assertFalse(storageIndexItemExists(connection, 1, "/images/provider-a.png"));
            assertFalse(assetExists(connection, 1, "/images/provider-a.png"));

            assertTrue(storageIndexItemExists(connection, 1, "/documents/readme.txt"));
            assertTrue(assetExists(connection, 1, "/documents/readme.txt"));

            assertTrue(storageIndexItemExists(connection, 2, "/images/"));
            assertTrue(storageIndexItemExists(connection, 2, "/images/provider-b.png"));
            assertTrue(assetExists(connection, 2, "/images/provider-b.png"));
        }
    }

    private static void createSchema(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    create table storage_providers (
                        id integer primary key
                    )
                    """);
            statement.execute("""
                    create table storage_index_items (
                        storage_provider_id integer not null references storage_providers (id) on delete cascade,
                        path_from_root text not null,
                        directory boolean not null,
                        primary key (storage_provider_id, path_from_root)
                    )
                    """);
            statement.execute("""
                    create table assets (
                        id uuid primary key,
                        storage_provider_id integer not null,
                        storage_path_from_root text not null,
                        foreign key (storage_provider_id, storage_path_from_root)
                            references storage_index_items (storage_provider_id, path_from_root)
                            on delete cascade on update cascade
                    )
                    """);
        }
    }

    private static void insertStorageProvider(Connection connection, int providerId) throws SQLException {
        try (var statement = connection.prepareStatement("insert into storage_providers (id) values (?)")) {
            statement.setInt(1, providerId);
            statement.executeUpdate();
        }
    }

    private static void insertStorageIndexItem(Connection connection,
                                               int providerId,
                                               String pathFromRoot,
                                               boolean directory) throws SQLException {
        try (var statement = connection.prepareStatement("""
                insert into storage_index_items (storage_provider_id, path_from_root, directory)
                values (?, ?, ?)
                """)) {
            statement.setInt(1, providerId);
            statement.setString(2, pathFromRoot);
            statement.setBoolean(3, directory);
            statement.executeUpdate();
        }
    }

    private static void insertAsset(Connection connection,
                                    int providerId,
                                    String pathFromRoot) throws SQLException {
        try (var statement = connection.prepareStatement("""
                insert into assets (id, storage_provider_id, storage_path_from_root)
                values (?, ?, ?)
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setInt(2, providerId);
            statement.setString(3, pathFromRoot);
            statement.executeUpdate();
        }
    }

    private static void deleteFolderTree(Connection connection,
                                         int providerId,
                                         String folderPath) throws SQLException {
        try (var statement = connection.prepareStatement("""
                delete from storage_index_items
                where storage_provider_id = ?
                  and (
                      path_from_root = ?
                      or path_from_root like ?
                  )
                """)) {
            statement.setInt(1, providerId);
            statement.setString(2, folderPath);
            statement.setString(3, folderPath + "%");
            statement.executeUpdate();
        }
    }

    private static boolean storageIndexItemExists(Connection connection,
                                                  int providerId,
                                                  String pathFromRoot) throws SQLException {
        try (var statement = connection.prepareStatement("""
                select count(*)
                from storage_index_items
                where storage_provider_id = ? and path_from_root = ?
                """)) {
            statement.setInt(1, providerId);
            statement.setString(2, pathFromRoot);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static boolean assetExists(Connection connection,
                                       int providerId,
                                       String pathFromRoot) throws SQLException {
        try (var statement = connection.prepareStatement("""
                select count(*)
                from assets
                where storage_provider_id = ? and storage_path_from_root = ?
                """)) {
            statement.setInt(1, providerId);
            statement.setString(2, pathFromRoot);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }
}
