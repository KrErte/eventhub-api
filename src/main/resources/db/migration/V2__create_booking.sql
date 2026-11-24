-- Seats already booked for an event. Maintained by a single conditional UPDATE when booking,
-- so concurrent bookings can never exceed the capacity (the CHECK is a last line of defence).
ALTER TABLE event ADD COLUMN booked_seats integer NOT NULL DEFAULT 0;
ALTER TABLE event ADD CONSTRAINT event_booked_seats_check CHECK (booked_seats >= 0 AND booked_seats <= capacity);

CREATE TABLE booking (
    id         uuid PRIMARY KEY,
    event_id   uuid         NOT NULL REFERENCES event (id) ON DELETE CASCADE,
    name       varchar(200) NOT NULL,
    email      varchar(320) NOT NULL,
    seats      integer      NOT NULL CHECK (seats BETWEEN 1 AND 10),
    created_at timestamptz  NOT NULL
);

-- One booking per e-mail address per event (case-insensitive). Also serves lookups by event_id.
CREATE UNIQUE INDEX uq_booking_event_email ON booking (event_id, lower(email));
