<?php
/**
 * OMR Web Server MySQL REST API
 * Handles real-time dual sync, student photo uploads with 20KB compression,
 * questions & answer keys synchronization, and bulk sync.
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-API-KEY, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/db_config.php';

// Helper function to save base64 photo to uploads folder
function saveBase64Image($base64Data, $rollNo) {
    if (empty($base64Data)) return '';
    // Strip data URI header if present
    if (preg_match('/^data:image\/(\w+);base64,/', $base64Data, $type)) {
        $base64Data = substr($base64Data, strpos($base64Data, ',') + 1);
    }
    $decoded = base64_decode($base64Data);
    if ($decoded === false || strlen($decoded) === 0) return '';

    $uploadDir = __DIR__ . '/uploads';
    if (!is_dir($uploadDir)) {
        @mkdir($uploadDir, 0755, true);
    }
    $safeRoll = preg_replace('/[^a-zA-Z0-9_-]/', '_', $rollNo);
    $filename = 'student_' . $safeRoll . '_' . time() . '.jpg';
    $filepath = $uploadDir . '/' . $filename;
    if (@file_put_contents($filepath, $decoded) !== false) {
        $isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') || (isset($_SERVER['SERVER_PORT']) && $_SERVER['SERVER_PORT'] == 443);
        $protocol = $isHttps ? 'https://' : 'http://';
        $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
        $scriptDir = dirname($_SERVER['SCRIPT_NAME']);
        $scriptDir = ($scriptDir === '/' || $scriptDir === '\\') ? '' : $scriptDir;
        return $protocol . $host . $scriptDir . '/uploads/' . $filename;
    }
    return '';
}

// Check API Key security if configured in db_config.php
if (defined('API_SECRET_KEY') && API_SECRET_KEY !== '') {
    $headers = getallheaders();
    $providedKey = '';
    if (isset($headers['X-API-KEY'])) {
        $providedKey = $headers['X-API-KEY'];
    } elseif (isset($headers['x-api-key'])) {
        $providedKey = $headers['x-api-key'];
    } elseif (isset($_REQUEST['api_key'])) {
        $providedKey = $_REQUEST['api_key'];
    }

    if ($providedKey !== API_SECRET_KEY) {
        http_response_code(401);
        echo json_encode(['status' => 'error', 'message' => 'Unauthorized: Invalid or missing API Key']);
        exit;
    }
}

$input = file_get_contents('php://input');
$data = json_decode($input, true);
if (!$data && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = $_POST;
}

$action = isset($_GET['action']) ? $_GET['action'] : ($data['action'] ?? '');
$d = (isset($data['data']) && is_array($data['data'])) ? $data['data'] : $data;

// Quick test ping
if ($action === 'ping' || $action === 'test_connection') {
    $pdo = getDbConnection();
    echo json_encode([
        'status' => 'success',
        'message' => 'MySQL Server Connected successfully!',
        'server_time' => date('Y-m-d H:i:s'),
        'php_version' => PHP_VERSION,
        'uploads_writable' => is_writable(__DIR__) || is_writable(__DIR__ . '/uploads')
    ]);
    exit;
}

$pdo = getDbConnection();

try {
    switch ($action) {
        // ----------------------------------------------------
        // 1. STUDENT PHOTO UPLOAD (Compressed ~20KB)
        // ----------------------------------------------------
        case 'upload_student_photo':
        case 'upload_photo':
            $rollNo = $d['roll_no'] ?? ($d['rollNo'] ?? 'photo');
            $base64 = $d['image_base64'] ?? ($d['imageBase64'] ?? ($d['image'] ?? ''));
            if (empty($base64)) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Missing image_base64 data']);
                exit;
            }

            $photoUrl = saveBase64Image($base64, $rollNo);
            if (!empty($photoUrl)) {
                // Also update student record if roll_no exists
                $stmt = $pdo->prepare("UPDATE students SET image_url = :image_url WHERE roll_no = :roll_no");
                $stmt->execute([':image_url' => $photoUrl, ':roll_no' => $rollNo]);

                echo json_encode([
                    'status' => 'success',
                    'message' => 'Photo uploaded and saved to server',
                    'image_url' => $photoUrl
                ]);
            } else {
                http_response_code(500);
                echo json_encode(['status' => 'error', 'message' => 'Failed to write photo to uploads/ folder']);
            }
            break;

        // ----------------------------------------------------
        // 2. SYNC STUDENT
        // ----------------------------------------------------
        case 'sync_student':
        case 'save_student':
            $rollNo = $d['roll_no'] ?? ($d['rollNo'] ?? '');
            $name = $d['name'] ?? '';
            if (empty($rollNo) || empty($name)) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Missing roll_no or name']);
                exit;
            }

            // Check if photo base64 is passed directly
            $finalImageUrl = $d['image_url'] ?? ($d['imagePath'] ?? ($d['imageUrl'] ?? ''));
            if (!empty($d['image_base64'])) {
                $uploadedUrl = saveBase64Image($d['image_base64'], $rollNo);
                if (!empty($uploadedUrl)) {
                    $finalImageUrl = $uploadedUrl;
                }
            }

            $stmt = $pdo->prepare("
                INSERT INTO students (roll_no, name, father_name, mother_name, gender, registration_no, dob, mobile_no, email, stream, subjects, image_url, timestamp)
                VALUES (:roll_no, :name, :father_name, :mother_name, :gender, :registration_no, :dob, :mobile_no, :email, :stream, :subjects, :image_url, :timestamp)
                ON DUPLICATE KEY UPDATE 
                    name = VALUES(name),
                    father_name = VALUES(father_name),
                    mother_name = VALUES(mother_name),
                    gender = VALUES(gender),
                    registration_no = VALUES(registration_no),
                    dob = VALUES(dob),
                    mobile_no = VALUES(mobile_no),
                    email = VALUES(email),
                    stream = VALUES(stream),
                    subjects = VALUES(subjects),
                    image_url = VALUES(image_url),
                    timestamp = VALUES(timestamp)
            ");

            $stmt->execute([
                ':roll_no' => $rollNo,
                ':name' => $name,
                ':father_name' => $d['father_name'] ?? ($d['fatherName'] ?? ''),
                ':mother_name' => $d['mother_name'] ?? ($d['motherName'] ?? ''),
                ':gender' => $d['gender'] ?? 'Male',
                ':registration_no' => $d['registration_no'] ?? ($d['registrationNo'] ?? ''),
                ':dob' => $d['dob'] ?? '',
                ':mobile_no' => $d['mobile_no'] ?? ($d['mobileNo'] ?? ''),
                ':email' => $d['email'] ?? '',
                ':stream' => $d['stream'] ?? 'ARTS',
                ':subjects' => $d['subjects'] ?? '',
                ':image_url' => $finalImageUrl,
                ':timestamp' => $d['timestamp'] ?? round(microtime(true) * 1000)
            ]);

            echo json_encode([
                'status' => 'success',
                'message' => 'Student synced successfully',
                'image_url' => $finalImageUrl
            ]);
            break;

        case 'delete_student':
            $rollNo = $d['roll_no'] ?? ($d['rollNo'] ?? ($_GET['rollNo'] ?? ''));
            if (!$rollNo) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'rollNo required']);
                exit;
            }
            $stmt = $pdo->prepare("DELETE FROM students WHERE roll_no = :roll_no");
            $stmt->execute([':roll_no' => $rollNo]);
            echo json_encode(['status' => 'success', 'message' => 'Student deleted']);
            break;

        // ----------------------------------------------------
        // 3. SYNC EXAM
        // ----------------------------------------------------
        case 'sync_exam':
        case 'save_exam':
            $id = $d['id'] ?? 0;
            $name = $d['name'] ?? '';
            if (empty($id) || empty($name)) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Missing exam id or name']);
                exit;
            }

            $stmt = $pdo->prepare("
                INSERT INTO exams (id, name, subject, date, title, logo_url, logo_opacity, logo_size, logo_position, marks_per_question, negative_marks, pass_marks, bonus_marks, template_type, timestamp)
                VALUES (:id, :name, :subject, :date, :title, :logo_url, :logo_opacity, :logo_size, :logo_position, :marks_per_question, :negative_marks, :pass_marks, :bonus_marks, :template_type, :timestamp)
                ON DUPLICATE KEY UPDATE 
                    name = VALUES(name),
                    subject = VALUES(subject),
                    date = VALUES(date),
                    title = VALUES(title),
                    logo_url = VALUES(logo_url),
                    logo_opacity = VALUES(logo_opacity),
                    logo_size = VALUES(logo_size),
                    logo_position = VALUES(logo_position),
                    marks_per_question = VALUES(marks_per_question),
                    negative_marks = VALUES(negative_marks),
                    pass_marks = VALUES(pass_marks),
                    bonus_marks = VALUES(bonus_marks),
                    template_type = VALUES(template_type),
                    timestamp = VALUES(timestamp)
            ");

            $stmt->execute([
                ':id' => $id,
                ':name' => $name,
                ':subject' => $d['subject'] ?? '',
                ':date' => $d['date'] ?? '',
                ':title' => $d['title'] ?? 'बिहार विद्यालय परीक्षा , समिति',
                ':logo_url' => $d['logo_url'] ?? ($d['logoUrl'] ?? ''),
                ':logo_opacity' => $d['logo_opacity'] ?? ($d['logoOpacity'] ?? 0.2),
                ':logo_size' => $d['logo_size'] ?? ($d['logoSize'] ?? 100),
                ':logo_position' => $d['logo_position'] ?? ($d['logoPosition'] ?? 'Left'),
                ':marks_per_question' => $d['marks_per_question'] ?? ($d['marksPerQuestion'] ?? 1.0),
                ':negative_marks' => $d['negative_marks'] ?? ($d['negativeMarks'] ?? 0.0),
                ':pass_marks' => $d['pass_marks'] ?? ($d['passMarks'] ?? 30.0),
                ':bonus_marks' => $d['bonus_marks'] ?? ($d['bonusMarks'] ?? 0.0),
                ':template_type' => $d['template_type'] ?? ($d['templateType'] ?? 'Standard'),
                ':timestamp' => $d['timestamp'] ?? round(microtime(true) * 1000)
            ]);

            echo json_encode(['status' => 'success', 'message' => 'Exam synced successfully']);
            break;

        case 'delete_exam':
            $id = $d['id'] ?? ($_GET['id'] ?? 0);
            if (!$id) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'id required']);
                exit;
            }
            $stmt = $pdo->prepare("DELETE FROM exams WHERE id = :id");
            $stmt->execute([':id' => $id]);
            $pdo->prepare("DELETE FROM answer_keys WHERE exam_id = :id")->execute([':id' => $id]);
            $pdo->prepare("DELETE FROM questions WHERE exam_id = :id")->execute([':id' => $id]);
            $pdo->prepare("DELETE FROM scan_results WHERE exam_id = :id")->execute([':id' => $id]);
            echo json_encode(['status' => 'success', 'message' => 'Exam and related records deleted']);
            break;

        // ----------------------------------------------------
        // 4. SYNC ANSWER KEY
        // ----------------------------------------------------
        case 'sync_answer_key':
        case 'save_answer_key':
            $examId = $d['exam_id'] ?? ($d['examId'] ?? 0);
            $setName = $d['set_name'] ?? ($d['setName'] ?? '');
            if (empty($examId) || empty($setName)) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Missing examId or setName']);
                exit;
            }

            $stmt = $pdo->prepare("
                INSERT INTO answer_keys (id, exam_id, set_name, num_questions, num_options, correct_answers, timestamp)
                VALUES (:id, :exam_id, :set_name, :num_questions, :num_options, :correct_answers, :timestamp)
                ON DUPLICATE KEY UPDATE 
                    num_questions = VALUES(num_questions),
                    num_options = VALUES(num_options),
                    correct_answers = VALUES(correct_answers),
                    timestamp = VALUES(timestamp)
            ");

            $ans = $d['correct_answers'] ?? ($d['correctAnswers'] ?? '[]');
            $stmt->execute([
                ':id' => $d['id'] ?? 0,
                ':exam_id' => $examId,
                ':set_name' => $setName,
                ':num_questions' => $d['num_questions'] ?? ($d['numQuestions'] ?? 50),
                ':num_options' => $d['num_options'] ?? ($d['numOptions'] ?? 4),
                ':correct_answers' => is_string($ans) ? $ans : json_encode($ans),
                ':timestamp' => $d['timestamp'] ?? round(microtime(true) * 1000)
            ]);

            echo json_encode(['status' => 'success', 'message' => 'Answer key synced successfully']);
            break;

        // ----------------------------------------------------
        // 5. SYNC QUESTION
        // ----------------------------------------------------
        case 'sync_question':
        case 'save_question':
            $qId = $d['id'] ?? 0;
            $examId = $d['exam_id'] ?? ($d['examId'] ?? 0);
            if (empty($qId) || empty($examId)) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Missing question id or examId']);
                exit;
            }

            $stmt = $pdo->prepare("
                INSERT INTO questions (id, exam_id, text, option_a, option_b, option_c, option_d, correct_index)
                VALUES (:id, :exam_id, :text, :option_a, :option_b, :option_c, :option_d, :correct_index)
                ON DUPLICATE KEY UPDATE 
                    text = VALUES(text),
                    option_a = VALUES(option_a),
                    option_b = VALUES(option_b),
                    option_c = VALUES(option_c),
                    option_d = VALUES(option_d),
                    correct_index = VALUES(correct_index)
            ");

            $stmt->execute([
                ':id' => $qId,
                ':exam_id' => $examId,
                ':text' => $d['text'] ?? '',
                ':option_a' => $d['option_a'] ?? ($d['optionA'] ?? ''),
                ':option_b' => $d['option_b'] ?? ($d['optionB'] ?? ''),
                ':option_c' => $d['option_c'] ?? ($d['optionC'] ?? ''),
                ':option_d' => $d['option_d'] ?? ($d['optionD'] ?? ''),
                ':correct_index' => $d['correct_index'] ?? ($d['correctIndex'] ?? 0)
            ]);

            echo json_encode(['status' => 'success', 'message' => 'Question synced successfully']);
            break;

        case 'delete_question':
            $qId = $d['id'] ?? ($_GET['id'] ?? 0);
            $stmt = $pdo->prepare("DELETE FROM questions WHERE id = :id");
            $stmt->execute([':id' => $qId]);
            echo json_encode(['status' => 'success', 'message' => 'Question deleted']);
            break;

        // ----------------------------------------------------
        // 6. SYNC SCAN RESULT
        // ----------------------------------------------------
        case 'sync_scan_result':
        case 'save_scan_result':
            $examId = $d['exam_id'] ?? ($d['examId'] ?? 0);
            $studentId = $d['student_id'] ?? ($d['studentId'] ?? '');
            if (empty($examId) || empty($studentId)) {
                http_response_code(400);
                echo json_encode(['status' => 'error', 'message' => 'Missing examId or studentId']);
                exit;
            }

            $stmt = $pdo->prepare("
                INSERT INTO scan_results (exam_id, student_id, paper_set, score, total_questions, student_answers, question_statuses, timestamp)
                VALUES (:exam_id, :student_id, :paper_set, :score, :total_questions, :student_answers, :question_statuses, :timestamp)
                ON DUPLICATE KEY UPDATE 
                    paper_set = VALUES(paper_set),
                    score = VALUES(score),
                    total_questions = VALUES(total_questions),
                    student_answers = VALUES(student_answers),
                    question_statuses = VALUES(question_statuses),
                    timestamp = VALUES(timestamp)
            ");

            $sa = $d['student_answers'] ?? ($d['studentAnswers'] ?? '[]');
            $qs = $d['question_statuses'] ?? ($d['questionStatuses'] ?? '[]');

            $stmt->execute([
                ':exam_id' => $examId,
                ':student_id' => $studentId,
                ':paper_set' => $d['paper_set'] ?? ($d['paperSet'] ?? 'Set A'),
                ':score' => $d['score'] ?? 0,
                ':total_questions' => $d['total_questions'] ?? ($d['totalQuestions'] ?? 50),
                ':student_answers' => is_string($sa) ? $sa : json_encode($sa),
                ':question_statuses' => is_string($qs) ? $qs : json_encode($qs),
                ':timestamp' => $d['timestamp'] ?? round(microtime(true) * 1000)
            ]);

            echo json_encode(['status' => 'success', 'message' => 'Scan result synced successfully']);
            break;

        // ----------------------------------------------------
        // 7. COMPLETE BULK SYNC (Exams, Students, Answer Keys, Questions, Results)
        // ----------------------------------------------------
        case 'bulk_sync':
            $pdo->beginTransaction();

            $examsCount = 0;
            $studentsCount = 0;
            $keysCount = 0;
            $questionsCount = 0;
            $resultsCount = 0;

            $bulkExams = $d['exams'] ?? ($data['exams'] ?? []);
            $bulkStudents = $d['students'] ?? ($data['students'] ?? []);
            $bulkKeys = $d['answer_keys'] ?? ($data['answer_keys'] ?? ($d['answerKeys'] ?? []));
            $bulkQuestions = $d['questions'] ?? ($data['questions'] ?? []);
            $bulkResults = $d['scan_results'] ?? ($data['scan_results'] ?? ($d['scanResults'] ?? []));

            // Sync Exams
            if (!empty($bulkExams) && is_array($bulkExams)) {
                $stmtExam = $pdo->prepare("
                    INSERT INTO exams (id, name, subject, date, title, logo_url, logo_opacity, logo_size, logo_position, marks_per_question, negative_marks, pass_marks, bonus_marks, template_type, timestamp)
                    VALUES (:id, :name, :subject, :date, :title, :logo_url, :logo_opacity, :logo_size, :logo_position, :marks_per_question, :negative_marks, :pass_marks, :bonus_marks, :template_type, :timestamp)
                    ON DUPLICATE KEY UPDATE 
                        name = VALUES(name), subject = VALUES(subject), date = VALUES(date), title = VALUES(title), logo_url = VALUES(logo_url),
                        logo_opacity = VALUES(logo_opacity), logo_size = VALUES(logo_size), logo_position = VALUES(logo_position),
                        marks_per_question = VALUES(marks_per_question), negative_marks = VALUES(negative_marks), pass_marks = VALUES(pass_marks),
                        bonus_marks = VALUES(bonus_marks), template_type = VALUES(template_type), timestamp = VALUES(timestamp)
                ");

                foreach ($bulkExams as $ex) {
                    $stmtExam->execute([
                        ':id' => $ex['id'],
                        ':name' => $ex['name'],
                        ':subject' => $ex['subject'] ?? '',
                        ':date' => $ex['date'] ?? '',
                        ':title' => $ex['title'] ?? 'बिहार विद्यालय परीक्षा , समिति',
                        ':logo_url' => $ex['logo_url'] ?? ($ex['logoUrl'] ?? ''),
                        ':logo_opacity' => $ex['logo_opacity'] ?? ($ex['logoOpacity'] ?? 0.2),
                        ':logo_size' => $ex['logo_size'] ?? ($ex['logoSize'] ?? 100),
                        ':logo_position' => $ex['logo_position'] ?? ($ex['logoPosition'] ?? 'Left'),
                        ':marks_per_question' => $ex['marks_per_question'] ?? ($ex['marksPerQuestion'] ?? 1.0),
                        ':negative_marks' => $ex['negative_marks'] ?? ($ex['negativeMarks'] ?? 0.0),
                        ':pass_marks' => $ex['pass_marks'] ?? ($ex['passMarks'] ?? 30.0),
                        ':bonus_marks' => $ex['bonus_marks'] ?? ($ex['bonusMarks'] ?? 0.0),
                        ':template_type' => $ex['template_type'] ?? ($ex['templateType'] ?? 'Standard'),
                        ':timestamp' => $ex['timestamp'] ?? 0
                    ]);
                    $examsCount++;
                }
            }

            // Sync Students
            if (!empty($bulkStudents) && is_array($bulkStudents)) {
                $stmtStudent = $pdo->prepare("
                    INSERT INTO students (roll_no, name, father_name, mother_name, gender, registration_no, dob, mobile_no, email, stream, subjects, image_url, timestamp)
                    VALUES (:roll_no, :name, :father_name, :mother_name, :gender, :registration_no, :dob, :mobile_no, :email, :stream, :subjects, :image_url, :timestamp)
                    ON DUPLICATE KEY UPDATE 
                        name = VALUES(name), father_name = VALUES(father_name), mother_name = VALUES(mother_name), gender = VALUES(gender),
                        registration_no = VALUES(registration_no), dob = VALUES(dob), mobile_no = VALUES(mobile_no), email = VALUES(email),
                        stream = VALUES(stream), subjects = VALUES(subjects), image_url = VALUES(image_url), timestamp = VALUES(timestamp)
                ");

                foreach ($bulkStudents as $st) {
                    $stmtStudent->execute([
                        ':roll_no' => $st['roll_no'] ?? ($st['rollNo'] ?? ''),
                        ':name' => $st['name'],
                        ':father_name' => $st['father_name'] ?? ($st['fatherName'] ?? ''),
                        ':mother_name' => $st['mother_name'] ?? ($st['motherName'] ?? ''),
                        ':gender' => $st['gender'] ?? 'Male',
                        ':registration_no' => $st['registration_no'] ?? ($st['registrationNo'] ?? ''),
                        ':dob' => $st['dob'] ?? '',
                        ':mobile_no' => $st['mobile_no'] ?? ($st['mobileNo'] ?? ''),
                        ':email' => $st['email'] ?? '',
                        ':stream' => $st['stream'] ?? 'ARTS',
                        ':subjects' => $st['subjects'] ?? '',
                        ':image_url' => $st['image_url'] ?? ($st['imagePath'] ?? ($st['imageUrl'] ?? '')),
                        ':timestamp' => $st['timestamp'] ?? 0
                    ]);
                    $studentsCount++;
                }
            }

            // Sync Answer Keys
            if (!empty($bulkKeys) && is_array($bulkKeys)) {
                $stmtKey = $pdo->prepare("
                    INSERT INTO answer_keys (id, exam_id, set_name, num_questions, num_options, correct_answers, timestamp)
                    VALUES (:id, :exam_id, :set_name, :num_questions, :num_options, :correct_answers, :timestamp)
                    ON DUPLICATE KEY UPDATE 
                        num_questions = VALUES(num_questions),
                        num_options = VALUES(num_options),
                        correct_answers = VALUES(correct_answers),
                        timestamp = VALUES(timestamp)
                ");

                foreach ($bulkKeys as $k) {
                    $ans = $k['correct_answers'] ?? ($k['correctAnswers'] ?? '[]');
                    $stmtKey->execute([
                        ':id' => $k['id'] ?? 0,
                        ':exam_id' => $k['exam_id'] ?? ($k['examId'] ?? 0),
                        ':set_name' => $k['set_name'] ?? ($k['setName'] ?? ''),
                        ':num_questions' => $k['num_questions'] ?? ($k['numQuestions'] ?? 50),
                        ':num_options' => $k['num_options'] ?? ($k['numOptions'] ?? 4),
                        ':correct_answers' => is_string($ans) ? $ans : json_encode($ans),
                        ':timestamp' => $k['timestamp'] ?? 0
                    ]);
                    $keysCount++;
                }
            }

            // Sync Questions
            if (!empty($bulkQuestions) && is_array($bulkQuestions)) {
                $stmtQ = $pdo->prepare("
                    INSERT INTO questions (id, exam_id, text, option_a, option_b, option_c, option_d, correct_index)
                    VALUES (:id, :exam_id, :text, :option_a, :option_b, :option_c, :option_d, :correct_index)
                    ON DUPLICATE KEY UPDATE 
                        text = VALUES(text),
                        option_a = VALUES(option_a),
                        option_b = VALUES(option_b),
                        option_c = VALUES(option_c),
                        option_d = VALUES(option_d),
                        correct_index = VALUES(correct_index)
                ");

                foreach ($bulkQuestions as $q) {
                    $stmtQ->execute([
                        ':id' => $q['id'],
                        ':exam_id' => $q['exam_id'] ?? ($q['examId'] ?? 0),
                        ':text' => $q['text'] ?? '',
                        ':option_a' => $q['option_a'] ?? ($q['optionA'] ?? ''),
                        ':option_b' => $q['option_b'] ?? ($q['optionB'] ?? ''),
                        ':option_c' => $q['option_c'] ?? ($q['optionC'] ?? ''),
                        ':option_d' => $q['option_d'] ?? ($q['optionD'] ?? ''),
                        ':correct_index' => $q['correct_index'] ?? ($q['correctIndex'] ?? 0)
                    ]);
                    $questionsCount++;
                }
            }

            // Sync Scan Results
            if (!empty($bulkResults) && is_array($bulkResults)) {
                $stmtRes = $pdo->prepare("
                    INSERT INTO scan_results (exam_id, student_id, paper_set, score, total_questions, student_answers, question_statuses, timestamp)
                    VALUES (:exam_id, :student_id, :paper_set, :score, :total_questions, :student_answers, :question_statuses, :timestamp)
                    ON DUPLICATE KEY UPDATE 
                        paper_set = VALUES(paper_set), score = VALUES(score), total_questions = VALUES(total_questions),
                        student_answers = VALUES(student_answers), question_statuses = VALUES(question_statuses), timestamp = VALUES(timestamp)
                ");

                foreach ($bulkResults as $sr) {
                    $sa = $sr['student_answers'] ?? ($sr['studentAnswers'] ?? '[]');
                    $qs = $sr['question_statuses'] ?? ($sr['questionStatuses'] ?? '[]');

                    $stmtRes->execute([
                        ':exam_id' => $sr['exam_id'] ?? ($sr['examId'] ?? 0),
                        ':student_id' => $sr['student_id'] ?? ($sr['studentId'] ?? ''),
                        ':paper_set' => $sr['paper_set'] ?? ($sr['paperSet'] ?? 'Set A'),
                        ':score' => $sr['score'] ?? 0,
                        ':total_questions' => $sr['total_questions'] ?? ($sr['totalQuestions'] ?? 50),
                        ':student_answers' => is_string($sa) ? $sa : json_encode($sa),
                        ':question_statuses' => is_string($qs) ? $qs : json_encode($qs),
                        ':timestamp' => $sr['timestamp'] ?? 0
                    ]);
                    $resultsCount++;
                }
            }

            $pdo->commit();
            echo json_encode([
                'status' => 'success',
                'message' => "Bulk sync complete: $examsCount exams, $keysCount answer keys, $questionsCount questions, $studentsCount students, $resultsCount results synced."
            ]);
            break;

        default:
            http_response_code(400);
            echo json_encode(['status' => 'error', 'message' => 'Invalid action: ' . htmlspecialchars($action)]);
            break;
    }
} catch (\Exception $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode(['status' => 'error', 'message' => 'Server Error: ' . $e->getMessage()]);
}
