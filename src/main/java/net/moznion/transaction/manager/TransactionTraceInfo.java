package net.moznion.transaction.manager;

import java.util.Optional;

import lombok.Getter;

@Getter
public class TransactionTraceInfo {
	private final String className;
	private final String fileName;
	private final String methodName;
	private final int lineNumber;
	private final long threadId;

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
