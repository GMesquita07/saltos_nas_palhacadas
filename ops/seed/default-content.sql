-- Default public content seed for Saltos nas Palhaçadas.
--
-- This is intentionally NOT a Flyway migration.
-- It is designed to be executed manually after the static media assets
-- have been deployed.
--
-- Descriptions, locations and dates are initial editable content.
-- Private notification emails are deliberately NOT versioned here;
-- configure them through the admin interface after deployment.
--
-- Running this script more than once must not duplicate profiles or media.

INSERT INTO profiles (
    slug,
    name,
    role,
    description,
    profile_image_url,
    profile_image_position,
    profile_image_zoom,
    featured_video_url,
    display_order,
    active
)
VALUES
(
    'dj-joao-tomas',
    'DJ João Tomás',
    'DJ & Animador',
    'DJ e animador dedicado a criar ambientes dinâmicos e momentos memoráveis, adaptando a música e a animação a cada evento.',
    'https://www.saltosnaspalhacadas.pt/content/profiles/dj-joao-tomas/profile.jpeg',
    '50% 50%',
    1.0,
    NULL,
    0,
    TRUE
),
(
    'dj-kidg',
    'DJ KidG',
    'DJ & Animador',
    'DJ e animador com uma abordagem energética e versátil, preparado para adaptar cada atuação ao público e ao ambiente do evento.',
    'https://www.saltosnaspalhacadas.pt/content/profiles/dj-kidg/profile.jpeg',
    '50% 50%',
    1.0,
    NULL,
    1,
    TRUE
),
(
    'saltos-nas-palhacadas',
    'Saltos nas Palhaçadas',
    'Animação Infantil',
    'Animação infantil pensada para tornar festas e eventos ainda mais especiais, com atividades divertidas e momentos adaptados às crianças.',
    'https://www.saltosnaspalhacadas.pt/content/profiles/saltos-nas-palhacadas/profile.jpeg',
    '50% 50%',
    1.0,
    NULL,
    2,
    TRUE
)
ON CONFLICT (slug) DO NOTHING;


WITH seed_items (
    profile_slug,
    media_type,
    title,
    location,
    event_date,
    media_url,
    thumbnail_url,
    display_order,
    published
) AS (
    VALUES

    -- DJ João Tomás
    (
        'dj-joao-tomas',
        'PHOTO',
        'DJ João Tomás - Foto 1',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-joao-tomas/photos/dj_joao1.jpeg',
        NULL,
        0,
        TRUE
    ),
    (
        'dj-joao-tomas',
        'PHOTO',
        'DJ João Tomás - Foto 2',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-joao-tomas/photos/dj_joao2.jpeg',
        NULL,
        1,
        TRUE
    ),
    (
        'dj-joao-tomas',
        'PHOTO',
        'DJ João Tomás - Logo',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-joao-tomas/photos/logo_dj_joao.jpeg',
        NULL,
        2,
        TRUE
    ),
    (
        'dj-joao-tomas',
        'VIDEO',
        'DJ João Tomás - Vídeo 1',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-joao-tomas/videos/video1.mp4',
        NULL,
        3,
        TRUE
    ),
    (
        'dj-joao-tomas',
        'VIDEO',
        'DJ João Tomás - Vídeo 2',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-joao-tomas/videos/videeo2.mp4',
        NULL,
        4,
        TRUE
    ),
    (
        'dj-joao-tomas',
        'VIDEO',
        'DJ João Tomás - Vídeo 3',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-joao-tomas/videos/video3.mp4',
        NULL,
        5,
        TRUE
    ),
    (
        'dj-joao-tomas',
        'VIDEO',
        'DJ João Tomás - Vídeo 4',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-joao-tomas/videos/video4.mp4',
        NULL,
        6,
        TRUE
    ),
    (
        'dj-joao-tomas',
        'VIDEO',
        'DJ João Tomás - Vídeo 5',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-joao-tomas/videos/video5.mp4',
        NULL,
        7,
        TRUE
    ),

    -- DJ KidG
    (
        'dj-kidg',
        'PHOTO',
        'DJ KidG - Foto 1',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-kidg/photos/kid1.jpeg',
        NULL,
        0,
        TRUE
    ),
    (
        'dj-kidg',
        'PHOTO',
        'DJ KidG - Foto 2',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-kidg/photos/kid2.jpeg',
        NULL,
        1,
        TRUE
    ),
    (
        'dj-kidg',
        'VIDEO',
        'DJ KidG - Vídeo 1',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-kidg/videos/video1.mp4',
        NULL,
        2,
        TRUE
    ),
    (
        'dj-kidg',
        'VIDEO',
        'DJ KidG - Vídeo 2',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/dj-kidg/videos/video2.mp4',
        NULL,
        3,
        TRUE
    ),

    -- Saltos nas Palhaçadas
    (
        'saltos-nas-palhacadas',
        'VIDEO',
        'Saltos nas Palhaçadas - Vídeo 1',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/saltos-nas-palhacadas/videos/saltos1.mp4',
        NULL,
        0,
        TRUE
    ),
    (
        'saltos-nas-palhacadas',
        'VIDEO',
        'Saltos nas Palhaçadas - Vídeo 2',
        'Portugal',
        DATE '2026-01-01',
        'https://www.saltosnaspalhacadas.pt/content/profiles/saltos-nas-palhacadas/videos/saltos2.mp4',
        NULL,
        1,
        TRUE
    )
)
INSERT INTO portfolio_items (
    profile_id,
    media_type,
    title,
    location,
    event_date,
    media_url,
    thumbnail_url,
    display_order,
    published
)
SELECT
    profile.id,
    seed.media_type,
    seed.title,
    seed.location,
    seed.event_date,
    seed.media_url,
    seed.thumbnail_url,
    seed.display_order,
    seed.published
FROM seed_items seed
JOIN profiles profile
  ON profile.slug = seed.profile_slug
WHERE NOT EXISTS (
    SELECT 1
    FROM portfolio_items existing
    WHERE existing.profile_id = profile.id
      AND existing.media_url = seed.media_url
);
