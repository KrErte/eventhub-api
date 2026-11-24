package com.example.eventhub.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Request body for {@code POST /api/events/{eventId}/bookings}. Messages are in Estonian because the UI shows
 * them as-is. Components are {@code @Nullable} because JSON may omit them; validation rejects missing values.
 */
public record CreateBookingRequest(
        @NotBlank(message = "Nimi on kohustuslik")
        @Size(max = 200, message = "Nimi võib olla kuni 200 märki")
        @Nullable String name,

        @NotBlank(message = "E-post on kohustuslik")
        @Email(message = "E-posti aadress ei ole korrektne")
        @Size(max = 320, message = "E-posti aadress on liiga pikk")
        @Nullable String email,

        @NotNull(message = "Kohtade arv on kohustuslik")
        @Min(value = 1, message = "Broneerida tuleb vähemalt 1 koht")
        @Max(value = 10, message = "Korraga saab broneerida kuni 10 kohta")
        @Nullable Integer seats) {

    /** Trims text before validation, so " mari@example.com " is accepted as a valid address. */
    public CreateBookingRequest {
        name = name == null ? null : name.strip();
        email = email == null ? null : email.strip();
    }
}
