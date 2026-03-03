-- =============================================================
-- chinguDachi PostgreSQL Schema — V1 초기 스키마
-- 엔티티 기준 전체 테이블 생성 (번역 컬럼 포함)
-- =============================================================

-- 유저 정보
CREATE TABLE account
(
    account_id         BIGINT GENERATED ALWAYS AS IDENTITY,
    account_type       VARCHAR(10)  NOT NULL,
    account_status     VARCHAR(20)  NOT NULL,
    email              VARCHAR(255),
    nickname           VARCHAR(20)  UNIQUE,
    birth_date         DATE,
    nation             VARCHAR(20),
    translate_language VARCHAR(2),
    city               VARCHAR(20),
    profile_image_url  VARCHAR(255),
    bio                VARCHAR(50),
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    deleted_at         TIMESTAMP,

    PRIMARY KEY (account_id)
);

-- OAuth 인증 정보
CREATE TABLE account_credential
(
    account_credential_id BIGINT GENERATED ALWAYS AS IDENTITY,
    account_id            BIGINT       NOT NULL,
    credential_type       VARCHAR(20)  NOT NULL,
    oauth_key             VARCHAR(255),
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,

    PRIMARY KEY (account_credential_id),
    FOREIGN KEY (account_id) REFERENCES account (account_id),
    CONSTRAINT uk_account_credential_account_type UNIQUE (account_id, credential_type),
    CONSTRAINT uk_account_credential_type_oauth_key UNIQUE (credential_type, oauth_key)
);

-- 약관 동의
CREATE TABLE account_terms_agreement
(
    account_terms_agreement_id BIGINT  GENERATED ALWAYS AS IDENTITY,
    account_id                 BIGINT  NOT NULL,
    is_terms_of_service_agree  BOOLEAN NOT NULL,
    is_privacy_policy_agree    BOOLEAN NOT NULL,
    is_marketing_agree         BOOLEAN NOT NULL,
    created_at                 TIMESTAMP NOT NULL,
    updated_at                 TIMESTAMP NOT NULL,

    PRIMARY KEY (account_terms_agreement_id),
    FOREIGN KEY (account_id) REFERENCES account (account_id)
);

-- JWT 리프레시 토큰
CREATE TABLE auth_token
(
    auth_token_id BIGINT GENERATED ALWAYS AS IDENTITY,
    account_id    BIGINT       NOT NULL,
    refresh_token VARCHAR(500) NOT NULL,
    expires_at    TIMESTAMP    NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,

    PRIMARY KEY (auth_token_id),
    FOREIGN KEY (account_id) REFERENCES account (account_id)
);

-- 관심사 태그 (다국어 라벨)
CREATE TABLE interest_tag
(
    interest_tag_id BIGINT GENERATED ALWAYS AS IDENTITY,
    tag_key         VARCHAR(20) NOT NULL UNIQUE,
    label_ko        VARCHAR(20) NOT NULL,
    label_ja        VARCHAR(20) NOT NULL,
    display_order   INTEGER     NOT NULL,
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL,

    PRIMARY KEY (interest_tag_id)
);

-- 유저-관심사 매핑
CREATE TABLE user_interest
(
    user_interest_id BIGINT    GENERATED ALWAYS AS IDENTITY,
    account_id       BIGINT    NOT NULL,
    interest_tag_id  BIGINT    NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL,

    PRIMARY KEY (user_interest_id),
    FOREIGN KEY (account_id) REFERENCES account (account_id),
    FOREIGN KEY (interest_tag_id) REFERENCES interest_tag (interest_tag_id),
    CONSTRAINT uk_user_interest_account_tag UNIQUE (account_id, interest_tag_id)
);

-- 채팅방
CREATE TABLE chat_room
(
    chat_room_id    BIGINT      GENERATED ALWAYS AS IDENTITY,
    room_name       VARCHAR(20) NOT NULL,
    room_owner_id   BIGINT      NOT NULL,
    room_type       VARCHAR(10) NOT NULL DEFAULT 'DIRECT',
    room_status     VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    direct_pair_key VARCHAR(50),
    last_message_at TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL,

    PRIMARY KEY (chat_room_id),
    FOREIGN KEY (room_owner_id) REFERENCES account (account_id)
);

-- 채팅방 참여자
CREATE TABLE room_member
(
    room_member_id BIGINT      GENERATED ALWAYS AS IDENTITY,
    chat_room_id   BIGINT      NOT NULL,
    account_id     BIGINT      NOT NULL,
    member_status  VARCHAR(20) NOT NULL,
    unread_count   INTEGER     NOT NULL DEFAULT 0,
    last_read_at   TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL,

    PRIMARY KEY (room_member_id),
    FOREIGN KEY (account_id) REFERENCES account (account_id),
    FOREIGN KEY (chat_room_id) REFERENCES chat_room (chat_room_id)
);

-- 메시지 (번역 컬럼 포함)
CREATE TABLE chat_message
(
    chat_message_id    BIGINT GENERATED ALWAYS AS IDENTITY,
    chat_room_id       BIGINT         NOT NULL,
    sender_id          BIGINT         NOT NULL,
    content            VARCHAR(1000)  NOT NULL,
    message_status     VARCHAR(10)    NOT NULL DEFAULT 'SENT',
    translated_content VARCHAR(1000),
    translation_status VARCHAR(10)    NOT NULL DEFAULT 'NONE',
    source_language    VARCHAR(2),
    target_language    VARCHAR(2),
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),

    PRIMARY KEY (chat_message_id),
    FOREIGN KEY (sender_id) REFERENCES account (account_id),
    FOREIGN KEY (chat_room_id) REFERENCES chat_room (chat_room_id)
);

CREATE INDEX idx_chat_message_room_id ON chat_message (chat_room_id, chat_message_id DESC);
