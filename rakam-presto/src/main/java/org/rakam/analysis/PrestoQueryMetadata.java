package org.rakam.analysis;

import com.google.common.base.Throwables;
import com.google.inject.name.Named;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.rakam.plugin.ContinuousQuery;
import org.rakam.report.PrestoConfig;
import org.rakam.report.PrestoQueryExecutor;
import org.rakam.report.QueryResult;
import org.rakam.util.RakamException;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PrestoQueryMetadata extends JDBCQueryMetadata {
    private final Connection prestoConnection;
    private final DatabaseMetaData prestoMetadata;
    private final PrestoQueryExecutor executor;
    private final PrestoConfig config;

    @Inject
    public PrestoQueryMetadata(@Named("report.metadata.store.jdbc") JDBCPoolDataSource dataSource, PrestoConfig config, PrestoQueryExecutor executor) {
        super(dataSource);
        this.executor = executor;
        this.config = config;

        Properties properties = new Properties();
        properties.put("user", "presto-rakam");
        try {
            this.prestoConnection = DriverManager.getConnection("jdbc:presto://"+config.getAddress().getHost()+":"+config.getAddress().getPort(), properties);
            this.prestoMetadata = prestoConnection.getMetaData();
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void createContinuousQuery(ContinuousQuery report) {
        QueryResult join = executor.executeRawStatement(String.format("CREATE VIEW %s.%s.%s AS %s",
                "streaming", report.project(), report.tableName, report.query))
                .getResult().join();
        if (join.isFailed()) {
            throw new RakamException(join.getError().message, HttpResponseStatus.BAD_REQUEST);
        }
    }

    @Override
    public void deleteContinuousQuery(String project, String tableName) {
        QueryResult join = executor.executeRawStatement(String.format("DROP VIEW %s.%s.%s",
                "streaming", project, tableName))
                .getResult().join();
        if (join.isFailed()) {
            throw new RakamException(join.getError().message, HttpResponseStatus.BAD_REQUEST);
        }
    }

    @Override
    public List<ContinuousQuery> getContinuousQueries(String project) {
        try {
            ArrayList<ContinuousQuery> continuousQueries = new ArrayList<>();
            ResultSet streaming = prestoMetadata.getTables("streaming", project, null, new String[]{"VIEW"});
            while(streaming.next()) {
//                continuousQueries.add(new ContinuousQuery(streaming.getString("table_schem"), streaming.getString("table_name"), streaming.getString("table_name"), "select 1", ImmutableList.of(), ImmutableList.of()));
            }
            return continuousQueries;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public ContinuousQuery getContinuousQuery(String project, String tableName) {
        try {
            ResultSet streaming = prestoMetadata.getTables("streaming", project, tableName, new String[]{"VIEW"});
            if(streaming.next()) {
//                return new ContinuousQuery(streaming.getString("table_schem"), streaming.getString("table_name"), streaming.getString("table_name"), "select 1", ImmutableList.of(), ImmutableList.of());
            }
            return null;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public List<ContinuousQuery> getAllContinuousQueries() {
        try {
            ArrayList<ContinuousQuery> continuousQueries = new ArrayList<>();
            ResultSet streaming = prestoMetadata.getTables("streaming", null, null, new String[]{"VIEW"});
            while(streaming.next()) {
//                continuousQueries.add(new ContinuousQuery(streaming.getString("table_schem"), streaming.getString("table_name"), streaming.getString("table_name"), "select 1", ImmutableList.of(), ImmutableList.of()));
            }
            return continuousQueries;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }
}