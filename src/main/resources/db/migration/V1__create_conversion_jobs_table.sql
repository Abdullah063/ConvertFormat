CREATE TABLE conversion_jobs (
                                 id UUID PRIMARY KEY,
                                 original_file_name VARCHAR(255) NOT NULL,
                                 source_storage_key VARCHAR(255) NOT NULL UNIQUE,
                                 result_storage_key VARCHAR(255),
                                 status VARCHAR(20) NOT NULL,
                                 error_message VARCHAR(1000),
                                 created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
                                 completed_at TIMESTAMP(6) WITHOUT TIME ZONE,

                                 CONSTRAINT ck_conversion_jobs_status
                                     CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);