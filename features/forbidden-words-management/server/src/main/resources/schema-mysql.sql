CREATE TABLE IF NOT EXISTS forbidden_word (
  id BIGINT NOT NULL AUTO_INCREMENT,
  word VARCHAR(255) NOT NULL,
  platform VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_forbidden_word_platform (word, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  action VARCHAR(32) NOT NULL,
  operator VARCHAR(64) NOT NULL,
  operator_ip VARCHAR(64) NOT NULL,
  target_word VARCHAR(255) NOT NULL,
  platform VARCHAR(32) NOT NULL,
  operation_time DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_operation_time (operation_time),
  KEY idx_operation_operator (operator),
  KEY idx_operation_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hit_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  actor VARCHAR(64) NOT NULL,
  platform VARCHAR(32) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  hit_word VARCHAR(255) NOT NULL,
  action VARCHAR(32) NOT NULL,
  action_time DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_hit_actor_time (actor, action_time),
  KEY idx_hit_time (action_time),
  KEY idx_hit_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
