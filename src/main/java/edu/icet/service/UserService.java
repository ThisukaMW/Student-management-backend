package edu.icet.service;

import edu.icet.entity.Role;
import edu.icet.entity.User;

/** The one place accounts are created and changed, so hashing and the unique-email rule can't be bypassed. */
public interface UserService {

    /** Creates an enabled account with a BCrypt-hashed password. */
    User createUser(String email, String rawPassword, Role role);

    /** Changes the login email, rejecting one that another account already uses. */
    void changeEmail(User user, String newEmail);

    /**
     * Changes role and/or enabled flag; null leaves a value unchanged. Disabling an account
     * revokes its refresh tokens so it can't get new access tokens.
     */
    void updateAccountSettings(User user, Role role, Boolean enabled);

    void delete(User user);
}
