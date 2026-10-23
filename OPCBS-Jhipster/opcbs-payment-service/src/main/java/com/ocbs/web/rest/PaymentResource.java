package com.ocbs.web.rest;

import com.ocbs.domain.DoctorSubscription;
import com.ocbs.domain.PaymentTransaction;
import com.ocbs.domain.ServicePackage;
import com.ocbs.repository.DoctorSubscriptionRepository;
import com.ocbs.repository.PaymentTransactionRepository;
import com.ocbs.repository.ServicePackageRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.util.VnPayUtil;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Transactional
public class PaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentResource.class);
    private static final String ENTITY_NAME = "payment";

    private final DoctorSubscriptionRepository subscriptionRepository;
    private final ServicePackageRepository packageRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final org.springframework.core.env.Environment env;
    private final ServicePackageResource servicePackageResource;
    private final com.ocbs.event.PaymentEventProducer paymentEventProducer;

    @org.springframework.beans.factory.annotation.Value("${application.vnpay.tmn-code:OPCBSDEMO}")
    private String vnpTmnCode;

    @org.springframework.beans.factory.annotation.Value("${application.vnpay.hash-secret:OPCBSVNPAYHASHSECRETDEMO202610}")
    private String vnpHashSecret;

    @org.springframework.beans.factory.annotation.Value("${application.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @org.springframework.beans.factory.annotation.Value("${application.payment.mock-enabled:false}")
    private boolean mockPaymentEnabled;

    public PaymentResource(
        DoctorSubscriptionRepository subscriptionRepository,
        ServicePackageRepository packageRepository,
        PaymentTransactionRepository transactionRepository,
        org.springframework.core.env.Environment env,
        @org.springframework.context.annotation.Lazy ServicePackageResource servicePackageResource,
        com.ocbs.event.PaymentEventProducer paymentEventProducer
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.packageRepository = packageRepository;
        this.transactionRepository = transactionRepository;
        this.env = env;
        this.servicePackageResource = servicePackageResource;
        this.paymentEventProducer = paymentEventProducer;
    }

    /**
     * Chỉ cho phép mock checkout khi được bật tường minh và VNPay URL trỏ đúng HTTPS sandbox.
     * Ràng buộc theo gateway thay cho profile để demo sandbox có thể chạy bằng Compose prod profile.
     */
    private boolean isMockPaymentAllowed() {
        if (!mockPaymentEnabled) {
            return false;
        }
        try {
            java.net.URI payUri = java.net.URI.create(vnpPayUrl);
            if (!"https".equalsIgnoreCase(payUri.getScheme()) || !"sandbox.vnpayment.vn".equalsIgnoreCase(payUri.getHost())) {
                return false;
            }
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

    /**
     * Xác minh quyền sở hữu giao dịch:
     * - Bác sĩ hiện tại phải là chủ sở hữu của giao dịch (khớp doctorLogin).
     * - ROLE_ADMIN được phép thao tác nhưng phải có log audit bảo mật riêng biệt.
     * - Trả về lỗi an toàn txnotfound đồng nhất để chống rò rỉ sự tồn tại của giao dịch của người khác.
     */
    private void validateTransactionOwnership(PaymentTransaction txn) {
        String currentLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));
        boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN);
        if (isAdmin) {
            if (txn.getDoctorLogin() != null && !txn.getDoctorLogin().equals(currentLogin)) {
                LOG.info("🛡️ [ADMIN AUDIT] Administrator '{}' thao tác trên giao dịch '{}' của bác sĩ '{}'",
                    currentLogin, txn.getTransactionCode(), txn.getDoctorLogin());
            }
            return;
        }
        if (txn.getDoctorLogin() == null || !txn.getDoctorLogin().equals(currentLogin)) {
            LOG.warn("🚨 [Ownership Violation] Người dùng '{}' cố ý thao tác trên giao dịch '{}' thuộc về bác sĩ '{}'",
                currentLogin, txn.getTransactionCode(), txn.getDoctorLogin());
            throw new BadRequestAlertException("Giao dịch không tồn tại hoặc không hợp lệ", ENTITY_NAME, "txnotfound");
        }
    }

    /**
     * Xác minh quyền sở hữu gói đăng ký Subscription:
     * - Khớp doctorLogin của bác sĩ hiện tại.
     * - ROLE_ADMIN có audit trail.
     * - Trả về lỗi an toàn subnotfound đồng nhất.
     */
    private void validateSubscriptionOwnership(DoctorSubscription sub) {
        String currentLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));
        boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN);
        if (isAdmin) {
            if (sub.getDoctorLogin() != null && !sub.getDoctorLogin().equals(currentLogin)) {
                LOG.info("🛡️ [ADMIN AUDIT] Administrator '{}' thao tác trên subscription #{} của bác sĩ '{}'",
                    currentLogin, sub.getId(), sub.getDoctorLogin());
            }
            return;
        }
        if (sub.getDoctorLogin() == null || !sub.getDoctorLogin().equals(currentLogin)) {
            LOG.warn("🚨 [Ownership Violation] Người dùng '{}' cố ý thao tác trên subscription #{} thuộc về bác sĩ '{}'",
                currentLogin, sub.getId(), sub.getDoctorLogin());
            throw new BadRequestAlertException("Gói đăng ký không tồn tại hoặc không hợp lệ", ENTITY_NAME, "subnotfound");
        }
    }

    /**
     * {@code GET /doctor/my-subscription} : Get current doctor's active subscription.
     */
    @GetMapping("/doctor/my-subscription")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DoctorSubscription> getMySubscription() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorSubscription> sub = subscriptionRepository.findFirstByDoctorLoginAndStatusOrderByCreatedDateDesc(login, "ACTIVE");
        return sub.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * {@code GET /doctor/my-subscription/quota} : Get current doctor's active subscription benefits, quotas and usage.
     */
    @GetMapping("/doctor/my-subscription/quota")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getMySubscriptionQuota() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorSubscription> activeSubOpt = subscriptionRepository
            .findFirstByDoctorLoginAndStatusOrderByCreatedDateDesc(login, "ACTIVE");

        Instant now = Instant.now();
        if (activeSubOpt.isEmpty() || activeSubOpt.get().getExpirationDate() == null || activeSubOpt.get().getExpirationDate().isBefore(now)) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("hasActiveSubscription", false);
            resp.put("message", "Quý Bác sĩ chưa kích hoạt gói dịch vụ hoặc gói dịch vụ đã hết hạn");
            return ResponseEntity.ok(resp);
        }

        DoctorSubscription sub = activeSubOpt.get();
        ServicePackage pkg = packageRepository.findById(sub.getServicePackageId()).orElse(null);

        long daysRemaining = ChronoUnit.DAYS.between(now, sub.getExpirationDate());
        if (daysRemaining < 0) daysRemaining = 0;

        Map<String, Object> resp = new HashMap<>();
        resp.put("hasActiveSubscription", true);
        resp.put("subscriptionId", sub.getId());
        resp.put("packageName", sub.getServicePackageName());
        resp.put("packageId", sub.getServicePackageId());
        resp.put("startDate", sub.getStartDate() != null ? sub.getStartDate().toString() : "");
        resp.put("expirationDate", sub.getExpirationDate() != null ? sub.getExpirationDate().toString() : "");
        resp.put("daysRemaining", daysRemaining);
        resp.put("maxPatientCapacity", pkg != null ? pkg.getMaxPatientCapacity() : null);
        resp.put("maxDailySlotsCapacity", pkg != null ? pkg.getMaxDailySlotsCapacity() : null);
        resp.put("durationDays", pkg != null ? pkg.getDurationDays() : null);
        resp.put("price", sub.getPrice());

        return ResponseEntity.ok(resp);
    }

    /**
     * {@code GET /doctor/subscriptions/history} : Get all subscription history of current doctor.
     */
    @GetMapping("/doctor/subscriptions/history")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DoctorSubscription>> getMySubscriptionHistory() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        List<DoctorSubscription> history = subscriptionRepository.findAllByDoctorLoginOrderByCreatedDateDesc(login);
        return ResponseEntity.ok(history);
    }

    /**
     * {@code POST /doctor/subscriptions/renew} : Renew current active/expired subscription package.
     */
    @PostMapping("/doctor/subscriptions/renew")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> renewSubscription(@RequestParam(name = "packageId", required = false) Long packageId) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorSubscription> activeSubOpt = subscriptionRepository.findFirstByDoctorLoginAndStatusOrderByCreatedDateDesc(login, "ACTIVE");
        DoctorSubscription currentSub = activeSubOpt.orElse(null);

        Long targetPackageId = packageId;
        if (targetPackageId == null && currentSub != null) {
            targetPackageId = currentSub.getServicePackageId();
        }
        if (targetPackageId == null) {
            throw new BadRequestAlertException("Vui lòng chọn gói dịch vụ cần gia hạn", ENTITY_NAME, "packagenotfound");
        }

        ServicePackage pkg = packageRepository.findById(targetPackageId)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy gói dịch vụ", ENTITY_NAME, "packagenotfound"));

        if (!Boolean.TRUE.equals(pkg.getIsActive())) {
            throw new BadRequestAlertException("Gói dịch vụ đã bị vô hiệu hóa, không thể gia hạn", ENTITY_NAME, "packageinactive");
        }

        String txnCode = "VNP_RNW" + System.currentTimeMillis() + (int)(Math.random() * 1000);

        DoctorSubscription sub = new DoctorSubscription();
        sub.setDoctorLogin(login);
        sub.setServicePackageId(pkg.getId());
        sub.setServicePackageName(pkg.getName());
        sub.setPrice(pkg.getPrice());
        sub.setStatus("PENDING_PAYMENT");
        sub.setTransactionCode(txnCode);
        DoctorSubscription savedSub = subscriptionRepository.save(sub);

        PaymentTransaction txn = new PaymentTransaction();
        txn.setSubscriptionId(savedSub.getId());
        txn.setTransactionCode(txnCode);
        txn.setDoctorId(savedSub.getDoctorId());
        txn.setDoctorLogin(login);
        txn.setAmount(pkg.getPrice());
        txn.setPaymentStatus("PENDING");
        txn.setPaymentMethod("VNPAY");
        transactionRepository.save(txn);

        LOG.info("🔄 [Payment Service] Created Renewal Subscription #{} for Doctor '{}', Package: '{}'",
            savedSub.getId(), login, pkg.getName());

        return ResponseEntity.ok(Map.of(
            "subscriptionId", savedSub.getId(),
            "transactionCode", txnCode,
            "amount", pkg.getPrice(),
            "packageName", pkg.getName(),
            "status", "PENDING_PAYMENT",
            "isRenewal", true
        ));
    }

    /**
     * {@code POST /doctor/subscriptions/subscribe} : Doctor selects a platform service package.
     */
    @PostMapping("/doctor/subscriptions/subscribe")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> subscribePackage(@RequestParam("packageId") Long packageId) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        ServicePackage pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy gói dịch vụ", ENTITY_NAME, "packagenotfound"));

        if (!Boolean.TRUE.equals(pkg.getIsActive())) {
            throw new BadRequestAlertException("Gói dịch vụ đã bị vô hiệu hóa hoặc không còn áp dụng", ENTITY_NAME, "packageinactive");
        }

        String txnCode = "VNP" + System.currentTimeMillis() + (int)(Math.random() * 1000);

        DoctorSubscription sub = new DoctorSubscription();
        sub.setDoctorLogin(login);
        sub.setServicePackageId(pkg.getId());
        sub.setServicePackageName(pkg.getName());
        sub.setPrice(pkg.getPrice());
        sub.setStatus("PENDING_PAYMENT");
        sub.setTransactionCode(txnCode);
        DoctorSubscription savedSub = subscriptionRepository.save(sub);

        PaymentTransaction txn = new PaymentTransaction();
        txn.setSubscriptionId(savedSub.getId());
        txn.setTransactionCode(txnCode);
        txn.setDoctorId(savedSub.getDoctorId());
        txn.setDoctorLogin(login);
        txn.setAmount(pkg.getPrice());
        txn.setPaymentStatus("PENDING");
        txn.setPaymentMethod("VNPAY");
        transactionRepository.save(txn);

        LOG.info("📦 [Payment Service] Created Subscription #{} with TransactionCode: {} for doctor: {}", savedSub.getId(), txnCode, login);

        return ResponseEntity.ok(Map.of(
            "subscriptionId", savedSub.getId(),
            "transactionCode", txnCode,
            "amount", pkg.getPrice(),
            "packageName", pkg.getName(),
            "status", "PENDING_PAYMENT"
        ));
    }

    /**
     * {@code POST /payments/vnpay/create-url} : Build VNPay Sandbox checkout URL with HMAC-SHA512 signature.
     */
    @PostMapping("/payments/vnpay/create-url")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> createVnPayUrl(
        @RequestParam("transactionCode") String transactionCode,
        @RequestParam(name = "returnUrl", defaultValue = "http://localhost:9000/doctor/subscription/callback") String returnUrl
    ) {
        PaymentTransaction txn = transactionRepository.findByTransactionCode(transactionCode)
            .orElseThrow(() -> new BadRequestAlertException("Giao dịch không tồn tại hoặc không hợp lệ", ENTITY_NAME, "txnotfound"));

        // Kiểm tra quyền sở hữu giao dịch bảo mật
        validateTransactionOwnership(txn);

        long amountInVnd = txn.getAmount().multiply(new BigDecimal(100)).longValue();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String createDate = formatter.format(new Date());

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VnPayUtil.VNP_VERSION);
        vnp_Params.put("vnp_Command", VnPayUtil.VNP_COMMAND);
        vnp_Params.put("vnp_TmnCode", (vnpTmnCode != null && !vnpTmnCode.isBlank()) ? vnpTmnCode : VnPayUtil.DEFAULT_TMN_CODE);
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVnd));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", transactionCode);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan goi dich vu MindBridge - Ma: " + transactionCode);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");
        vnp_Params.put("vnp_CreateDate", createDate);

        // Compute HMAC-SHA512 hash using configured secret
        String secureHash = VnPayUtil.hashAllFields(vnp_Params, vnpHashSecret);
        vnp_Params.put("vnp_SecureHash", secureHash);

        // Build query string
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String val = vnp_Params.get(fieldName);
            if (val != null && !val.isEmpty()) {
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                     .append("=")
                     .append(URLEncoder.encode(val, StandardCharsets.US_ASCII))
                     .append("&");
            }
        }
        String basePayUrl = (vnpPayUrl != null && !vnpPayUrl.isBlank()) ? vnpPayUrl : VnPayUtil.DEFAULT_PAY_URL;
        String paymentUrl = basePayUrl + "?" + query.substring(0, query.length() - 1);

        // Persist generated payment URL on transaction
        txn.setPaymentUrl(paymentUrl);
        transactionRepository.save(txn);

        LOG.info("🔗 [VNPay Gateway] Generated Payment URL for TxnRef: {}", transactionCode);
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl, "transactionCode", transactionCode));
    }

    /**
     * {@code GET /payments/vnpay/callback} : VNPay IPN / Return Callback with IDEMPOTENCY & TRANSACTIONAL GUARD.
     */
    @GetMapping("/payments/vnpay/callback")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<Map<String, Object>> vnpayCallback(@RequestParam Map<String, String> allParams) {
        Map<String, String> safeAuditParams = new HashMap<>(allParams);
        safeAuditParams.remove("vnp_SecureHash");
        safeAuditParams.remove("vnp_SecureHashType");
        LOG.info("📥 [VNPay Callback / IPN AUDIT] Received callback params: {}", safeAuditParams);

        String transactionCode = allParams.get("vnp_TxnRef");
        String responseCode = allParams.get("vnp_ResponseCode");

        if (transactionCode == null || transactionCode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("RspCode", "99", "Message", "Missing vnp_TxnRef"));
        }

        // 1. Kiểm tra Chữ ký bảo mật HMAC-SHA512
        boolean isValidSignature = VnPayUtil.verifyCallbackSignature(allParams, vnpHashSecret);
        if (!isValidSignature) {
            LOG.error("❌ [VNPay Callback] Invalid Secure Hash Signature for transaction: {}", transactionCode);
            return ResponseEntity.badRequest().body(Map.of("RspCode", "97", "Message", "Invalid Checksum Signature"));
        }

        // 2. Kiểm tra Mã đối tác (Merchant Code)
        String expectedTmnCode = (vnpTmnCode != null && !vnpTmnCode.isBlank()) ? vnpTmnCode : VnPayUtil.DEFAULT_TMN_CODE;
        String callbackTmnCode = allParams.get("vnp_TmnCode");
        if (callbackTmnCode == null || !callbackTmnCode.equalsIgnoreCase(expectedTmnCode)) {
            LOG.error("❌ [VNPay Callback] Merchant code mismatch: expected '{}', got '{}'", expectedTmnCode, callbackTmnCode);
            return ResponseEntity.badRequest().body(Map.of("RspCode", "98", "Message", "Invalid Merchant Code (vnp_TmnCode)"));
        }

        // 3. Lấy Transaction với PESSIMISTIC LOCK (Khóa Bi Quan) để chống Race Condition khi có nhiều Webhook retry đồng thời
        PaymentTransaction txn = transactionRepository.findByTransactionCodeWithLock(transactionCode)
            .orElseThrow(() -> new BadRequestAlertException("Giao dịch không tồn tại hoặc không hợp lệ", ENTITY_NAME, "txnotfound"));

        DoctorSubscription sub = subscriptionRepository.findByIdWithLock(txn.getSubscriptionId())
            .orElseGet(() -> subscriptionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new BadRequestAlertException("Gói đăng ký không tồn tại hoặc không hợp lệ", ENTITY_NAME, "subnotfound")));

        // Kiểm tra quyền sở hữu nếu gọi từ trình duyệt bác sĩ đang đăng nhập
        if (SecurityUtils.getCurrentUserLogin().isPresent()) {
            validateTransactionOwnership(txn);
            validateSubscriptionOwnership(sub);
        }

        // 4. Kiểm tra Số tiền thanh toán (Amount Verification)
        String vnpAmountStr = allParams.get("vnp_Amount");
        if (vnpAmountStr == null || vnpAmountStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("RspCode", "99", "Message", "Missing vnp_Amount"));
        }
        try {
            long callbackAmount = Long.parseLong(vnpAmountStr) / 100;
            long expectedAmount = txn.getAmount().longValue();
            if (callbackAmount != expectedAmount) {
                LOG.error("❌ [VNPay Callback] Amount mismatch: expected {}, got {}", expectedAmount, callbackAmount);
                return ResponseEntity.badRequest().body(Map.of("RspCode", "04", "Message", "Invalid Amount"));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("RspCode", "99", "Message", "Invalid vnp_Amount format"));
        }

        // 5. IDEMPOTENCY GUARD (Tính lũy đẳng): Nếu giao dịch ĐÃ THÀNH CÔNG từ trước, trả về kết quả ngay mà không cộng thêm ngày!
        if ("SUCCESS".equalsIgnoreCase(txn.getPaymentStatus())) {
            LOG.warn("⚡ [IDEMPOTENCY GUARD] Giao dịch '{}' đã được xử lý THÀNH CÔNG trước đó. Trả về Idempotent response mà KHÔNG cộng trùng thời hạn!", transactionCode);
            return ResponseEntity.ok(Map.of(
                "RspCode", "00",
                "Message", "Confirm Success (Idempotent Replay)",
                "isIdempotentReplay", true,
                "status", "ACTIVE",
                "transactionCode", transactionCode,
                "startDate", sub.getStartDate() != null ? sub.getStartDate().toString() : "",
                "expirationDate", sub.getExpirationDate() != null ? sub.getExpirationDate().toString() : ""
            ));
        }

        // 6. Xử lý Trạng thái Thanh toán từ VNPay
        Instant now = Instant.now();
        String vnpTransactionNo = allParams.get("vnp_TransactionNo");
        if (vnpTransactionNo != null && !vnpTransactionNo.isBlank()) {
            txn.setVnpTransactionNo(vnpTransactionNo);
        }
        if ("00".equals(responseCode)) {
            // Xác minh gói dịch vụ còn hiệu lực
            ServicePackage pkg = packageRepository.findById(sub.getServicePackageId())
                .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy gói dịch vụ tương ứng", ENTITY_NAME, "packagenotfound"));

            if (!Boolean.TRUE.equals(pkg.getIsActive())) {
                LOG.error("❌ [VNPay Callback] Service package #{} is disabled!", pkg.getId());
                throw new BadRequestAlertException("Gói dịch vụ đã bị vô hiệu hóa", ENTITY_NAME, "packageinactive");
            }

            int duration = pkg.getDurationDays();

            // Thanh toán thành công
            txn.setPaymentStatus("SUCCESS");
            txn.setPaidAt(now);
            txn.setResponseCode(responseCode);
            txn.setResponseMessage("Giao dich thanh toan thanh cong qua VNPay Sandbox");
            transactionRepository.save(txn);

            Instant newExpirationDate = calculateNewExpirationDate(sub.getDoctorLogin(), sub.getId(), duration, now);

            sub.setStatus("ACTIVE");
            sub.setStartDate(now);
            sub.setExpirationDate(newExpirationDate);
            subscriptionRepository.save(sub);

            if (servicePackageResource != null) {
                servicePackageResource.evictAdminDashboardCache();
            }

            LOG.info("🎉 [VNPay Success] Kích hoạt thành công gói '{}' cho bác sĩ '{}' (Hạn dùng: {} ngày, Đến: {})",
                sub.getServicePackageName(), sub.getDoctorLogin(), duration, sub.getExpirationDate());

            // Phát sự kiện thanh toán thành công qua Broker/Event dispatcher
            paymentEventProducer.publishPaymentEvent(new com.ocbs.event.PaymentEvent(
                transactionCode,
                sub.getId(),
                sub.getDoctorLogin(),
                pkg.getId(),
                pkg.getName(),
                txn.getAmount(),
                "SUCCESS",
                now,
                sub.getExpirationDate(),
                duration
            ));

            return ResponseEntity.ok(Map.of(
                "RspCode", "00",
                "Message", "Confirm Success",
                "isIdempotentReplay", false,
                "status", "ACTIVE",
                "transactionCode", transactionCode,
                "startDate", now.toString(),
                "expirationDate", sub.getExpirationDate().toString()
            ));
        } else {
            // Thanh toán thất bại hoặc hủy
            txn.setPaymentStatus("FAILED");
            txn.setResponseCode(responseCode);
            txn.setResponseMessage("Giao dich khong thanh cong, ma loi: " + responseCode);
            transactionRepository.save(txn);

            sub.setStatus("PAYMENT_FAILED");
            subscriptionRepository.save(sub);

            LOG.warn("⚠️ [VNPay Failed] Thanh toán thất bại cho giao dịch '{}', Mã lỗi: {}", transactionCode, responseCode);

            // Phát sự kiện thanh toán thất bại
            paymentEventProducer.publishPaymentEvent(new com.ocbs.event.PaymentEvent(
                transactionCode,
                sub.getId(),
                sub.getDoctorLogin(),
                sub.getServicePackageId(),
                sub.getServicePackageName(),
                txn.getAmount(),
                "FAILED",
                null,
                null,
                null
            ));

            return ResponseEntity.ok(Map.of(
                "RspCode", "01",
                "Message", "Payment Failed",
                "isIdempotentReplay", false,
                "status", "PAYMENT_FAILED",
                "transactionCode", transactionCode
            ));
        }
    }

    /**
     * {@code POST /payments/mock-checkout} : Fast 1-Click Sandbox Checkout WITH IDEMPOTENCY & TRANSACTIONAL GUARD.
     */
    @PostMapping("/payments/mock-checkout")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<Map<String, Object>> mockCheckout(@RequestParam("transactionCode") String transactionCode) {
        if (!isMockPaymentAllowed()) {
            LOG.error("🚨 [Security Violation] Cố gắng gọi Mock Checkout khi chế độ thanh toán giả lập bị tắt cho mã '{}'", transactionCode);
            throw new BadRequestAlertException("Mock payment is disabled in this environment", ENTITY_NAME, "mockpaymentdisabled");
        }

        // 1. Khóa giao dịch với Pessimistic Lock
        PaymentTransaction txn = transactionRepository.findByTransactionCodeWithLock(transactionCode)
            .orElseThrow(() -> new BadRequestAlertException("Giao dịch không tồn tại hoặc không hợp lệ", ENTITY_NAME, "txnotfound"));

        // 2. Xác minh quyền sở hữu giao dịch (chống dùng transactionCode của bác sĩ khác)
        validateTransactionOwnership(txn);

        // 3. Khóa Subscription tương ứng với Pessimistic Lock
        DoctorSubscription sub = subscriptionRepository.findByIdWithLock(txn.getSubscriptionId())
            .orElseGet(() -> subscriptionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new BadRequestAlertException("Gói đăng ký không tồn tại hoặc không hợp lệ", ENTITY_NAME, "subnotfound")));

        validateSubscriptionOwnership(sub);

        // 4. Xác minh quan hệ toàn vẹn giữa Transaction và Subscription
        if (!txn.getSubscriptionId().equals(sub.getId()) || !txn.getTransactionCode().equals(sub.getTransactionCode())) {
            LOG.error("🚨 [Integrity Violation] Transaction subscriptionId '{}' does not match Sub ID '{}'", txn.getSubscriptionId(), sub.getId());
            throw new BadRequestAlertException("Giao dịch không tồn tại hoặc không hợp lệ", ENTITY_NAME, "txnotfound");
        }

        // 5. Idempotency Guard: Nếu giao dịch đã THÀNH CÔNG từ trước, trả về kết quả ngay mà không cộng lặp ngày
        if ("SUCCESS".equalsIgnoreCase(txn.getPaymentStatus())) {
            LOG.warn("⚡ [IDEMPOTENCY GUARD] Mock Checkout gọi lặp lại cho mã '{}'. Trả về kết quả trước đó mà không cộng lặp ngày!", transactionCode);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "isIdempotentReplay", true,
                "status", "ACTIVE",
                "transactionCode", transactionCode,
                "message", "Giao dịch đã được thanh toán thành công trước đó (Idempotent Response).",
                "startDate", sub.getStartDate() != null ? sub.getStartDate().toString() : "",
                "expirationDate", sub.getExpirationDate() != null ? sub.getExpirationDate().toString() : ""
            ));
        }

        // 6. Xác minh giao dịch còn ở trạng thái được phép thanh toán (PENDING)
        if (!"PENDING".equalsIgnoreCase(txn.getPaymentStatus())) {
            LOG.warn("⚠️ [Invalid Payment Status] Giao dịch '{}' đang ở trạng thái '{}', không thể thanh toán", transactionCode, txn.getPaymentStatus());
            throw new BadRequestAlertException("Giao dịch không ở trạng thái chờ thanh toán", ENTITY_NAME, "invalidpaymentstatus");
        }

        if (!"PENDING_PAYMENT".equalsIgnoreCase(sub.getStatus())) {
            LOG.warn("⚠️ [Invalid Subscription Status] Gói đăng ký '{}' đang ở trạng thái '{}', không thể thanh toán", sub.getId(), sub.getStatus());
            throw new BadRequestAlertException("Gói dịch vụ không ở trạng thái chờ thanh toán", ENTITY_NAME, "invalidsubscriptionstatus");
        }

        // 7. Xác minh gói dịch vụ tồn tại và đang hoạt động (isActive)
        ServicePackage pkg = packageRepository.findById(sub.getServicePackageId())
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy gói dịch vụ", ENTITY_NAME, "packagenotfound"));

        if (!Boolean.TRUE.equals(pkg.getIsActive())) {
            LOG.error("🚨 [Security Violation] Cố gắng thanh toán gói dịch vụ đã bị vô hiệu hóa: ID {}", pkg.getId());
            throw new BadRequestAlertException("Gói dịch vụ này đã bị vô hiệu hóa bởi quản trị viên", ENTITY_NAME, "packageinactive");
        }

        // 8. Xác minh số tiền giao dịch khớp chính xác với giá niêm yết trong cơ sở dữ liệu
        if (txn.getAmount() == null || pkg.getPrice() == null || txn.getAmount().compareTo(pkg.getPrice()) != 0) {
            LOG.error("🚨 [Amount Mismatch] Số tiền giao dịch {} không khớp giá gói {}", txn.getAmount(), pkg.getPrice());
            throw new BadRequestAlertException("Số tiền thanh toán không khớp với giá niêm yết của gói dịch vụ", ENTITY_NAME, "amountmismatch");
        }

        int duration = pkg.getDurationDays();
        Instant now = Instant.now();

        // 9. Chỉ backend được phép cập nhật SUCCESS và kích hoạt subscription
        txn.setPaymentStatus("SUCCESS");
        txn.setPaidAt(now);
        txn.setResponseCode("00");
        txn.setResponseMessage("Giao dich thanh toan thanh cong (Mock Sandbox)");
        transactionRepository.save(txn);

        Instant newExpirationDate = calculateNewExpirationDate(sub.getDoctorLogin(), sub.getId(), duration, now);

        sub.setStatus("ACTIVE");
        sub.setStartDate(now);
        sub.setExpirationDate(newExpirationDate);
        subscriptionRepository.save(sub);

        if (servicePackageResource != null) {
            servicePackageResource.evictAdminDashboardCache();
        }

        LOG.info("🎉 [Payment Service] Mock Checkout FIRST RUN SUCCESS for Doctor '{}', Sub ID: {}, Package: '{}' (Hạn mới: {})",
            sub.getDoctorLogin(), sub.getId(), sub.getServicePackageName(), newExpirationDate);

        // Phát sự kiện thanh toán thành công qua Broker/Event dispatcher
        paymentEventProducer.publishPaymentEvent(new com.ocbs.event.PaymentEvent(
            transactionCode,
            sub.getId(),
            sub.getDoctorLogin(),
            pkg.getId(),
            pkg.getName(),
            txn.getAmount(),
            "SUCCESS",
            now,
            newExpirationDate,
            duration
        ));

        return ResponseEntity.ok(Map.of(
            "success", true,
            "isIdempotentReplay", false,
            "status", "ACTIVE",
            "transactionCode", transactionCode,
            "message", "Thanh toán gói dịch vụ thành công!",
            "startDate", now.toString(),
            "expirationDate", sub.getExpirationDate().toString()
        ));
    }

    private Instant calculateNewExpirationDate(String doctorLogin, Long currentSubId, int durationDays, Instant now) {
        List<DoctorSubscription> existingSubs = subscriptionRepository.findAllByDoctorLoginOrderByCreatedDateDesc(doctorLogin);
        for (DoctorSubscription s : existingSubs) {
            if (!s.getId().equals(currentSubId) && "ACTIVE".equalsIgnoreCase(s.getStatus()) && s.getExpirationDate() != null) {
                if (s.getExpirationDate().isAfter(now)) {
                    LOG.info("⚡ [Smart Renewal] Bác sĩ '{}' còn hạn đến {}. Tự động cộng dồn thêm {} ngày -> Hạn mới: {}",
                        doctorLogin, s.getExpirationDate(), durationDays, s.getExpirationDate().plus(durationDays, ChronoUnit.DAYS));
                    s.setStatus("EXTENDED");
                    subscriptionRepository.save(s);
                    return s.getExpirationDate().plus(durationDays, ChronoUnit.DAYS);
                }
            }
        }
        return now.plus(durationDays, ChronoUnit.DAYS);
    }
}
