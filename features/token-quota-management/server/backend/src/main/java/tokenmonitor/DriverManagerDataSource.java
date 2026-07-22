package tokenmonitor;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/** Minimal DataSource for deployment bootstrap. Replace with the platform connection pool in production. */
public final class DriverManagerDataSource implements DataSource {
    private final String url;
    private final String user;
    private final String password;

    public DriverManagerDataSource(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override public Connection getConnection() throws SQLException { return DriverManager.getConnection(url, user, password); }
    @Override public Connection getConnection(String username, String pwd) throws SQLException {
        return DriverManager.getConnection(url, username, pwd);
    }
    @Override public PrintWriter getLogWriter() { return DriverManager.getLogWriter(); }
    @Override public void setLogWriter(PrintWriter out) { DriverManager.setLogWriter(out); }
    @Override public void setLoginTimeout(int seconds) { DriverManager.setLoginTimeout(seconds); }
    @Override public int getLoginTimeout() { return DriverManager.getLoginTimeout(); }
    @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("parent logger is not supported");
    }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("not a wrapper for " + iface.getName());
    }
    @Override public boolean isWrapperFor(Class<?> iface) { return iface.isInstance(this); }
}
