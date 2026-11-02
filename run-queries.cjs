#!/usr/bin/env node
/**
 * Direct Query Runner for Banking Management System.
 * Connects to bank_system.db, parses queries.sql, and prints formatted output.
 * Usage:
 *   node run-queries.cjs        (runs all queries)
 *   node run-queries.cjs 7      (runs query #7)
 */

const fs = require('node:fs');
const path = require('node:path');
const { getConnection, DB_FILE } = require('./db-connection.cjs');

function parseQueries(sqlFilePath) {
    const content = fs.readFileSync(sqlFilePath, 'utf8');
    const lines = content.split(/\r?\n/);
    const queries = [];

    let currentTitle = '';
    let currentSql = [];
    let queryIndex = 1;

    for (const rawLine of lines) {
        const line = rawLine.trim();

        if (line.startsWith('--')) {
            const comment = line.substring(2).trim();
            if (comment && !comment.startsWith('===') && !comment.startsWith('---')) {
                const cleaned = comment.replace(/^\d+\.\s*/, '');
                currentTitle = currentTitle ? `${currentTitle} - ${cleaned}` : cleaned;
            }
            continue;
        }

        if (!line) continue;

        currentSql.push(rawLine);

        if (line.endsWith(';')) {
            const sqlText = currentSql.join('\n').trim().replace(/;$/, '').trim();
            if (sqlText) {
                queries.push({
                    index: queryIndex++,
                    title: currentTitle || `Query #${queryIndex}`,
                    sql: sqlText
                });
                currentTitle = '';
            }
            currentSql = [];
        }
    }

    return queries;
}

function run() {
    const targetQuery = process.argv[2] ? parseInt(process.argv[2], 10) : null;
    const queriesFile = path.resolve(__dirname, 'queries.sql');

    if (!fs.existsSync(queriesFile)) {
        console.error(`Error: queries.sql file not found at: ${queriesFile}`);
        process.exit(1);
    }

    const queries = parseQueries(queriesFile);
    const db = getConnection();

    console.log('='.repeat(80));
    console.log('      INDIA BANKING MANAGEMENT SYSTEM - DATABASE QUERY CONNECTION');
    console.log('='.repeat(80));
    console.log(` Database File       : ${DB_FILE}`);
    console.log(` Target Script File  : queries.sql (${queries.length} total queries)`);
    if (targetQuery) {
        console.log(` Filter Target Query : #${targetQuery}`);
    }
    console.log('='.repeat(80) + '\n');

    let executed = 0;

    for (const q of queries) {
        if (targetQuery && q.index !== targetQuery) continue;

        executed++;
        console.log('='.repeat(80));
        console.log(` QUERY [${q.index}]: ${q.title}`);
        console.log('='.repeat(80));
        console.log(q.sql);
        console.log('-'.repeat(80));

        const start = performance.now();
        try {
            const stmt = db.prepare(q.sql);
            const rows = stmt.all();
            const elapsed = (performance.now() - start).toFixed(2);

            if (rows.length === 0) {
                console.log('  (No rows returned)');
            } else {
                console.table(rows);
            }
            console.log(`  ==> ${rows.length} row(s) returned in ${elapsed} ms.\n`);
        } catch (err) {
            console.error(`  [ERROR]: ${err.message}\n`);
        }
    }

    if (executed === 0 && targetQuery) {
        console.log(`No query with index #${targetQuery} found. Available: 1 to ${queries.length}`);
    }
}

run();

