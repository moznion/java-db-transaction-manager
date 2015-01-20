package net.moznion.transaction.manager;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
public class TransactionTrace {
	private final String className;
	private final String fileName;
	private final String methodName;
	private final int lineNumber;
	private final long threadId;

	@Setter
	@Accessors(fluent = true)
	public static class Builder {
		private String className = "";
		private String fileName = "";
		private String methodName = "";
		private int lineNumber = 0;
		private long threadId = 0;

		public TransactionTrace build() {
			return new TransactionTrace(this);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	private TransactionTrace(Builder b) {
		className = b.className;
		fileName = b.fileName;
		methodName = b.methodName;
		lineNumber = b.lineNumber;
		threadId = b.threadId;
	}
}
