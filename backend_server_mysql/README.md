# OMR Evaluation System - MySQL Database & Web Server Setup

This directory contains the complete backend solution for syncing data to your own web server / MySQL database alongside Firebase.

---

## 📁 Files Included

1. **`omr_database.sql`** : Ready-to-import SQL schema with all 5 tables (`exams`, `students`, `answer_keys`, `questions`, `scan_results`). Full UTF-8 (`utf8mb4`) support for Hindi/English bilingual text.
2. **`db_config.php`** : Database credentials configuration file.
3. **`api.php`** : REST API endpoint handling real-time dual sync and bulk sync from the Android app.

---

## 🚀 How to Setup on Your Web Hosting / cPanel / VPS

### Step 1: Create Database & Import SQL Schema
1. Open your hosting control panel (cPanel, DirectAdmin, Plesk, or phpMyAdmin).
2. Go to **MySQL Databases** and create a new database (e.g. `omr_system`).
3. Create a MySQL user with a strong password and assign it full privileges to the database.
4. Open **phpMyAdmin**, select the `omr_system` database.
5. Go to the **Import** tab, choose the file **`omr_database.sql`**, and click **Go** (or paste the SQL queries into the **SQL** tab and run).

---

### Step 2: Configure `db_config.php`
Open `db_config.php` and set your credentials:

```php
define('DB_HOST', 'localhost');
define('DB_NAME', 'your_cpanel_omr_system');
define('DB_USER', 'your_cpanel_db_user');
define('DB_PASS', 'your_strong_password');

// Optional API Key for security (match with the app's setting):
define('API_SECRET_KEY', 'my_secret_key_123');
```

---

### Step 3: Upload to Web Server
Upload the `omr_api` folder (containing `api.php` and `db_config.php`) to your website's `public_html` directory:

Example path on server:  
`https://yourdomain.com/omr_api/api.php`

---

### Step 4: Configure in Android App
1. Open the OMR Grader Android App.
2. Tap on **"☁️ Cloud & MySQL"** in the top header (or tap **"MySQL Sync"** in the category bar).
3. Toggle **"Web Server MySQL Sync"** ON.
4. Enter your Web Server API URL:
   `https://yourdomain.com/omr_api/api.php`
5. Enter your API Secret Key (if configured in `db_config.php`).
6. Tap **"Save URL"** then **"Test Connection"** to verify.
7. Tap **"Sync All Data to MySQL Now"** to push all current exams, students, and scan results!

---

## 🗄️ Database Tables Overview

| Table Name | Description | Primary Key |
| :--- | :--- | :--- |
| **`exams`** | Exam name, subject, date, logo watermark, marks, negative marking, pass marks | `id` (INT) |
| **`students`** | Roll No, student name, father/mother name, DOB, stream, subjects, image URL | `roll_no` (VARCHAR) |
| **`answer_keys`** | Exam ID, Set Name (Set A, Set B...), question count, correct answers array | `(exam_id, set_name)` |
| **`questions`** | Bilingual Hindi/English questions, 4 options (A/B/C/D), correct option index | `id` (INT) |
| **`scan_results`** | Scanned OMR result, student ID, score, total marks, bubble answers array | `(exam_id, student_id)` |
