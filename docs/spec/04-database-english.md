# 04 — 英语学科数据库设计

> **数据库**: MySQL 8.0 · 字符集 utf8mb4 · 引擎 InnoDB  
> **后端**: Spring Boot JPA + MyBatis-Plus  
> **日期**: 2026-06-30

---

## 一、英语学科 ER 图

```
┌─────────────────┐       ┌──────────────────────┐
│ english_classroom│       │ english_word_book     │
│─────────────────│       │──────────────────────│
│ id              │       │ id                    │
│ teacher_id      │──→User│ name (中考核心词汇)     │
│ name            │       │ grade_level           │
│ student_ids JSON │       │ word_count            │
│ schedule (排课)  │       │ cover_image           │
│ status          │       │ created_at            │
│ created_at      │       └──────────┬───────────┘
└────────┬────────┘                  │
         │                           │ 1:N
         │ 1:N                       ▼
         ▼                  ┌──────────────────────┐
┌──────────────────────┐    │ english_word          │
│ english_class_record  │    │──────────────────────│
│──────────────────────│    │ id                    │
│ id                   │    │ book_id       ──────→│ WordBook
│ classroom_id ───────→│    │ word (abandon)        │
│ student_id           │    │ phonetic (音标)        │
│ score / notes        │    │ meaning (释义)         │
│ attendance           │    │ example (例句)         │
│ created_at           │    │ part_of_speech (词性)  │
└──────────────────────┘    │ difficulty (1-5)       │
                            │ audio_url             │
                            └──────────┬───────────┘
                                       │
                                       │ N:M (学生进度)
                                       ▼
┌──────────────────────────────────────┐
│ english_student_word_progress        │
│──────────────────────────────────────│
│ id                                   │
│ student_id                           │
│ word_id ────────────────────────────→│ Word
│ status (new/learning/review/mastered)│
│ correct_count / wrong_count          │
│ last_review_at                       │
│ next_review_at (艾宾浩斯间隔)          │
└──────────────────────────────────────┘

┌─────────────────────────┐    ┌──────────────────────────┐
│ english_vocab_test       │    │ english_vocab_test_record │
│─────────────────────────│    │──────────────────────────│
│ id                       │1:N │ id                        │
│ teacher_id               │───→│ test_id                    │
│ book_id          ───────→│    │ student_id                │
│ name                     │    │ total / correct / wrong    │
│ word_count / time_limit  │    │ score                     │
│ created_at               │    │ wrong_word_ids JSON       │
└─────────────────────────┘    │ created_at                 │
                               └──────────────────────────┘

┌─────────────────────────┐
│ english_reading_article │        ┌──────────────────────┐
│─────────────────────────│        │ english_reading_question│
│ id                       │1:N     │──────────────────────│
│ title                    │───────→│ id                    │
│ content (正文)            │        │ article_id            │
│ level (难度)              │        │ question_text         │
│ source (来源)             │        │ options JSON          │
│ tags                     │        │ correct_answer        │
│ word_count               │        │ explanation (解析)     │
└─────────────────────────┘        └──────────────────────┘

┌─────────────────────────┐
│ english_dialogue_scenario│       ┌───────────────────────┐
│─────────────────────────│       │ english_dialogue_record│
│ id                       │1:N    │───────────────────────│
│ title (餐厅点餐)          │──────→│ id                     │
│ description              │       │ scenario_id            │
│ level                    │       │ student_id             │
│ roles JSON (角色)         │       │ messages JSON (对话记录) │
│ prompts JSON (提示)       │       │ ai_feedback (AI评测)    │
└─────────────────────────┘       │ score                  │
                                  └───────────────────────┘

┌─────────────────────────┐
│ english_grammar_point     │       ┌────────────────────────┐
│─────────────────────────│       │ english_sentence_practice│
│ id                       │       │────────────────────────│
│ name (现在完成时)          │       │ id                      │
│ category (时态/语态/从句)  │       │ grammar_point_id        │
│ explanation (语法解释)     │       │ prompt (提示词)          │
│ formula (结构公式)         │       │ correct_sentence        │
│ examples JSON             │       │ difficulty              │
│ level                    │       └────────────────────────┘
└─────────────────────────┘

┌─────────────────────────┐
│ english_feedback          │
│─────────────────────────│
│ id                       │
│ student_id               │
│ teacher_id               │
│ classroom_id             │
│ type (classroom/vocab/reading) │
│ content (反馈内容)         │
│ ai_analysis (AI分析JSON)  │
│ strengths / weaknesses   │
│ suggestions JSON         │
│ created_at               │
└─────────────────────────┘
```

---

## 二、建表 SQL

