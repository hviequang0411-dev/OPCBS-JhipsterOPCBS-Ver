package com.ocbs.service;

import com.ocbs.config.Constants;
import com.ocbs.domain.Authority;
import com.ocbs.domain.User;
import com.ocbs.repository.AuthorityRepository;
import com.ocbs.repository.UserRepository;
import com.ocbs.security.AuthoritiesConstants;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.dto.AdminUserDTO;
import com.ocbs.service.dto.UserDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.security.RandomUtil;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthorityRepository authorityRepository;

    private final com.ocbs.repository.PatientProfileRepository patientProfileRepository;

    private final com.ocbs.repository.DoctorProfileRepository doctorProfileRepository;

    private final com.ocbs.repository.SpecializationRepository specializationRepository;

    private final com.ocbs.service.mapper.PatientProfileMapper patientProfileMapper;

    private final com.ocbs.service.mapper.DoctorProfileMapper doctorProfileMapper;

    private final MailService mailService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthorityRepository authorityRepository,
            com.ocbs.repository.PatientProfileRepository patientProfileRepository,
            com.ocbs.repository.DoctorProfileRepository doctorProfileRepository,
            com.ocbs.repository.SpecializationRepository specializationRepository,
            com.ocbs.service.mapper.PatientProfileMapper patientProfileMapper,
            com.ocbs.service.mapper.DoctorProfileMapper doctorProfileMapper,
            MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.specializationRepository = specializationRepository;
        this.patientProfileMapper = patientProfileMapper;
        this.doctorProfileMapper = doctorProfileMapper;
        this.mailService = mailService;
    }

    public Optional<User> activateRegistration(String key) {
        LOG.debug("Activating user for activation key {}", key);
        return userRepository.findOneByActivationKey(key).map(user -> {
            // activate given user for the registration key.
            user.setActivated(true);
            user.setActivationKey(null);
            LOG.debug("Activated user: {}", user);
            return user;
        });
    }

    public Optional<User> completePasswordReset(String newPassword, String key) {
        LOG.debug("Reset user password for reset key {}", key);
        return userRepository
                .findOneByResetKey(key)
                .filter(user -> user.getResetDate().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setResetKey(null);
                    user.setResetDate(null);
                    return user;
                });
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository
                .findOneByEmailIgnoreCase(mail)
                .filter(User::isActivated)
                .map(user -> {
                    user.setResetKey(RandomUtil.generateResetKey());
                    user.setResetDate(Instant.now());
                    return user;
                });
    }

    public User registerUser(AdminUserDTO userDTO, String password) {
        userRepository.findOneByLogin(userDTO.getLogin().toLowerCase()).ifPresent(existingUser -> {
            boolean removed = removeNonActivatedUser(existingUser);
            if (!removed) {
                throw new UsernameAlreadyUsedException();
            }
        });
        userRepository.findOneByEmailIgnoreCase(userDTO.getEmail()).ifPresent(existingUser -> {
            boolean removed = removeNonActivatedUser(existingUser);
            if (!removed) {
                throw new EmailAlreadyUsedException();
            }
        });
        User newUser = new User();
        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setLogin(userDTO.getLogin().toLowerCase());
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userDTO.getFirstName());
        newUser.setLastName(userDTO.getLastName());
        if (userDTO.getEmail() != null) {
            newUser.setEmail(userDTO.getEmail().toLowerCase());
        }
        newUser.setImageUrl(userDTO.getImageUrl());
        newUser.setLangKey(userDTO.getLangKey());
        // Generate 6-digit OTP for Email Activation
        String otp = String.format("%06d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 1000000));
        newUser.setActivated(false);
        newUser.setActivationKey(otp);
        newUser.setResetDate(Instant.now()); // Mark OTP created timestamp
        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
        authorityRepository.findById(AuthoritiesConstants.PATIENT).ifPresent(authorities::add);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);

        // Auto-create PatientProfile
        com.ocbs.domain.PatientProfile patientProfile = new com.ocbs.domain.PatientProfile();
        String fullName = "";
        if (userDTO.getFirstName() != null || userDTO.getLastName() != null) {
            fullName = ((userDTO.getFirstName() != null ? userDTO.getFirstName() : "") + " " +
                    (userDTO.getLastName() != null ? userDTO.getLastName() : "")).trim();
        }
        if (fullName.isEmpty()) {
            fullName = userDTO.getLogin();
        }
        patientProfile.setFullName(fullName);
        patientProfile.setPhone("N/A");
        patientProfile.setUser(newUser);
        patientProfileRepository.save(patientProfile);

        LOG.debug("Created Information for User and PatientProfile: {}", newUser);
        return newUser;
    }

    public User registerDoctor(com.ocbs.web.rest.vm.ManagedDoctorVM doctorVM) {
        userRepository.findOneByLogin(doctorVM.getLogin().toLowerCase()).ifPresent(existingUser -> {
            boolean removed = removeNonActivatedUser(existingUser);
            if (!removed) {
                throw new UsernameAlreadyUsedException();
            }
        });
        userRepository.findOneByEmailIgnoreCase(doctorVM.getEmail()).ifPresent(existingUser -> {
            boolean removed = removeNonActivatedUser(existingUser);
            if (!removed) {
                throw new EmailAlreadyUsedException();
            }
        });

        User newUser = new User();
        String encryptedPassword = passwordEncoder.encode(doctorVM.getPassword());
        newUser.setLogin(doctorVM.getLogin().toLowerCase());
        newUser.setPassword(encryptedPassword);

        String[] names = doctorVM.getFullName() != null ? doctorVM.getFullName().trim().split("\\s+", 2)
                : new String[] { "" };
        newUser.setFirstName(names[0]);
        newUser.setLastName(names.length > 1 ? names[1] : "");
        newUser.setEmail(doctorVM.getEmail().toLowerCase());
        newUser.setLangKey(Constants.DEFAULT_LANGUAGE);

        // Generate 6-digit OTP for Email Activation
        String otp = String.format("%06d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 1000000));
        newUser.setActivated(false);
        newUser.setActivationKey(otp);
        newUser.setResetDate(Instant.now()); // Mark OTP created timestamp

        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
        authorityRepository.findById(AuthoritiesConstants.DOCTOR).ifPresent(authorities::add);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);

        com.ocbs.domain.DoctorProfile doctor = new com.ocbs.domain.DoctorProfile();
        doctor.setUser(newUser);
        doctor.setFullName(doctorVM.getFullName() != null && !doctorVM.getFullName().isBlank() ? doctorVM.getFullName()
                : doctorVM.getLogin());
        doctor.setPhone(doctorVM.getPhone() != null ? doctorVM.getPhone() : "");
        doctor.setTitle(doctorVM.getTitle() != null ? doctorVM.getTitle() : "Doctor");
        doctor.setBio(doctorVM.getBio());
        doctor.setExperienceYears(doctorVM.getExperienceYears() != null ? doctorVM.getExperienceYears() : 0);
        doctor.setConsultationFee(doctorVM.getConsultationFee() != null ? doctorVM.getConsultationFee()
                : java.math.BigDecimal.valueOf(50));
        doctor.setIsActive(true);

        if (doctorVM.getSpecializationIds() != null && !doctorVM.getSpecializationIds().isEmpty()) {
            Set<com.ocbs.domain.Specialization> specs = new HashSet<>(
                    specializationRepository.findAllById(doctorVM.getSpecializationIds()));
            doctor.setSpecializationses(specs);
        }
        doctorProfileRepository.save(doctor);

        LOG.debug("Created Doctor profile and Information for User: {}", newUser);
        return newUser;
    }

    public enum OtpVerificationResult {
        SUCCESS,
        USER_NOT_FOUND,
        ALREADY_ACTIVATED,
        OTP_EXPIRED,
        OTP_INVALID
    }

    public OtpVerificationResult verifyOtpStatus(String emailOrLogin, String otp) {
        LOG.debug("Verifying OTP '{}' for user/email '{}'", otp, emailOrLogin);
        if (emailOrLogin == null || emailOrLogin.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
            return OtpVerificationResult.OTP_INVALID;
        }

        String searchKey = emailOrLogin.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findOneByEmailIgnoreCase(searchKey);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findOneByLogin(searchKey);
        }

        if (userOpt.isEmpty()) {
            return OtpVerificationResult.USER_NOT_FOUND;
        }

        User user = userOpt.get();
        if (user.isActivated()) {
            return OtpVerificationResult.ALREADY_ACTIVATED;
        }

        // Check 90 seconds expiration (1 minute 30 seconds)
        Instant otpCreatedTime = user.getResetDate();
        // if (otpCreatedTime == null || otpCreatedTime.isBefore(Instant.now().minus(90,
        // ChronoUnit.SECONDS))) {
        // return OtpVerificationResult.OTP_EXPIRED;
        // }
        if (otpCreatedTime.isBefore(Instant.now().minus(90, ChronoUnit.SECONDS))) {
            return OtpVerificationResult.OTP_EXPIRED;
        }
        String storedKey = user.getActivationKey();
        if (storedKey != null && storedKey.trim().equals(otp.trim())) {
            user.setActivated(true);
            user.setActivationKey(null);
            user.setResetDate(null);
            userRepository.save(user);
            LOG.info("User '{}' ({}) successfully activated via OTP!", user.getLogin(), user.getEmail());
            return OtpVerificationResult.SUCCESS;
        }

        return OtpVerificationResult.OTP_INVALID;
    }

    public Optional<User> verifyOtp(String emailOrLogin, String otp) {
        if (verifyOtpStatus(emailOrLogin, otp) == OtpVerificationResult.SUCCESS) {
            String searchKey = emailOrLogin.trim().toLowerCase();
            Optional<User> u = userRepository.findOneByEmailIgnoreCase(searchKey);
            return u.isPresent() ? u : userRepository.findOneByLogin(searchKey);
        }
        return Optional.empty();
    }

    public Optional<User> resendOtp(String emailOrLogin) {
        LOG.debug("Resending OTP for user/email '{}'", emailOrLogin);
        if (emailOrLogin == null || emailOrLogin.trim().isEmpty()) {
            return Optional.empty();
        }

        String searchKey = emailOrLogin.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findOneByEmailIgnoreCase(searchKey);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findOneByLogin(searchKey);
        }

        return userOpt.map(user -> {
            if (!user.isActivated()) {
                String newOtp = String.format("%06d",
                        java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 1000000));
                user.setActivationKey(newOtp);
                user.setResetDate(Instant.now()); // Reset 90-second window
                userRepository.save(user);
                mailService.sendActivationOtpEmail(user, newOtp);
                LOG.info("New OTP '{}' generated and sent for user '{}'", newOtp, user.getLogin());
            }
            return user;
        });
    }

    public User loginOrRegisterWithGoogle(String email, String name, String picture, String googleId) {
        LOG.debug("Authenticating with Google for email: {}", email);
        String cleanEmail = email.toLowerCase().trim();
        Optional<User> existingUserOpt = userRepository.findOneByEmailIgnoreCase(cleanEmail);

        if (existingUserOpt.isPresent()) {
            User user = existingUserOpt.get();
            if (!user.isActivated()) {
                user.setActivated(true);
                user.setActivationKey(null);
            }
            if (picture != null && !picture.isBlank() && (user.getImageUrl() == null || user.getImageUrl().isBlank())) {
                user.setImageUrl(picture);
            }
            return userRepository.save(user);
        }

        // Tạo tài khoản mới từ Google
        User newUser = new User();
        String login = cleanEmail.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (login.length() < 3) {
            login = "user" + UUID.randomUUID().toString().substring(0, 5);
        }
        // Đảm bảo login không bị trùng
        if (userRepository.findOneByLogin(login).isPresent()) {
            login = login + UUID.randomUUID().toString().substring(0, 4);
        }

        newUser.setLogin(login);
        newUser.setEmail(cleanEmail);
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Ngẫu nhiên mật khẩu

        String[] nameParts = name != null ? name.trim().split("\\s+", 2) : new String[] { "Google", "User" };
        newUser.setFirstName(nameParts[0]);
        newUser.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        newUser.setImageUrl(picture);
        newUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        newUser.setActivated(true);

        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
        authorityRepository.findById(AuthoritiesConstants.PATIENT).ifPresent(authorities::add);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);

        // Tạo PatientProfile
        com.ocbs.domain.PatientProfile patientProfile = new com.ocbs.domain.PatientProfile();
        patientProfile.setFullName(name != null && !name.isBlank() ? name : login);
        patientProfile.setPhone("N/A");
        patientProfile.setUser(newUser);
        patientProfileRepository.save(patientProfile);

        LOG.info("Successfully registered new user '{}' via Google Sign-In", login);
        return newUser;
    }

    private boolean removeNonActivatedUser(User existingUser) {
        if (existingUser.isActivated()) {
            return false;
        }
        userRepository.delete(existingUser);
        userRepository.flush();
        return true;
    }

    public User createUser(AdminUserDTO userDTO) {
        User user = new User();
        user.setLogin(userDTO.getLogin().toLowerCase());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail().toLowerCase());
        }
        user.setImageUrl(userDTO.getImageUrl());
        if (userDTO.getLangKey() == null) {
            user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
        } else {
            user.setLangKey(userDTO.getLangKey());
        }
        String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        user.setActivated(true);
        if (userDTO.getAuthorities() != null) {
            Set<Authority> authorities = userDTO
                    .getAuthorities()
                    .stream()
                    .map(authorityRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            user.setAuthorities(authorities);
        }
        userRepository.save(user);
        LOG.debug("Created Information for User: {}", user);
        return user;
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update.
     * @return updated user.
     */
    public Optional<AdminUserDTO> updateUser(AdminUserDTO userDTO) {
        return Optional.of(userRepository.findById(userDTO.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(user -> {
                    user.setLogin(userDTO.getLogin().toLowerCase());
                    user.setFirstName(userDTO.getFirstName());
                    user.setLastName(userDTO.getLastName());
                    if (userDTO.getEmail() != null) {
                        user.setEmail(userDTO.getEmail().toLowerCase());
                    }
                    user.setImageUrl(userDTO.getImageUrl());
                    user.setActivated(userDTO.isActivated());
                    user.setLangKey(userDTO.getLangKey());
                    Set<Authority> managedAuthorities = user.getAuthorities();
                    managedAuthorities.clear();
                    userDTO
                            .getAuthorities()
                            .stream()
                            .map(authorityRepository::findById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .forEach(managedAuthorities::add);
                    userRepository.save(user);
                    LOG.debug("Changed Information for User: {}", user);
                    return user;
                })
                .map(AdminUserDTO::new);
    }

    public void deleteUser(String login) {
        userRepository.findOneByLogin(login).ifPresent(user -> {
            patientProfileRepository.findByUser(user).ifPresent(patientProfileRepository::delete);
            doctorProfileRepository.findByUser(user).ifPresent(doctorProfileRepository::delete);
            userRepository.delete(user);
            LOG.debug("Deleted User: {}", user);
        });
    }

    /**
     * Update basic information (first name, last name, email, language) for the
     * current user.
     *
     * @param firstName first name of user.
     * @param lastName  last name of user.
     * @param email     email of user.
     * @param langKey   language key.
     * @param imageUrl  image URL of user.
     */
    public void updateUser(String firstName, String lastName, String email, String langKey, String imageUrl) {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .ifPresent(user -> {
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    if (email != null) {
                        user.setEmail(email.toLowerCase());
                    }
                    user.setLangKey(langKey);
                    user.setImageUrl(imageUrl);
                    userRepository.save(user);
                    LOG.debug("Changed Information for User: {}", user);
                });
    }

    @Transactional
    public void changePassword(String currentClearTextPassword, String newPassword) {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .ifPresent(user -> {
                    String currentEncryptedPassword = user.getPassword();
                    if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                        throw new InvalidPasswordException();
                    }
                    String encryptedPassword = passwordEncoder.encode(newPassword);
                    user.setPassword(encryptedPassword);
                    LOG.debug("Changed password for User: {}", user);
                });
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllPublicUsers(Pageable pageable) {
        return userRepository.findAllByIdNotNullAndActivatedIsTrue(pageable).map(UserDTO::new);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByLogin(login);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin);
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired every day, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userRepository
                .findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
                        Instant.now().minus(3, ChronoUnit.DAYS))
                .forEach(user -> {
                    LOG.debug("Deleting not activated user {}", user.getLogin());
                    patientProfileRepository.findByUser(user).ifPresent(patientProfileRepository::delete);
                    doctorProfileRepository.findByUser(user).ifPresent(doctorProfileRepository::delete);
                    userRepository.delete(user);
                });
    }

    /**
     * Gets a list of all the authorities.
     * 
     * @return a list of all the authorities.
     */
    @Transactional(readOnly = true)
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).toList();
    }

    /**
     * Get the current user's profile with detailed patient or doctor information.
     */
    @Transactional(readOnly = true)
    public Optional<com.ocbs.service.dto.UserProfileDTO> getCurrentUserProfile() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneWithAuthoritiesByLogin)
                .map(user -> {
                    com.ocbs.service.dto.UserProfileDTO dto = new com.ocbs.service.dto.UserProfileDTO();
                    dto.setUserId(user.getId());
                    dto.setLogin(user.getLogin());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    dto.setEmail(user.getEmail());
                    dto.setImageUrl(user.getImageUrl());
                    dto.setActivated(user.isActivated());
                    dto.setLangKey(user.getLangKey());

                    Set<String> authNames = user.getAuthorities().stream().map(Authority::getName)
                            .collect(Collectors.toSet());
                    dto.setAuthorities(authNames);

                    if (authNames.contains(AuthoritiesConstants.ADMIN)) {
                        dto.setPrimaryRole("ADMIN");
                    } else if (authNames.contains(AuthoritiesConstants.DOCTOR)) {
                        dto.setPrimaryRole("DOCTOR");
                    } else if (authNames.contains(AuthoritiesConstants.PATIENT)) {
                        dto.setPrimaryRole("PATIENT");
                    } else {
                        dto.setPrimaryRole("USER");
                    }

                    patientProfileRepository.findByUser_Login(user.getLogin()).ifPresent(patient -> {
                        dto.setPatientProfile(patientProfileMapper.toDto(patient));
                    });

                    doctorProfileRepository.findByUser_Login(user.getLogin()).ifPresent(doctor -> {
                        dto.setDoctorProfile(doctorProfileMapper.toDto(doctor));
                    });

                    return dto;
                });
    }

    /**
     * Updates the current user's profile information.
     */
    public com.ocbs.service.dto.UserProfileDTO updateCurrentUserProfile(
            com.ocbs.service.dto.UserProfileDTO profileDTO) {
        String login = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Current user login not found"));

        User user = userRepository.findOneWithAuthoritiesByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found for login: " + login));

        if (profileDTO.getFirstName() != null)
            user.setFirstName(profileDTO.getFirstName());
        if (profileDTO.getLastName() != null)
            user.setLastName(profileDTO.getLastName());
        if (profileDTO.getEmail() != null)
            user.setEmail(profileDTO.getEmail().toLowerCase());
        if (profileDTO.getImageUrl() != null)
            user.setImageUrl(profileDTO.getImageUrl());
        userRepository.save(user);

        if (profileDTO.getPatientProfile() != null) {
            com.ocbs.domain.PatientProfile patient = patientProfileRepository.findByUser_Login(login)
                    .orElseGet(() -> {
                        com.ocbs.domain.PatientProfile p = new com.ocbs.domain.PatientProfile();
                        p.setUser(user);
                        return p;
                    });
            if (profileDTO.getPatientProfile().getFullName() != null) {
                patient.setFullName(profileDTO.getPatientProfile().getFullName());
            }
            if (profileDTO.getPatientProfile().getPhone() != null) {
                patient.setPhone(profileDTO.getPatientProfile().getPhone());
            }
            patient.setDateOfBirth(profileDTO.getPatientProfile().getDateOfBirth());
            patient.setGender(profileDTO.getPatientProfile().getGender());
            patient.setAddress(profileDTO.getPatientProfile().getAddress());
            patient.setMedicalHistory(profileDTO.getPatientProfile().getMedicalHistory());
            patientProfileRepository.save(patient);
        }

        if (profileDTO.getDoctorProfile() != null
                && user.getAuthorities().stream().anyMatch(a -> a.getName().equals(AuthoritiesConstants.DOCTOR))) {
            doctorProfileRepository.findByUser_Login(login).ifPresent(doctor -> {
                if (profileDTO.getDoctorProfile().getFullName() != null)
                    doctor.setFullName(profileDTO.getDoctorProfile().getFullName());
                if (profileDTO.getDoctorProfile().getTitle() != null)
                    doctor.setTitle(profileDTO.getDoctorProfile().getTitle());
                doctor.setBio(profileDTO.getDoctorProfile().getBio());
                doctor.setExperienceYears(profileDTO.getDoctorProfile().getExperienceYears());
                doctor.setConsultationFee(profileDTO.getDoctorProfile().getConsultationFee());
                doctor.setPhone(profileDTO.getDoctorProfile().getPhone());
                doctor.setGender(profileDTO.getDoctorProfile().getGender());
                if (profileDTO.getDoctorProfile().getIsActive() != null)
                    doctor.setIsActive(profileDTO.getDoctorProfile().getIsActive());
                doctorProfileRepository.save(doctor);
            });
        }

        return getCurrentUserProfile().orElse(null);
    }
}
