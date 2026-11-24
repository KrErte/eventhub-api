CREATE TABLE event (
    id          uuid PRIMARY KEY,
    title       varchar(200) NOT NULL,
    description text,
    starts_at   timestamptz  NOT NULL,
    venue       varchar(200) NOT NULL,
    capacity    integer      NOT NULL CHECK (capacity > 0)
);

CREATE INDEX idx_event_starts_at ON event (starts_at);
