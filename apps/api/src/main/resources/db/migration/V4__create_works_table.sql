CREATE TABLE works (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL,
    summary     VARCHAR(500),
    description TEXT,
    category_id BIGINT,
    visibility  VARCHAR(20) NOT NULL DEFAULT 'HIDDEN',
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_works_slug UNIQUE (slug),
    CONSTRAINT fk_works_category FOREIGN KEY (category_id) REFERENCES categories(id)
);
