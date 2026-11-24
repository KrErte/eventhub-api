-- Dev-only sample data (loaded by the "dev" profile, see application-dev.properties).
-- Repeatable migration: re-runs whenever this file changes; ON CONFLICT keeps it idempotent.
INSERT INTO event (id, title, description, starts_at, venue, capacity) VALUES
    ('0193f6a0-0000-7000-8000-000000000001', 'Jazzkaar avakontsert', 'Festivali avaõhtu Kultuurikatlas.', '2027-04-20 19:00:00+03', 'Kultuurikatel, Tallinn', 400),
    ('0193f6a0-0000-7000-8000-000000000002', 'Frontend meetup', 'Angulari ja Spring Booti õhtu koos pitsaga.', '2027-02-11 18:00:00+02', 'Tehnopol, Tallinn', 80),
    ('0193f6a0-0000-7000-8000-000000000003', 'Tartu talvine jooks', NULL, '2027-01-30 11:00:00+02', 'Raekoja plats, Tartu', 1500)
ON CONFLICT (id) DO NOTHING;
