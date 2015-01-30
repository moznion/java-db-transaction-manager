package net.moznion.db.transaction.manager;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Represents the stack traced information for transaction.
 * 
 * @author moznion
 *
 */
@Getter
public class TransactionTraceInfo {
	final static String UNKNOWN_SYMBOL = "Unknown";
	final static int UNKNOWN_NUM = -1;

	private final String className;
	private final String fileName;
	private final String methodName;
	private final int lineNumber;
	private final long threadId;

	/**
	 * Builder of traced information for transaction.
	 * 
	 * <p>
	 * This class provides fluent accessors for each fields. You can specify the
	 * field by method chaining.<br>
	 * If field isn't specified it will be "Unknown" (when filed is String) or
	 * {@code -1} (when field is num).
	 * </p>
	 */
	@Accessors(fluent = true)
	@Setter
	public static class Builder {
		private String className = UNKNOWN_SYMBOL;
		private String fileName = UNKNOWN_SYMBOL;
		private String methodName = UNKNOWN_SYMBOL;
		private int lineNumber = UNKNOWN_NUM;
		private long threadId = UNKNOWN_NUM;

		/**
		 * Construct new instance of TransactionTraceInfo based on builder.
		 * 
		 * @return new instance of TransactionTraceInfo
		 */
		public TransactionTraceInfo build() {
			return new TransactionTraceInfo(this);
		}
	}

	/**
	 * Return new builder for this instance.
	 * 
	 * @return builder for this instance.
	 */
	public static Builder builder() {
		return new Builder();
	}

	private TransactionTraceInfo(Builder b) {
		if (b.className == null) {
			className = UNKNOWN_SYMBOL;
		} else {
			className = b.className;
		}

		if (b.fileName == null) {
			fileName = UNKNOWN_SYMBOL;
		} else {
			fileName = b.fileName;
		}

		if (b.methodName == null) {
			methodName = UNKNOWN_SYMBOL;
		} else {
			methodName = b.methodName;
		}

		this.lineNumber = b.lineNumber;
		this.threadId = b.threadId;
	}
	@Override
	public String toString() {
		return new StringBuilder().append("File Name: ").append(fileName)
				.append(", Class Name: ").append(className)
				.append(", Method Name: ").append(methodName)
				.append(", Line Number: ").append(lineNumber)
				.append(", Thread ID: ").append(threadId).toString();
	}
}
