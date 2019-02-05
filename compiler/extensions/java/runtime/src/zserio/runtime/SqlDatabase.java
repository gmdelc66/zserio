package zserio.runtime;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The base class for SQL database classes generated by Zserio.
 * <p>
 * SQL database can be created using two different constructors:</p>
 * <ul>
 * <li>Constructor using database file name. Then this class handles SQLite connection by itself.</li>
 * <li>Constructor using SQLite connection. Then this class does not handle SQLite connection.</li>
 * </ul>
 * <p>
 * This base class supports table relocations. If some tables are relocated to the different databases, then
 * caller must reflect these relocations in constructor parameter <code>tableToDbFileNameRelocationMap</code> or
 * <code>tableToAttachedDbNameRelocationMap</code>.</p>
 * <p>
 * The parameter <code>tableToDbFileNameRelocationMap</code> is used by the constructor using
 * database file name and it maps table name to file name of database where the table is relocated. Then this
 * class attaches all specified databases automatically. Example:</p>
 * <pre>
 * final Map&lt;String, String&gt; tableToDbFileNameRelocationMap = new HashMap&lt;String, String&gt;();
 * tableToDbFileNameRelocationMap.put("slovakia", "db_europe.sqlite");
 * final AmericaDb americaDb = new AmericaDb("db_america.sqlite", Mode.CREATE, tableToDbFileNameRelocationMap);
 * ...
 * americaDb.close()</pre>
 * <p>
 * The parameter <code>tableToAttachedDbNameRelocationMap</code> is used by the constructor using
 * SQLite connection and it maps table name to alias name of database where the table is relocated. In this
 * case, caller is responsible for attaching all specified databases. Example:</p>
 * <pre>
 * final Properties connectionProps = new Properties();
 * connectionProps.setProperty("flags", "CREATE");
 * final String uriPath = "jdbc:sqlite:file:db_america.sqlite";
 * final Connection connection = DriverManager.getConnection(uriPath, connectionProps);
 *
 * final String aliasDbName = "db_europe";
 * final String attachSqlQuery = new StringBuilder("ATTACH DATABASE 'file:db_europe.sqlite' AS " + aliasDbName);
 * final Statement attachStatement = connection.createStatement();
 * attachStatement.executeUpdate(attachSqlQuery);
 * attachStatement.close();
 *
 * final Map&lt;String, String&gt; tableToAttachedDbNameRelocationMap = new HashMap&lt;String, String&gt;();
 * tableToAttachedDbNameRelocationMap.put("slovakia", aliasDbName);
 * final AmericaDb americaDb = new AmericaDb(connection, tableToAttachedDbNameRelocationMap);
 * ...
 * final String detachSqlQuery = "DETACH DATABASE " + aliasDbName;
 * final Statement detachStatement = connection.createStatement();
 * detachStatement.executeUpdate(detachSqlQuery);
 * detachStatement.close();
 *
 * connection.close();</pre>
 */
public class SqlDatabase
{
    /**
     * The enumeration for the database access mode.
     */
    public enum Mode
    {
        /** The read only mode. */
        READONLY,

        /** The write mode. */
        WRITE,

        /** The creation mode. */
        CREATE
    }

