package edu.icet.repository;

import edu.icet.entity.Student;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Optional, combinable search filters for the student list. Blank filters are ignored. */
public final class StudentSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private StudentSpecifications() {
    }

    public static Specification<Student> withFilters(String name, String batch, String institute) {
        List<Specification<Student>> specs = new ArrayList<>();
        if (StringUtils.hasText(name)) {
            specs.add(nameContains(name));
        }
        if (StringUtils.hasText(batch)) {
            specs.add(equalsIgnoreCase("batch", batch));
        }
        if (StringUtils.hasText(institute)) {
            specs.add(equalsIgnoreCase("institute", institute));
        }
        return Specification.allOf(specs);
    }

    /** Matches when the first OR last name contains the text, ignoring case. */
    static Specification<Student> nameContains(String name) {
        String pattern = "%" + escapeLike(name.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("lastName")), pattern, LIKE_ESCAPE));
    }

    static Specification<Student> equalsIgnoreCase(String attribute, String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get(attribute)), normalized);
    }

    // Treat % and _ typed by the user as literal characters, not wildcards
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
