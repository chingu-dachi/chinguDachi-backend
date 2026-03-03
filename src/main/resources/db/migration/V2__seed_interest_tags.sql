-- =============================================================
-- interest_tag 초기 데이터 (15개 고정 태그)
-- docs/design/wireframes/02-onboarding.md 기준
-- =============================================================

INSERT INTO interest_tag (tag_key, label_ko, label_ja, display_order, created_at, updated_at) VALUES
('TRAVEL',   '여행',        '旅行',           1,  now(), now()),
('FOOD',     '맛집',        'グルメ',          2,  now(), now()),
('CAFE',     '카페',        'カフェ',          3,  now(), now()),
('MUSIC',    '음악',        '音楽',            4,  now(), now()),
('KPOP',     'K-POP',      'K-POP',          5,  now(), now()),
('JPOP',     'J-POP',      'J-POP',          6,  now(), now()),
('ANIME',    '애니·만화',   'アニメ·漫画',      7,  now(), now()),
('MOVIE',    '영화·드라마', '映画·ドラマ',      8,  now(), now()),
('GAMING',   '게임',        'ゲーム',          9,  now(), now()),
('FASHION',  '패션',        'ファッション',     10, now(), now()),
('SPORTS',   '운동',        'スポーツ',        11, now(), now()),
('LANGUAGE', '언어교환',    '言語交換',         12, now(), now()),
('COOKING',  '요리',        '料理',            13, now(), now()),
('PHOTO',    '사진',        '写真',            14, now(), now()),
('READING',  '독서',        '読書',            15, now(), now());
interest_tag