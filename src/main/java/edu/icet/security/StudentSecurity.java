package edu.icet.security;

import edu.icet.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Ownership checks for {@code @PreAuthorize}, e.g. {@code @studentSecurity.isOwner(#id, authentication)}.
 * Kept out of controllers so the rules sit next to the other authorization logic.
 */
@Component("studentSecurity")
@RequiredArgsConstructor
public class StudentSecurity {

    private final StudentRepository studentRepository;

    /**
     * True when the student profile belongs to the signed-in user. False for a profile that doesn't
     * exist, so a student gets 403 rather than 404 and can't probe which ids exist.
     */
    public boolean isOwner(Long studentId, Authentication authentication) {
        Long userId = CurrentUser.id(authentication);
        return userId != null && studentId != null && studentRepository.existsByIdAndUserId(studentId, userId);
    }
}
