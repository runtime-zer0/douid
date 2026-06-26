CREATE TABLE work_media (
    id         BIGSERIAL PRIMARY KEY,
    work_id    BIGINT NOT NULL,
    media_id   BIGINT NOT NULL,
    role       VARCHAR(30) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    alt_text   VARCHAR(255),
    CONSTRAINT fk_work_media_work FOREIGN KEY (work_id) REFERENCES works(id),
    CONSTRAINT fk_work_media_media FOREIGN KEY (media_id) REFERENCES media(id)
);
