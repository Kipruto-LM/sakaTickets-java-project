// This file belongs to the "database" package: the classes that talk to PostgreSQL.
package database;

/**
 * The error our code throws when something goes wrong with the database
 * (it can't connect, a query fails, and so on).
 *
 * OOP idea: inheritance to make our own exception type. DatabaseException extends
 * RuntimeException, so it IS a RuntimeException and can be thrown and caught like one.
 * Having our own type lets the screens catch "database problems" separately from other errors.
 */
public class DatabaseException extends RuntimeException {
    // Constructor: the message explains what we were trying to do, and "cause" is the
    // original error from Java's database classes (kept so the full details aren't lost).
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);      // "super(...)" runs the parent class's constructor
    }
}
