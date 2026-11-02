/**
 * Reusable SQLite Database Connection Manager for Banking Management System.
 * Uses native node:sqlite (Node.js built-in, zero external dependencies).
 */

const { DatabaseSync } = require('node:sqlite');
const path = require('node:path');
const fs = require('node:fs');

const DB_FILE = path.resolve(__dirname, 'bank_system.db');

let dbInstance = null;

/**
 * Returns active database connection instance.
 */
function getConnection() {
    if (!dbInstance) {
        dbInstance = new DatabaseSync(DB_FILE);
        // Enable foreign key constraints
        dbInstance.exec('PRAGMA foreign_keys = ON;');
    }
    return dbInstance;
}

/**
 * Executes a SELECT query and returns all matching rows.
 */
function query(sql, params = []) {
    const db = getConnection();
    const stmt = db.prepare(sql);
    return stmt.all(...params);
}

/**
 * Executes an INSERT, UPDATE, or DELETE query and returns changes.
 */
function execute(sql, params = []) {
    const db = getConnection();
    const stmt = db.prepare(sql);
    return stmt.run(...params);
}

/**
 * Closes the active database connection.
 */
function closeConnection() {
    if (dbInstance) {
        dbInstance.close();
        dbInstance = null;
    }
}

module.exports = {
    getConnection,
    query,
    execute,
    closeConnection,
    DB_FILE,
};

