ALTER TABLE conversion_jobs
    ADD COLUMN conversion_type VARCHAR(30) DEFAULT 'DOCX_TO_PDF',
    ADD COLUMN quality INTEGER;

ALTER TABLE conversion_jobs
    ALTER COLUMN conversion_type SET NOT NULL,
    ALTER COLUMN conversion_type DROP DEFAULT;

ALTER TABLE conversion_jobs
    ADD CONSTRAINT ck_conversion_jobs_type
        CHECK (conversion_type IN ('DOCX_TO_PDF', 'IMAGE_TO_WEBP')),
    ADD CONSTRAINT ck_conversion_jobs_quality
        CHECK (quality IS NULL OR quality BETWEEN 1 AND 100),
    ADD CONSTRAINT ck_conversion_jobs_type_quality
        CHECK (
            (conversion_type = 'DOCX_TO_PDF' AND quality IS NULL)
            OR
            (conversion_type = 'IMAGE_TO_WEBP' AND quality IS NOT NULL)
        );