```sql
-- 1. 词库
CREATE TABLE english_word_book (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '词库名称',
  description VARCHAR(500),
  grade_level VARCHAR(20) COMMENT '学段',
  word_count INT DEFAULT 0,
  cover_image VARCHAR(255),
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英语词库';

-- 2. 单词
CREATE TABLE english_word (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  book_id BIGINT NOT NULL,
  word VARCHAR(100) NOT NULL,
  phonetic VARCHAR(100) COMMENT '音标',
  meaning VARCHAR(500) NOT NULL COMMENT '释义',
  example VARCHAR(500) COMMENT '例句',
  example_cn VARCHAR(500) COMMENT '例句翻译',
  part_of_speech VARCHAR(50) COMMENT '词性',
  difficulty TINYINT DEFAULT 1 COMMENT '难度1-5',
  audio_url VARCHAR(255),
  status TINYINT DEFAULT 1,
  INDEX idx_book (book_id),
  FOREIGN KEY (book_id) REFERENCES english_word_book(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英语单词';

-- 3. 学生单词进度
CREATE TABLE english_student_word_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  word_id BIGINT NOT NULL,
  status ENUM('new','learning','review','mastered') DEFAULT 'new',
  correct_count INT DEFAULT 0,
  wrong_count INT DEFAULT 0,
  last_review_at DATETIME,
  next_review_at DATETIME COMMENT '艾宾浩斯复习日期',
  UNIQUE KEY uk_student_word (student_id, word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生单词进度';

-- 4. 课堂
CREATE TABLE english_classroom (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL COMMENT '课堂名称',
  description TEXT,
  student_ids JSON COMMENT '学生ID列表',
  schedule VARCHAR(200) COMMENT '排课时间',
  book_id BIGINT COMMENT '关联词库',
  status ENUM('active','completed','cancelled') DEFAULT 'active',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英语课堂';

-- 5. 课堂记录
CREATE TABLE english_class_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  score DECIMAL(5,1),
  notes TEXT COMMENT '课堂笔记/表现',
  attendance BOOLEAN DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (classroom_id) REFERENCES english_classroom(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课堂记录';

-- 6. 词汇测试
CREATE TABLE english_vocab_test (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  book_id BIGINT,
  name VARCHAR(200) COMMENT '测试名称',
  word_count INT DEFAULT 20,
  time_limit INT DEFAULT 600 COMMENT '限时(秒)',
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='词汇测试';

-- 7. 测试记录
CREATE TABLE english_vocab_test_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  test_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  total INT DEFAULT 0,
  correct INT DEFAULT 0,
  wrong INT DEFAULT 0,
  score DECIMAL(5,1),
  wrong_word_ids JSON,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (test_id) REFERENCES english_vocab_test(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='词汇测试记录';

-- 8. 阅读文章
CREATE TABLE english_reading_article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(300) NOT NULL,
  content TEXT NOT NULL COMMENT '正文',
  level ENUM('easy','medium','hard') DEFAULT 'medium',
  source VARCHAR(200),
  tags JSON,
  word_count INT,
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英语阅读文章';

-- 9. 阅读理解题
CREATE TABLE english_reading_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_id BIGINT NOT NULL,
  question_text TEXT NOT NULL,
  options JSON COMMENT '选项数组',
  correct_answer VARCHAR(10) COMMENT 'A/B/C/D',
  explanation TEXT COMMENT '解析',
  FOREIGN KEY (article_id) REFERENCES english_reading_article(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读理解题';

-- 10. 口语场景
CREATE TABLE english_dialogue_scenario (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  level ENUM('beginner','intermediate','advanced') DEFAULT 'beginner',
  roles JSON COMMENT '角色定义',
  prompts JSON COMMENT '对话提示',
  cover_image VARCHAR(255),
  status TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='口语场景';

-- 11. 口语对话记录
CREATE TABLE english_dialogue_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scenario_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  messages JSON COMMENT '对话历史',
  ai_feedback TEXT COMMENT 'AI评测',
  score DECIMAL(5,1),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (scenario_id) REFERENCES english_dialogue_scenario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='口语对话记录';

-- 12. 语法知识点
CREATE TABLE english_grammar_point (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  category VARCHAR(50) COMMENT '时态/语态/从句/词法',
  explanation TEXT COMMENT '语法解释',
  formula VARCHAR(500) COMMENT '结构公式',
  examples JSON COMMENT '例句数组',
  level ENUM('basic','intermediate','advanced') DEFAULT 'basic',
  sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语法知识点';

-- 13. 造句练习
CREATE TABLE english_sentence_practice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  grammar_point_id BIGINT NOT NULL,
  prompt VARCHAR(500) NOT NULL COMMENT '提示词',
  correct_sentence VARCHAR(500) NOT NULL COMMENT '标准答案',
  difficulty TINYINT DEFAULT 1,
  FOREIGN KEY (grammar_point_id) REFERENCES english_grammar_point(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='造句练习';

-- 14. 学习反馈
CREATE TABLE english_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  classroom_id BIGINT,
  type ENUM('classroom','vocab','reading','comprehensive') NOT NULL,
  content TEXT,
  ai_analysis JSON COMMENT 'AI分析',
  strengths JSON COMMENT '优势',
  weaknesses JSON COMMENT '薄弱点',
  suggestions JSON COMMENT '建议',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student (student_id),
  INDEX idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习反馈';
```

---

## 三、前端页面 ↔ 数据库映射

| 前端页面 | 主要表 | API 路径 |
|----------|--------|----------|
| 课堂管理 | english_classroom + class_record | `/api/v1/english/classrooms` |
| 词汇测试 | english_vocab_test + test_record + word_book | `/api/v1/english/vocab-tests` |
| 单词记忆 | english_word + word_book + word_progress | `/api/v1/english/words` |
| 词库选择 | english_word_book | `/api/v1/english/word-books` |
| AI 阅读理解 | english_reading_article + reading_question | `/api/v1/english/ai-reading` |
| AI 情境口语 | english_dialogue_scenario + dialogue_record | `/api/v1/english/ai-dialogue` |
| 语法体系 | english_grammar_point | `/api/v1/english/grammar` |
| 造句练习 | english_sentence_practice + grammar_point | `/api/v1/english/sentences` |
| 学习反馈 | english_feedback | `/api/v1/english/feedback` |
