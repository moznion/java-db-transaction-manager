package net.moznion.db.transaction.manager;

import lombok.Getter;

import java.util.Optional;

/**
 * Represents the stack traced information for transaction.
 * 
 * @author moznion
 *
 */
@Getter
public class TransactionTraceInfo {
	private final String className;
	private final String fileName;
	private final String methodName;
	private final int lineNumber;
	private final long threadId;

	/**
	 * Construct traced information for transaction with optional {@code StackTraceElement} and thread ID.
	 * 
	 * <p>
	 * If optional {@code StackTraceElement} is null, when each fields will be "Unknown".
	 * </p>
	 * 
	 * @param maybeStackTraceElement
	 * @param threadId
	 */
	public TransactionTraceInfo(Optional<StackTraceElement> maybeStackTraceElement, long threadId) {
		this.threadId = threadId;

		if (maybeStackTraceElement.isPresent()) {
			StackTraceElement stackTraceElement = maybeStackTraceElement.get();
			className = stackTraceElement.getClassName();
			fileName = stackTraceElement.getFileName();
			methodName = stackTraceElement.getMethodName();
			lineNumber = stackTraceElement.getLineNumber();
		} else {
			String unknownSimbol = "Unknown";
			className = unknownSimbol;
			fileName = unknownSimbol;
			methodName = unknownSimbol;
			lineNumber = -1;
		}
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("File Name: ")
			.append(fileName)
			.append(", Class Name: ")
			.append(className)
			.append(", Method Name: ")
			.append(methodName)
			.append(", Line Number: ")
			.append(lineNumber)
			.append(", Thread ID: ")
			.append(threadId)
			.toString();
	}
}
