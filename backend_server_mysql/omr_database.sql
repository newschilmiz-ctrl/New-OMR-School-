-- ====================================================================
-- OMR EVALUATION & GRADING SYSTEM - COMPLETE MYSQL DATABASE SCHEMA
-- Compatible with MySQL 5.7+, MySQL 8.0+, MariaDB 10.3+
-- Full UTF-8 (utf8mb4) support for Hindi / Regional Languages & Symbols
-- ====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Using user's database: gceedakt_rsarts
CREATE DATABASE IF NOT EXISTS `gceedakt_rsarts` 
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `gceedakt_rsarts`;

-- --------------------------------------------------------
-- Table 1: `exams`
-- Stores all examination metadata, negative marking, and layout configs
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exams` (
    `id` INT NOT NULL,
    `name` VARCHAR(255) NOT NULL COMMENT 'Exam Title / Name',
    `subject` VARCHAR(255) NOT NULL COMMENT 'Subject Name',
    `date` VARCHAR(100) DEFAULT '' COMMENT 'Exam Date string',
    `title` VARCHAR(255) DEFAULT 'बिहार विद्यालय परीक्षा , समिति' COMMENT 'Header Institution Title',
    `logo_url` TEXT COMMENT 'School/Institute Logo URL or Cloudinary URL',
    `logo_opacity` FLOAT DEFAULT 0.2 COMMENT 'Watermark logo opacity',
    `logo_size` FLOAT DEFAULT 100 COMMENT 'Logo diameter size in dp',
    `logo_position` VARCHAR(50) DEFAULT 'Left' COMMENT 'Left, Center, or Right',
    `marks_per_question` FLOAT DEFAULT 1.0 COMMENT 'Marks awarded per correct bubble',
    `negative_marks` FLOAT DEFAULT 0.0 COMMENT 'Marks deducted per wrong bubble',
    `pass_marks` FLOAT DEFAULT 30.0 COMMENT 'Minimum passing marks',
    `bonus_marks` FLOAT DEFAULT 0.0 COMMENT 'Bonus marks added to all students',
    `template_type` VARCHAR(100) DEFAULT 'Standard' COMMENT 'OMR Sheet template layout',
    `timestamp` BIGINT DEFAULT 0 COMMENT 'Creation timestamp (epoch ms)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_exam_subject` (`subject`),
    INDEX `idx_exam_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table 2: `students`
-- Stores student admissions, roll numbers, photos, and subject choices
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `students` (
    `id` INT AUTO_INCREMENT,
    `roll_no` VARCHAR(100) NOT NULL COMMENT 'Student Roll No (e.g. 260001)',
    `name` VARCHAR(255) NOT NULL COMMENT 'Student Full Name',
    `father_name` VARCHAR(255) DEFAULT '',
    `mother_name` VARCHAR(255) DEFAULT '',
    `gender` VARCHAR(20) DEFAULT 'Male',
    `registration_no` VARCHAR(100) DEFAULT '',
    `dob` VARCHAR(50) DEFAULT '',
    `mobile_no` VARCHAR(50) DEFAULT '',
    `email` VARCHAR(255) DEFAULT '',
    `stream` VARCHAR(100) DEFAULT 'ARTS',
    `subjects` VARCHAR(255) DEFAULT '',
    `image_url` TEXT COMMENT 'Cloudinary / Web photo URL',
    `timestamp` BIGINT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_roll_no` (`roll_no`),
    INDEX `idx_stream` (`stream`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table 3: `answer_keys`
-- Stores answer keys for sets (Set A, Set B, etc.) per exam
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `answer_keys` (
    `id` INT NOT NULL DEFAULT 0,
    `exam_id` INT NOT NULL COMMENT 'Foreign key to exams.id',
    `set_name` VARCHAR(50) NOT NULL COMMENT 'Set A, Set B, Set C, Set D',
    `num_questions` INT NOT NULL DEFAULT 50,
    `num_options` INT NOT NULL DEFAULT 4,
    `correct_answers` LONGTEXT NOT NULL COMMENT 'JSON Array of 0-based option indexes [0, 2, 1...]',
    `timestamp` BIGINT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`exam_id`, `set_name`),
    INDEX `idx_ans_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table 4: `questions`
-- Stores bilingual question bank and multiple choice options
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `questions` (
    `id` INT NOT NULL,
    `exam_id` INT NOT NULL,
    `text` TEXT NOT NULL COMMENT 'Question Statement',
    `option_a` TEXT,
    `option_b` TEXT,
    `option_c` TEXT,
    `option_d` TEXT,
    `correct_index` INT DEFAULT 0 COMMENT '0 for A, 1 for B, 2 for C, 3 for D',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_q_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table 5: `scan_results`
-- Stores evaluated OMR sheet scans, student marks, and bubble answers
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `scan_results` (
    `id` INT AUTO_INCREMENT,
    `exam_id` INT NOT NULL COMMENT 'Exam ID',
    `student_id` VARCHAR(100) NOT NULL COMMENT 'Student Roll No / Barcode',
    `paper_set` VARCHAR(50) DEFAULT 'Set A',
    `score` FLOAT NOT NULL COMMENT 'Calculated Score',
    `total_questions` INT NOT NULL DEFAULT 50,
    `student_answers` LONGTEXT COMMENT 'JSON Array of student marked options [1, 2, -1, 0...]',
    `question_statuses` LONGTEXT COMMENT 'JSON Array of statuses: 1=correct, 0=wrong, -1=unattempted',
    `timestamp` BIGINT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_student` (`exam_id`, `student_id`),
    INDEX `idx_res_exam_id` (`exam_id`),
    INDEX `idx_res_student_id` (`student_id`),
    INDEX `idx_res_score` (`score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
