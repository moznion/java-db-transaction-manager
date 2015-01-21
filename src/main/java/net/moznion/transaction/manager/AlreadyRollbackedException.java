package net.moznion.transaction.manager;

public class AlreadyRollbackedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public AlreadyRollbackedException(String message) {
		super(message);
	}
}
