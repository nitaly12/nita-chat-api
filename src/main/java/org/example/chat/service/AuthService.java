package org.example.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.JwtResponse;
import org.example.chat.dto.request.LoginRequest;
import org.example.chat.dto.request.RegisterRequest;
import org.example.chat.entity.PasswordResetToken;
import org.example.chat.entity.RefreshToken;
import org.example.chat.entity.Role;
import org.example.chat.entity.User;
import org.example.chat.exception.InvalidCredentialsException;
import org.example.chat.exception.UsernameAlreadyExistsException;
import org.example.chat.repository.PasswordResetTokenRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UsernameAlreadyExistsException("Email already registered: " + request.getEmail());
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(user);
    }

    @Transactional
    public JwtResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        String accessToken = jwtUtils.generateToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("No account found for this email"));

        passwordResetTokenRepository.deleteByUser(user);

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        passwordResetTokenRepository.save(new PasswordResetToken(otp, user));

        emailService.sendPasswordResetOtp(email, otp);
    }

    @Transactional(readOnly = true)
    public void verifyOtp(String email, String otp) {
        PasswordResetToken resetToken = loadValidOtp(email, otp);
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("OTP has expired");
        }
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        PasswordResetToken resetToken = loadValidOtp(email, otp);

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new InvalidCredentialsException("OTP has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }

    private PasswordResetToken loadValidOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid OTP"));

        PasswordResetToken resetToken = passwordResetTokenRepository.findByUser(user)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid OTP"));

        if (!resetToken.getToken().equals(otp)) {
            throw new InvalidCredentialsException("Invalid OTP");
        }
        return resetToken;
    }
}
