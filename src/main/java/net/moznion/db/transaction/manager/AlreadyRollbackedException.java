package net.moznion.db.transaction.manager;

/**
 * Thrown to indicate that a transaction has been already rollbacked by another {@code rollback()}.
 * 
 * @author moznion
 *
 */
public class AlreadyRollbackedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an {@code AlreadyRollbackedException} with the specified detail message.
	 * 
	 * @param message the detail message.
	 */
	public AlreadyRollbackedException(String message) {
		super(message);
	}
}
