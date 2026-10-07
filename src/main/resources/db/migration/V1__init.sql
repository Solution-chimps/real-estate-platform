CREATE TABLE app_user (
    id            CHAR(36)      NOT NULL,
    email         VARCHAR(255)  NOT NULL,
    name          VARCHAR(120)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(20)   NOT NULL,
    enabled       BOOLEAN       NOT NULL,
    mfa_secret    VARCHAR(255),
    mfa_enabled   BOOLEAN       NOT NULL,
    mfa_last_used_step BIGINT,
    created_at    ${timestampType} NOT NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);

CREATE TABLE property (
    id             CHAR(36)       NOT NULL,
    code           VARCHAR(20)    NOT NULL,
    title          VARCHAR(160)   NOT NULL,
    description    VARCHAR(4000)  NOT NULL,
    purpose        VARCHAR(20)    NOT NULL,
    property_type  VARCHAR(20)    NOT NULL,
    neighborhood   VARCHAR(120)   NOT NULL,
    city           VARCHAR(120)   NOT NULL,
    price          DECIMAL(14, 2) NOT NULL,
    condo_fee      DECIMAL(14, 2),
    property_tax   DECIMAL(14, 2),
    area           DECIMAL(10, 2) NOT NULL,
    bedrooms       INT            NOT NULL,
    suites         INT            NOT NULL,
    bathrooms      INT            NOT NULL,
    parking_spaces INT            NOT NULL,
    available_from DATE,
    featured       BOOLEAN        NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    created_at     ${timestampType} NOT NULL,
    updated_at     ${timestampType} NOT NULL,
    CONSTRAINT pk_property PRIMARY KEY (id),
    CONSTRAINT uk_property_code UNIQUE (code)
);

CREATE INDEX idx_property_status ON property (status);
CREATE INDEX idx_property_status_updated ON property (status, updated_at);

CREATE TABLE property_feature (
    property_id CHAR(36)    NOT NULL,
    position    INT         NOT NULL,
    name        VARCHAR(80) NOT NULL,
    CONSTRAINT pk_property_feature PRIMARY KEY (property_id, position),
    CONSTRAINT fk_property_feature_property FOREIGN KEY (property_id) REFERENCES property (id) ON DELETE CASCADE
);

CREATE TABLE property_photo (
    property_id CHAR(36)     NOT NULL,
    position    INT          NOT NULL,
    url         VARCHAR(500) NOT NULL,
    CONSTRAINT pk_property_photo PRIMARY KEY (property_id, position),
    CONSTRAINT fk_property_photo_property FOREIGN KEY (property_id) REFERENCES property (id) ON DELETE CASCADE
);

CREATE TABLE property_code_counter (
    id         INT NOT NULL,
    next_value INT NOT NULL,
    CONSTRAINT pk_property_code_counter PRIMARY KEY (id)
);

INSERT INTO property_code_counter (id, next_value) VALUES (1, 1);

CREATE TABLE contact (
    id          CHAR(36)      NOT NULL,
    property_id CHAR(36),
    name        VARCHAR(120)  NOT NULL,
    phone       VARCHAR(30)   NOT NULL,
    email       VARCHAR(255)  NOT NULL,
    message     VARCHAR(2000) NOT NULL,
    is_read     BOOLEAN       NOT NULL,
    created_at  ${timestampType} NOT NULL,
    CONSTRAINT pk_contact PRIMARY KEY (id),
    CONSTRAINT fk_contact_property FOREIGN KEY (property_id) REFERENCES property (id) ON DELETE SET NULL
);

CREATE INDEX idx_contact_created ON contact (created_at);
