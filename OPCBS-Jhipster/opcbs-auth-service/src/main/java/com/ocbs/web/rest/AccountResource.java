package com.ocbs.web.rest;

import com.ocbs.domain.User;
import com.ocbs.repository.UserRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.MailService;
import com.ocbs.service.UserService;
import com.ocbs.service.dto.AdminUserDTO;
import com.ocbs.service.dto.PasswordChangeDTO;
import com.ocbs.web.rest.errors.*;
import com.ocbs.web.rest.vm.KeyAndPasswordVM;
import com.ocbs.web.rest.vm.ManagedUserVM;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
@Validated
public class AccountResource {

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Account resource request invalid")
    private static class AccountResourceException extends RuntimeException {

        private AccountResourceException(String message) {
            super(message);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AccountResource.class);

    private final UserRepository userRepository;

    private final UserService userService;

    private final MailService mailService;

    private final PasswordEncoder passwordEncoder;
    private final com.ocbs.security.TokenBlacklistService tokenBlacklistService;
    private final com.ocbs.event.UserEventProducer userEventProducer;

    public AccountResource(
        UserRepository userRepository,
        UserService userService,
        MailService mailService,
        PasswordEncoder passwordEncoder,
        com.ocbs.security.TokenBlacklistService tokenBlacklistService,
        com.ocbs.event.UserEventProducer userEventProducer
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userEventProducer = userEventProducer;
    }

