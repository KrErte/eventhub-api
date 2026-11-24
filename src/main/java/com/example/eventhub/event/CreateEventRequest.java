package com.example.eventhub.event;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Request body for {@code POST /api/events}. Messages are in Estonian because they are shown in the UI as-is.
 * Components are {@code @Nullable} because JSON may omit them; Bean Validation rejects missing required values.
 */
public record CreateEventRequest(
        @NotBlank(message = "Pealkiri on kohustuslik")
        @Size(max = 200, message = "Pealkiri võib olla kuni 200 märki")
        @Nullable String title,

        @Size(max = 2000, message = "Kirjeldus võib olla kuni 2000 märki")
        @Nullable String description,

        @NotNull(message = "Algusaeg on kohustuslik")
        @Future(message = "Algusaeg peab olema tulevikus")
        @Nullable Instant startsAt,

        @NotBlank(message = "Toimumiskoht on kohustuslik")
        @Size(max = 200, message = "Toimumiskoht võib olla kuni 200 märki")
        @Nullable String venue,

        @NotNull(message = "Kohtade arv on kohustuslik")
        @Min(value = 1, message = "Kohtade arv peab olema vähemalt 1")
        @Max(value = 100_000, message = "Kohtade arv võib olla kuni 100 000")
        @Nullable Integer capacity) {
}
