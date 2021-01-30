package cloud.apposs.cachex.storage.jdbc;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public final class ConnectionListenerSupport {
	private final List<ConnectionListener> listeners = new LinkedList<ConnectionListener>();
	
	public void add(ConnectionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
	
	public void remove(ConnectionListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

	public void fireConnectionInValid(ConnectionWrapper connection) {
		for (ConnectionListener listener : listeners) {
        	listener.connectionInvalid(connection);
        }
	}

	public void firePoolInvalid(SQLException e) {
		for (ConnectionListener listener : listeners) {
        	listener.poolInvalid(e);
        }
	}

	public void fireConnectionCreated(ConnectionWrapper connection) {
		for (ConnectionListener listener : listeners) {
        	listener.connectionCreated(connection);
        }
	}

	public void fireConnectionRetrived(ConnectionWrapper connection) {
		for (ConnectionListener listener : listeners) {
        	listener.connectionRetrived(connection);
        }
	}
}
