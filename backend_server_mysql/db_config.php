<?php
/**
 * Database Connection Configuration
 * Edit the credentials below with your MySQL / cPanel database details.
 */

define('DB_HOST', 'localhost');
define('DB_NAME', 'omr_system');
define('DB_USER', 'root');
define('DB_PASS', '');

// Optional secret API key to protect your endpoint (Match with the app's setting)
define('API_SECRET_KEY', ''); // Leave empty if you don't require an API Key

function getDbConnection() {
    $charset = 'utf8mb4';
    $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . $charset;
    $options = [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ];
    try {
        return new PDO($dsn, DB_USER, DB_PASS, $options);
    } catch (\PDOException $e) {
        http_response_code(500);
        echo json_encode(['status' => 'error', 'message' => 'Database connection failed: ' . $e->getMessage()]);
        exit;
    }
}
