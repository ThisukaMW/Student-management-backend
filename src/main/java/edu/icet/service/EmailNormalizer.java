package edu.icet.service;

import java.util.Locale;

/** Emails are stored and compared trimmed and lowercased, so "A@x.com " and "a@x.com" are the same account. */
final class EmailNormalizer {

    private EmailNormalizer() {
    }

    static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
