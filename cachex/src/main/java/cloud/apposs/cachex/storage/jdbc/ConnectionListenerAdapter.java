package cloud.apposs.cachex.storage.jdbc;

import java.sql.SQLException;

public class ConnectionListenerAdapter implements ConnectionListener {
	@Override
	public void connectionCreated(ConnectionWrapper connection) {
	}

	@Override
	public void connectionInvalid(ConnectionWrapper connection) {
	}

	@Override
	public void connectionRetrived(ConnectionWrapper connection) {
	}

	@Override
	public void poolInvalid(SQLException e) {
	}
}
