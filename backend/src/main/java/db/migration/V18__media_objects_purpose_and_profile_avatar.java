package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V18__media_objects_purpose_and_profile_avatar extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        addColumnIfMissing(connection, "media_objects", "purpose", "VARCHAR(30) NOT NULL DEFAULT 'CLIENT_CONTENT'");
        addConstraintIfMissing(connection, "media_objects", "media_objects_purpose_check",
                "ALTER TABLE media_objects ADD CONSTRAINT media_objects_purpose_check CHECK (purpose IN ('CLIENT_CONTENT', 'PROFILE_AVATAR'))");

        addColumnIfMissing(connection, "app_users", "profile_media_id", "UUID");
        addConstraintIfMissing(connection, "app_users", "app_users_profile_media_fk",
                "ALTER TABLE app_users ADD CONSTRAINT app_users_profile_media_fk FOREIGN KEY (profile_media_id) REFERENCES media_objects(id) ON DELETE SET NULL");

        replaceOwnerUserForeignKey(connection);
    }

    private static void replaceOwnerUserForeignKey(Connection connection) throws Exception {
        Set<String> constraints = ownerUserForeignKeyNames(connection);
        constraints.add("media_objects_owner_user_fk");

        try (Statement statement = connection.createStatement()) {
            for (String constraint : constraints) {
                if (constraintExists(connection, "media_objects", constraint)) {
                    statement.execute("ALTER TABLE media_objects DROP CONSTRAINT " + quoteIdentifier(constraint));
                }
            }

            statement.execute("""
                    ALTER TABLE media_objects
                    ADD CONSTRAINT media_objects_owner_user_fk
                    FOREIGN KEY (owner_user_id)
                    REFERENCES app_users(id)
                    ON DELETE RESTRICT
                    """);
        }
    }

    private static Set<String> ownerUserForeignKeyNames(Connection connection) throws Exception {
        Set<String> constraints = new LinkedHashSet<>();
        DatabaseMetaData metadata = connection.getMetaData();

        for (String tableName : Set.of("media_objects", "MEDIA_OBJECTS")) {
            try (ResultSet importedKeys = metadata.getImportedKeys(connection.getCatalog(), null, tableName)) {
                while (importedKeys.next()) {
                    String fkColumn = importedKeys.getString("FKCOLUMN_NAME");
                    String pkTable = importedKeys.getString("PKTABLE_NAME");
                    String fkName = importedKeys.getString("FK_NAME");
                    if (equalsIdentifier(fkColumn, "owner_user_id") && equalsIdentifier(pkTable, "app_users") && fkName != null && !fkName.isBlank()) {
                        constraints.add(fkName);
                    }
                }
            }
        }

        constraints.addAll(ownerUserForeignKeyNamesFromInformationSchema(connection));

        return constraints;
    }

    private static Set<String> ownerUserForeignKeyNamesFromInformationSchema(Connection connection) {
        Set<String> constraints = new LinkedHashSet<>();

        try (var statement = connection.prepareStatement("""
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema = kcu.table_schema
                JOIN information_schema.referential_constraints rc
                  ON tc.constraint_name = rc.constraint_name
                 AND tc.constraint_schema = rc.constraint_schema
                JOIN information_schema.constraint_column_usage ccu
                  ON rc.unique_constraint_name = ccu.constraint_name
                 AND rc.unique_constraint_schema = ccu.constraint_schema
                WHERE LOWER(tc.table_name) = LOWER('media_objects')
                  AND LOWER(kcu.column_name) = LOWER('owner_user_id')
                  AND LOWER(ccu.table_name) = LOWER('app_users')
                  AND tc.constraint_type = 'FOREIGN KEY'
                """)) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String constraint = result.getString("constraint_name");
                    if (constraint != null && !constraint.isBlank()) {
                        constraints.add(constraint);
                    }
                }
            }
        } catch (Exception ignored) {
            return Set.of();
        }

        return constraints;
    }

    private static void addColumnIfMissing(Connection connection, String table, String column, String definition) throws Exception {
        if (columnExists(connection, table, column)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static void addConstraintIfMissing(Connection connection, String table, String constraint, String sql) throws Exception {
        if (constraintExists(connection, table, constraint)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        for (String tableName : Set.of(table, table.toUpperCase(Locale.ROOT))) {
            for (String columnName : Set.of(column, column.toUpperCase(Locale.ROOT))) {
                try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                    if (columns.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean constraintExists(Connection connection, String table, String constraint) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT 1
                FROM information_schema.table_constraints
                WHERE LOWER(table_name) = LOWER(?)
                  AND LOWER(constraint_name) = LOWER(?)
                """)) {
            statement.setString(1, table);
            statement.setString(2, constraint);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static boolean equalsIdentifier(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }
}
