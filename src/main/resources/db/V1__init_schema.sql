CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ATTENDEE', 'ORGANIZER', 'ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organizer_id BIGINT NOT NULL REFERENCES users (id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    venue_name VARCHAR(200) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE bookings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    event_id BIGINT NOT NULL REFERENCES events (id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'CANCELLED')),
    total_paise BIGINT NOT NULL CHECK (total_paise >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE event_seats (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events (id),
    section VARCHAR(50) NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    seat_number INT NOT NULL,
    price_paise BIGINT NOT NULL CHECK (price_paise >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'HELD', 'SOLD')),
    held_by BIGINT REFERENCES users (id),
    held_until TIMESTAMPTZ,
    booking_id BIGINT REFERENCES bookings (id),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (event_id, section, row_label, seat_number)
);

CREATE INDEX idx_event_seats_event_status ON event_seats (event_id, status);
CREATE INDEX idx_event_seats_held_until ON event_seats (held_until) WHERE status = 'HELD';

CREATE TABLE payments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings (id),
    provider_order_id VARCHAR(100) NOT NULL,
    provider_payment_id VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    amount_paise BIGINT NOT NULL CHECK (amount_paise >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payment_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider_event_id VARCHAR(100) NOT NULL UNIQUE,
    payload TEXT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
