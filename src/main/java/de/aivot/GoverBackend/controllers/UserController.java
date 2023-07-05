package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.models.entities.DepartmentMembership;
import de.aivot.GoverBackend.models.entities.User;
import de.aivot.GoverBackend.permissions.IsAdmin;
import de.aivot.GoverBackend.repositories.DepartmentMembershipRepository;
import de.aivot.GoverBackend.repositories.UserRepository;
import de.aivot.GoverBackend.services.MailService;
import de.aivot.GoverBackend.services.PasswordService;
import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Optional;

@RestController
public class UserController {
    private final UserRepository repository;
    private final DepartmentMembershipRepository departmentMembershipRepository;
    private final MailService mailService;

    @Autowired
    public UserController(
            UserRepository repository,
            DepartmentMembershipRepository departmentMembershipRepository,
            MailService mailService
    ) {
        this.repository = repository;
        this.departmentMembershipRepository = departmentMembershipRepository;
        this.mailService = mailService;
    }

    @GetMapping("/api/users")
    public Collection<User> list(
            @RequestParam(required = false) Boolean admin,
            @RequestParam(required = false) Integer department
    ) {
        Collection<User> users;
        if (department != null) {
            users = departmentMembershipRepository
                    .findAllByDepartmentId(department)
                    .stream()
                    .map(DepartmentMembership::getUser)
                    .toList();
        } else if (admin != null) {
            users = repository
                    .findAllByAdminOrderByEmail(Boolean.TRUE.equals(admin));
        } else  {
            users = repository
                    .findAllByOrderByEmail();
        }

        return users
                .stream()
                .peek(u -> u.setPassword("")) // Empty all password
                .toList();
    }

    @IsAdmin
    @PostMapping("/api/users")
    public User create(
            Authentication authentication,
            @RequestBody User newUser,
            @RequestParam(required = false) Boolean notify
    ) {
        boolean userExists = repository.existsByEmail(newUser.getEmail());
        if (userExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        // Encode new user password
        String clearPassword = newUser.getPassword();
        newUser.setPassword(PasswordService.encodePassword(newUser.getPassword()));

        User createdUser = repository.save(newUser);
        createdUser.setPassword(""); // Empty password

        if (Boolean.TRUE.equals(notify)) {
            mailService.sendInfoMail(
                    "Benutzerkonto angelegt",
                    "Für Sie wurde ein neues Gover-Benutzerkonto angelegt. Verwenden Sie die E-Mail-Adresse \"" + createdUser.getEmail() + "\" und das Passwort \"" + clearPassword + "\" um sich anzumelden.",
                    createdUser.getEmail()
            );
        }

        return createdUser;
    }

    @GetMapping("/api/users/self")
    public User retrieveSelf(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        user.setPassword(""); // Empty password
        return user;
    }

    @GetMapping("/api/users/{id}")
    public User retrieve(@PathVariable Integer id) {

        return repository
                .findById(id)
                .map(user -> {
                    user.setPassword("");
                    return user;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/api/users/{id}")
    public User update(Authentication authentication, @PathVariable Integer id, @RequestBody User updatedUser) {
        User requester = (User) authentication.getPrincipal();

        if (!requester.isAdmin() && !requester.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Optional<User> user = repository.findById(id);
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (!user.get().getEmail().equals(updatedUser.getEmail())) {
            boolean userExists = repository.existsByEmail(updatedUser.getEmail());
            if (userExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
        }

        User existingUser = user.get();

        if (StringUtils.isNotNullOrEmpty(updatedUser.getName())) {
            existingUser.setName(updatedUser.getName());
        }
        if (StringUtils.isNotNullOrEmpty(updatedUser.getEmail())) {
            existingUser.setEmail(updatedUser.getEmail());
        }

        if (requester.isAdmin()) {
            existingUser.setAdmin(updatedUser.isAdmin());
            existingUser.setActive(updatedUser.isActive());
        }

        if (StringUtils.isNotNullOrEmpty(updatedUser.getPassword())) {
            existingUser.setPassword(PasswordService.encodePassword(updatedUser.getPassword()));
        }

        User savedUser = repository.save(existingUser);
        savedUser.setPassword(""); // Empty password
        return savedUser;
    }

    @IsAdmin
    @DeleteMapping("/api/users/{id}")
    public void destroy(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Optional<User> optUser = repository.findById(id);
        if (optUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        User user = optUser.get();
        user.setActive(false);
        user.setName("Inaktiver Nutzer");
        user.setEmail("inaktiv." + StringUtils.generateRandomString(16) + "@gover.digital");
        repository.save(user);
    }
}
