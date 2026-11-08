package edu.icet.service;

import edu.icet.entity.Role;
import edu.icet.entity.User;

/** The one place accounts are created and changed, so hashing and the unique-email rule can't be bypassed. */
public interface UserService {

    /** Creates an enabled account with a BCrypt-hashed password. */
    User createUser(String email, String rawPassword, Role role);

    /** Changes the login email, rejecting one that another account already uses. */
    void changeEmail(User user, String newEmail);

    void delete(User user);
}
