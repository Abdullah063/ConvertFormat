ALTER TABLE conversion_jobs
    DROP CONSTRAINT ck_conversion_jobs_type,
    DROP CONSTRAINT ck_conversion_jobs_type_quality;

ALTER TABLE conversion_jobs
    ADD CONSTRAINT ck_conversion_jobs_type
        CHECK (
            conversion_type IN (
                'DOCX_TO_PDF',
                'PPTX_TO_PDF',
                'XLSX_TO_PDF',
                'IMAGE_TO_WEBP',
                'WEBP_TO_JPEG',
                'WEBP_TO_PNG',
                'PNG_TO_JPEG',
                'JPEG_TO_PNG',
                'IMAGE_TO_PDF'
            )
        ),
    ADD CONSTRAINT ck_conversion_jobs_type_quality
        CHECK (
            (conversion_type IN ('IMAGE_TO_WEBP', 'WEBP_TO_JPEG', 'PNG_TO_JPEG')
                AND quality IS NOT NULL)
            OR
            (conversion_type IN (
                'DOCX_TO_PDF',
                'PPTX_TO_PDF',
                'XLSX_TO_PDF',
                'WEBP_TO_PNG',
                'JPEG_TO_PNG',
                'IMAGE_TO_PDF'
            ) AND quality IS NULL)
        );