    /**
     * {@code POST  /register} : register the user.
     */
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<java.util.Map<String, String>> registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        LOG.debug("REST request to register account");
        if (isPasswordLengthInvalid(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerUser(managedUserVM, managedUserVM.getPassword());
        mailService.sendActivationOtpEmail(user, user.getActivationKey());
        userEventProducer.publishUserEvent(new com.ocbs.event.UserEvent(
            user.getId(),
            user.getLogin(),
            user.getEmail(),
            (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : ""),
            "ROLE_PATIENT",
            "USER_REGISTERED"
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of(
            "status", "CREATED",
            "message", "Đăng ký thành công! Mã OTP đã được gửi đến email của bạn.",
            "email", user.getEmail(),
            "otp", user.getActivationKey() != null ? user.getActivationKey() : ""
        ));
    }

    /**
     * {@code POST  /register-doctor} : register a doctor account with professional profile.
     */
    @PostMapping("/register-doctor")
    @Transactional
    public ResponseEntity<java.util.Map<String, String>> registerDoctorAccount(@Valid @RequestBody com.ocbs.web.rest.vm.ManagedDoctorVM managedDoctorVM) {
        LOG.debug("REST request to register doctor account");
        if (isPasswordLengthInvalid(managedDoctorVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerDoctor(managedDoctorVM);
        mailService.sendActivationOtpEmail(user, user.getActivationKey());
        userEventProducer.publishUserEvent(new com.ocbs.event.UserEvent(
            user.getId(),
            user.getLogin(),
            user.getEmail(),
            (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : ""),
            "ROLE_DOCTOR",
            "USER_REGISTERED"
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of(
            "status", "CREATED",
            "message", "Đăng ký tài khoản Bác sĩ thành công! Mã OTP đã được gửi đến email của bạn.",
            "email", user.getEmail(),
            "otp", user.getActivationKey() != null ? user.getActivationKey() : ""
        ));
    }

    public static class VerifyOtpVM {
        private String emailOrLogin;
        private String otp;

        public String getEmailOrLogin() { return emailOrLogin; }
        public void setEmailOrLogin(String emailOrLogin) { this.emailOrLogin = emailOrLogin; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    public static class ResendOtpVM {
        private String emailOrLogin;

        public String getEmailOrLogin() { return emailOrLogin; }
        public void setEmailOrLogin(String emailOrLogin) { this.emailOrLogin = emailOrLogin; }
    }

    /**
     * {@code POST  /account/verify-otp} : verify 6-digit OTP code to activate account.
     */
    @PostMapping("/account/verify-otp")
    @Transactional
    public ResponseEntity<java.util.Map<String, String>> verifyOtp(@RequestBody VerifyOtpVM vm) {
        LOG.debug("REST request to verify OTP for: {}", vm.getEmailOrLogin());
        com.ocbs.service.UserService.OtpVerificationResult result = userService.verifyOtpStatus(vm.getEmailOrLogin(), vm.getOtp());
        
        switch (result) {
            case SUCCESS -> {
                userRepository.findOneByLogin(vm.getEmailOrLogin().toLowerCase())
                    .or(() -> userRepository.findOneByEmailIgnoreCase(vm.getEmailOrLogin()))
                    .ifPresent(u -> userEventProducer.publishUserEvent(new com.ocbs.event.UserEvent(
                        u.getId(), u.getLogin(), u.getEmail(),
                        (u.getFirstName() != null ? u.getFirstName() : "") + " " + (u.getLastName() != null ? u.getLastName() : ""),
                        u.getAuthorities().stream().map(com.ocbs.domain.Authority::getName).sorted().findFirst().orElse("ROLE_USER"),
                        "USER_ACTIVATED"
                    )));
                return ResponseEntity.ok(java.util.Map.of("status", "SUCCESS", "message", "Kích hoạt tài khoản thành công!"));
            }
            case OTP_EXPIRED -> {
                throw new BadRequestAlertException("Mã OTP đã hết hạn (chỉ có hiệu lực trong 1 phút 30 giây). Vui lòng nhấn 'Gửi lại mã OTP'.", "userManagement", "otpExpired");
            }
            case ALREADY_ACTIVATED -> {
                throw new BadRequestAlertException("Tài khoản này đã được kích hoạt trước đó. Bạn có thể đăng nhập ngay.", "userManagement", "alreadyActivated");
            }
            case USER_NOT_FOUND -> {
                throw new BadRequestAlertException("Không tìm thấy tài khoản với email hoặc tên đăng nhập này.", "userManagement", "userNotFound");
            }
            default -> {
                throw new BadRequestAlertException("Mã OTP không chính xác. Vui lòng kiểm tra lại.", "userManagement", "invalidOtp");
            }
        }
    }

    /**
     * {@code POST  /account/resend-otp} : resend new 6-digit OTP code.
     */
    @PostMapping("/account/resend-otp")
    public ResponseEntity<java.util.Map<String, String>> resendOtp(@RequestBody ResendOtpVM vm) {
        LOG.debug("REST request to resend OTP for: {}", vm.getEmailOrLogin());
        Optional<User> user = userService.resendOtp(vm.getEmailOrLogin());
        if (user.isEmpty()) {
            throw new BadRequestAlertException("Không tìm thấy tài khoản với email hoặc username này, hoặc tài khoản đã kích hoạt.", "userManagement", "userNotFound");
        }
        User foundUser = user.get();
        return ResponseEntity.ok(java.util.Map.of(
            "status", "SUCCESS",
            "message", "Mã OTP mới đã được gửi lại vào email của bạn.",
            "email", foundUser.getEmail() != null ? foundUser.getEmail() : "",
            "otp", foundUser.getActivationKey() != null ? foundUser.getActivationKey() : ""
        ));
    }

    /**
     * {@code GET  /activate} : activate the registered user.
     */
    @GetMapping("/activate")
    public void activateAccount(@RequestParam(value = "key") String key) {
        LOG.debug("REST request to activate account");
        Optional<User> user = userService.activateRegistration(key);
        if (user.isEmpty()) {
            throw new AccountResourceException("No user was found for this activation key");
        }
    }

    /**
     * {@code GET  /account} : get the current user.
     *
     * @return the current user.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user couldn't be returned.
     */
    @GetMapping("/account")
    public AdminUserDTO getAccount() {
        LOG.debug("REST request to get account");
        return userService
            .getUserWithAuthorities()
            .map(AdminUserDTO::new)
            .orElseThrow(() -> new AccountResourceException("User could not be found"));
    }

    /**
     * {@code GET  /account/profile} : get the current user profile including patient or doctor profile.
     *
     * @return the current user profile.
     */
    @GetMapping("/account/profile")
    public com.ocbs.service.dto.UserProfileDTO getProfile() {
        LOG.debug("REST request to get current user profile");
        return userService
            .getCurrentUserProfile()
            .orElseThrow(() -> new AccountResourceException("User profile could not be found"));
    }

    /**
     * {@code PUT  /account/profile} : update the current user profile.
     *
     * @param profileDTO the updated user profile DTO.
     * @return the updated user profile.
     */
    @PutMapping("/account/profile")
    public com.ocbs.service.dto.UserProfileDTO updateProfile(@Valid @RequestBody com.ocbs.service.dto.UserProfileDTO profileDTO) {
        LOG.debug("REST request to update current user profile");
        return userService.updateCurrentUserProfile(profileDTO);
    }

    /**
     * {@code POST  /account} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user login wasn't found.
     */
    @PostMapping("/account")
    public void saveAccount(@Valid @RequestBody AdminUserDTO userDTO) {
        LOG.debug("REST request to save account");
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new AccountResourceException("Current user login not found")
        );
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
        if (existingUser.isPresent() && !existingUser.orElseThrow().getLogin().equalsIgnoreCase(userLogin)) {
            throw new EmailAlreadyUsedException();
        }
        Optional<User> user = userRepository.findOneByLogin(userLogin);
        if (user.isEmpty()) {
            throw new AccountResourceException("User could not be found");
        }
        userService.updateUser(
            userDTO.getFirstName(),
            userDTO.getLastName(),
            userDTO.getEmail(),
            userDTO.getLangKey(),
            userDTO.getImageUrl()
        );
    }

    /**
     * {@code POST  /account/change-password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/account/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        LOG.debug("REST request to change password");
        if (isPasswordLengthInvalid(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());

        // =========================================================================
        // [ENTERPRISE SECURITY - REDIS TOKEN BLACKLIST]:
        // Khi người dùng đổi mật khẩu thành công, lập tức vô hiệu hóa toàn bộ token
        // được cấp trước thời điểm này trên Redis, buộc đăng nhập lại bằng mật khẩu mới.
        // =========================================================================
        com.ocbs.security.SecurityUtils.getCurrentUserLogin().ifPresent(login -> {
            tokenBlacklistService.blacklistUserTokens(login);
            LOG.info(">>> [REDIS BLACKLIST] Đã hủy toàn bộ phiên làm việc cũ sau khi user '{}' đổi mật khẩu.", login);
        });
    }

    /**
     * {@code POST   /account/reset-password/init} : Send an email to reset the password of the user.
     *
     * @param mail the mail of the user.
     */
    @PostMapping(path = "/account/reset-password/init")
    public void requestPasswordReset(@RequestBody @Email @Size(min = 5, max = 254) String mail) {
        LOG.debug("REST request to request password reset");
        Optional<User> user = userService.requestPasswordReset(mail);
        if (user.isPresent()) {
            mailService.sendPasswordResetMail(user.orElseThrow());
        } else {
            // Pretend the request has been successful to prevent checking which emails really exist
            // but log that an invalid attempt has been made
            LOG.warn("Password reset requested for non existing mail");
        }
    }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the password could not be reset.
     */
    @PostMapping(path = "/account/reset-password/finish")
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (isPasswordLengthInvalid(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user = userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (user.isEmpty()) {
            // Dummy hash to prevent reset-key enumeration via response-time timing attack:
            // mirrors the bcrypt cost of a successful path so both branches take equal time.
            passwordEncoder.encode(keyAndPassword.getNewPassword());
            throw new AccountResourceException("No user was found for this reset key");
        }
    }

    private static boolean isPasswordLengthInvalid(String password) {
        return (
            StringUtils.isEmpty(password) ||
            password.length() < ManagedUserVM.PASSWORD_MIN_LENGTH ||
            password.length() > ManagedUserVM.PASSWORD_MAX_LENGTH
        );
    }
}