    /**
     * Constructs a new SQL database with the given file path and the given mode.
     * <p>
     * This constructor creates SQLite connection and attaches possible databases specified by map
     * <code>tableToDbFileNameRelocationMap</code>.</p>
     *
     * @param fileName                       The path to the SQLite database.
     * @param mode                           The database mode.
     * @param tableToDbFileNameRelocationMap Table to database file name relocation map.
     *
     * @throws SQLException       If the connection cannot be resolved.
     * @throws URISyntaxException If the path to the SQLite database cannot be resolved.
     */
    public SqlDatabase(String fileName, Mode mode, Map<String, String> tableToDbFileNameRelocationMap)
            throws SQLException, URISyntaxException
    {
        final Properties connectionProps = new Properties();
        connectionProps.setProperty("flags", mode.toString());
        final String uriPath = "jdbc:sqlite:file:" + new File(fileName).toString();

        connection = DriverManager.getConnection(uriPath, connectionProps);
        isExternal = false;

        tableNameToAttachedDbNameMap = new HashMap<String, String>();
        dbFileNameToAttachedDbNameMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : tableToDbFileNameRelocationMap.entrySet())
        {
            final String relocatedTableName = entry.getKey();
            final String dbFileName = entry.getValue();

            String attachedDbName = dbFileNameToAttachedDbNameMap.get(dbFileName);
            if (attachedDbName == null)
            {
                attachedDbName = this.getClass().getSimpleName() + "_" + relocatedTableName;
                attachDatabase(dbFileName, attachedDbName);
                dbFileNameToAttachedDbNameMap.put(dbFileName, attachedDbName);
            }

            tableNameToAttachedDbNameMap.put(relocatedTableName, attachedDbName);
        }
    }

    /**
     * Constructs a new SQL database object with the given connection.
     *
     * @param externalConnection                 Database connection to use.
     * @param tableToAttachedDbNameRelocationMap Table to database name relocation map.
     */
    public SqlDatabase(Connection externalConnection, Map<String, String> tableToAttachedDbNameRelocationMap)
    {
        connection = externalConnection;
        isExternal = true;
        tableNameToAttachedDbNameMap = tableToAttachedDbNameRelocationMap;
        dbFileNameToAttachedDbNameMap = null;
    }

    /**
     * Closes the database connection.
     * <p>
     * The database is closed only if this object has been created by constructor using database file name.</p>
     *
     * @throws SQLException If the connection cannot be closed.
     */
    public void close() throws SQLException
    {
        if (!isExternal)
        {
            detachDatabases();
            connection.close();
        }
    }

    /**
     * Gets the underlying database connection.
     *
     * @return The database connection.
     */
    public Connection getConnection()
    {
        return connection;
    }

    /**
     * Executes the given SQL statement.
     *
     * It may be an INSERT, UPDATE, or DELETE statement or an SQL statement that returns nothing, such as
     * an SQL DDL statement.
     *
     * @param sql SQL statement to execute.
     *
     * @throws SQLException If a database access error occurs.
     */
    public void executeUpdate(String sql) throws SQLException
    {
        final Statement statement = connection.createStatement();
        try
        {
            statement.executeUpdate(sql);
        }
        finally
        {
            statement.close();
        }
    }

    /**
     * Prepares the given SQL statement.
     *
     * It may be an INSERT, UPDATE, or DELETE statement or an SQL statement that returns nothing, such as
     * an SQL DDL statement.
     *
     * @param sql SQL statement to prepare.
     *
     * @return The prepared statement.
     *
     * @throws SQLException If a database access error occurs.
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
         return connection.prepareStatement(sql);
    }

    /**
     * Gets current auto-commit mode.
     *
     * @return true if auto-commit mode is active, otherwise false.
     *
     * @throws SQLException If a database access error occurs.
     */
    public boolean getAutoCommit() throws SQLException
    {
        return connection.getAutoCommit();
    }

    /**
     * Sets auto-commit mode to the given state.
     *
     * @param autoCommit true to enable auto-commit mode or false to disable it.
     *
     * @throws SQLException If a database access error occurs.
     */
    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        connection.setAutoCommit(autoCommit);
    }

    /**
     * Makes all changes made since the previous commit/rollback permanent.
     *
     * @throws SQLException If a database access error occurs.
     */
    public void commit() throws SQLException
    {
        connection.commit();
    }

    /**
     * Starts transaction if it is not started.
     *
     * @return true if transaction has been started, otherwise false.
     *
     * @throws SQLException If a database access error occurs.
     */
    public boolean startTransaction() throws SQLException
    {
        boolean wasTransactionStarted = false;
        if (getAutoCommit())
        {
            setAutoCommit(false);
            wasTransactionStarted = true;
        }

        return wasTransactionStarted;
    }

    /**
     * Ends started transaction.
     *
     * @param wasTransactionStarted true if transaction should be ended, otherwise false.
     *
     * @throws SQLException If a database access error occurs.
     */
    public void endTransaction(boolean wasTransactionStarted) throws SQLException
    {
        if (wasTransactionStarted)
        {
            commit();
            setAutoCommit(true);
        }
    }

    /**
     * Gets alias name of attached database for given relocated table.
     *
     * @param relocatedTableName Name of relocated table.
     *
     * @return Alias name of attached database for given relocated table or <code>null</code> if given relocated
     * table has not been found.
     */
    protected String getAttachedDbName(String relocatedTableName)
    {
        return tableNameToAttachedDbNameMap.get(relocatedTableName);
    }

    private void attachDatabase(String dbFileName, String dbName) throws SQLException
    {
        final StringBuilder sqlQuery = new StringBuilder("ATTACH DATABASE '");
        sqlQuery.append(new File(dbFileName).toString()); // use File to handle separators corretly
        sqlQuery.append("' AS ");
        sqlQuery.append(dbName);
        executeUpdate(sqlQuery.toString());
    }

    private void detachDatabases() throws SQLException
    {
        for (String attachedDbName : dbFileNameToAttachedDbNameMap.values())
        {
            final String sqlQuery = "DETACH DATABASE " + attachedDbName;
            executeUpdate(sqlQuery);
        }
    }

    private final Connection    connection;     /** The database connection. */
    private final boolean       isExternal;     /** true if database connection will be closed by caller. */

    /**
     * Table relocation map. It maps table name to the corresponded attached database name. It contains
     * only tables which were relocated to database different from database given by Zserio.
     */
    private final Map<String, String>   tableNameToAttachedDbNameMap;

    /**
     * Database file name map. It maps database file name to the attached database name. It contains only
     * database file names to which some table was relocated.
     */
    private final Map<String, String>   dbFileNameToAttachedDbNameMap;
}
