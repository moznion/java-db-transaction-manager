transaction-manager [![Build Status][travis-image]][travis-url] [![Maven Central][maven-image]][maven-url] [![javadoc.io][javadocio-image]][javadocio-url]
=============

Simply DB transaction manager for Java.

Synopsis
---

### Basic transaction with commit

```java
TransactionManager txnManager = new TransactionManager(connection);
txnManager.txnBegin();
try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
    preparedStatement.executeUpdate();
}
txnManager.txnCommit(); // insert successfully
```

### Basic transaction with rollback

```java
TransactionManager txnManager = new TransactionManager(connection);
txnManager.txnBegin();
try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
    preparedStatement.executeUpdate();
}
txnManager.txnRollback(); // rollback
```

### Scope based transaction with commit

```java
TransactionManager txnManager = new TransactionManager(connection);
try (TransactionScope txn = new TransactionScope(txnManager)) {
    try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
        preparedStatement.executeUpdate();
    }
    txn.commit(); // insert successfully
}
```

### Scope based transaction with rollback

```java
TransactionManager txnManager = new TransactionManager(connection);
try (TransactionScope txn = new TransactionScope(txnManager)) {
    try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
        preparedStatement.executeUpdate();
    }
    txn.rollback(); // rollback
}
```

### Scope based transaction with implicit rollback

```java
TransactionManager txnManager = new TransactionManager(connection);
try (TransactionScope txn = new TransactionScope(txnManager)) {
    try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
        preparedStatement.executeUpdate();
    }
} // if reach here without any action (commit or rollback), transaction will rollback automatically
```

Description
--

transaction-manager is a simply DB transaction manager.

This package provides `begin`, `commit` and `rollback` function for transaction.
And also provides scope based transaction manager (scope based means it is with `try-with-resources` statement).

This package is inspired by [DBIx::TransactionManager](https://metacpan.org/pod/DBIx::TransactionManager) from Perl.

Behavior Nested Transaction
--

If any of nested transaction is rollbacked, all of transaction will rollback.

Dependencies
--

- Java 8 or later

See Also
--

- [DBIx::TransactionManager](https://metacpan.org/pod/DBIx::TransactionManager)

Author
--

moznion (<moznion@gmail.com>)

License
--

```
The MIT License (MIT)
Copyright © 2014 moznion, http://moznion.net/ <moznion@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the “Software”), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

[travis-url]: https://travis-ci.org/moznion/java-transaction-manager
[travis-image]: https://travis-ci.org/moznion/java-transaction-manager.svg?branch=master
[maven-url]: https://maven-badges.herokuapp.com/maven-central/net.moznion/transaction-manager
[maven-image]: https://maven-badges.herokuapp.com/maven-central/net.moznion/transaction-manager/badge.svg?style=flat
[javadocio-url]: https://javadocio-badges.herokuapp.com/net.moznion/transaction-manager
[javadocio-image]: https://javadocio-badges.herokuapp.com/net.moznion/transaction-manager/badge.svg

