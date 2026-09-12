ALTER TABLE conversion_jobs
    ADD COLUMN access_token_hash VARCHAR(64);

UPDATE conversion_jobs
SET access_token_hash = encode(
        sha256((random()::TEXT || clock_timestamp()::TEXT || id::TEXT)::BYTEA),
        'hex'
    );

ALTER TABLE conversion_jobs
    ALTER COLUMN access_token_hash SET NOT NULL;

CREATE UNIQUE INDEX ux_conversion_jobs_access_token_hash
    ON conversion_jobs (access_token_hash);
