CREATE TABLE subject (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    datasource_id VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT DEFAULT 0
);

CREATE INDEX idx_subject_datasource ON subject(datasource_id);

CREATE TABLE subject_table (
    id VARCHAR(64) PRIMARY KEY,
    subject_id VARCHAR(64) NOT NULL,
    table_id VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted BIT DEFAULT 0,
    CONSTRAINT uk_subject_table UNIQUE (subject_id, table_id)
);

CREATE INDEX idx_subject_table_subject ON subject_table(subject_id);

CREATE TABLE term (
    id VARCHAR(64) PRIMARY KEY,
    subject_id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT DEFAULT 0
);

CREATE INDEX idx_term_subject ON term(subject_id);

CREATE TABLE term_relation (
    id VARCHAR(64) PRIMARY KEY,
    term_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(16) NOT NULL,
    table_id VARCHAR(64) NOT NULL,
    field_name VARCHAR(128),
    deleted BIT DEFAULT 0,
    CONSTRAINT uk_term_relation UNIQUE (term_id, table_id, field_name)
);

CREATE INDEX idx_term_relation_term ON term_relation(term_id);