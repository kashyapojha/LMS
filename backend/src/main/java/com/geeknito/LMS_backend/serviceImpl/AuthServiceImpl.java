package com.geeknito.LMS_backend.serviceImpl;

import com.geeknito.LMS_backend.dto.LoginRequest;
import com.geeknito.LMS_backend.dto.RegisterRequest;
import com.geeknito.LMS_backend.dto.UserResponse;
import com.geeknito.LMS_backend.entity.Role;
import com.geeknito.LMS_backend.entity.User;
import com.geeknito.LMS_backend.exception.AuthenticationException;
import com.geeknito.LMS_backend.repository.UserRepository;
import com.geeknito.LMS_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    public UserResponse registerAdmin(RegisterRequest request) {
        return registerUser(request, Role.ADMIN);
    }

    @Override
    public UserResponse registerStudent(RegisterRequest request) {
        return registerUser(request, Role.STUDENT);
    }

    private UserResponse registerUser(RegisterRequest request, Role role) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }

        String name = request.getNameToUse();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isEnabled(true)
                .build();

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    @Override
    public UserResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Invalid email or password");
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new AuthenticationException(e.getMessage());
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        return mapToUserResponse(user);
    }

    @Override
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new AuthenticationException("User not found"));
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        String avatar = "https://api.dicebear.com/7.x/initials/svg?seed=" + user.getName().replace(" ", "%20");
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .fullName(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatar(avatar)
                .build();
    }
}
