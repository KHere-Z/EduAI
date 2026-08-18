ALTER TABLE question_bank ADD COLUMN question_type VARCHAR(10) DEFAULT NULL COMMENT '选择题/填空题/计算题/解答题';
ALTER TABLE question_bank ADD COLUMN shared TINYINT(1) DEFAULT 1 COMMENT '1=共享公域 0=私域';
