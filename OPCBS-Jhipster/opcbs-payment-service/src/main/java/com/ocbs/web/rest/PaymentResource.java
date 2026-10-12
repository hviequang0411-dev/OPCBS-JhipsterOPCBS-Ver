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

    public PaymentResource(
        DoctorSubscriptionRepository subscriptionRepository,
        ServicePackageRepository packageRepository,
        PaymentTransactionRepository transactionRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.packageRepository = packageRepository;
        this.transactionRepository = transactionRepository;
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
     * {@code POST /doctor/subscriptions/subscribe} : Doctor selects a platform service package.
     */
    @PostMapping("/doctor/subscriptions/subscribe")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> subscribePackage(@RequestParam("packageId") Long packageId) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        ServicePackage pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new BadRequestAlertException("Service package not found", ENTITY_NAME, "packagenotfound"));

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
            .orElseThrow(() -> new BadRequestAlertException("Transaction not found", ENTITY_NAME, "txnotfound"));

        long amountInVnd = txn.getAmount().multiply(new BigDecimal(100)).longValue();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String createDate = formatter.format(new Date());

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VnPayUtil.VNP_VERSION);
        vnp_Params.put("vnp_Command", VnPayUtil.VNP_COMMAND);
        vnp_Params.put("vnp_TmnCode", VnPayUtil.VNP_TMN_CODE);
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVnd));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", transactionCode);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan goi dich vu MindBridge - Ma: " + transactionCode);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");
        vnp_Params.put("vnp_CreateDate", createDate);

        // Compute HMAC-SHA512 hash
        String secureHash = VnPayUtil.hashAllFields(vnp_Params);
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
        String paymentUrl = VnPayUtil.VNP_PAY_URL + "?" + query.substring(0, query.length() - 1);

        LOG.info("🔗 [VNPay Gateway] Generated Payment URL for TxnRef: {}", transactionCode);
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl, "transactionCode", transactionCode));
    }

    /**
     * {@code GET /payments/vnpay/callback} : VNPay IPN / Return Callback with IDEMPOTENCY & TRANSACTIONAL GUARD.
     */
    @GetMapping("/payments/vnpay/callback")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<Map<String, Object>> vnpayCallback(@RequestParam Map<String, String> allParams) {
        LOG.info("📥 [VNPay Callback / IPN] Received callback params: {}", allParams);

        String transactionCode = allParams.get("vnp_TxnRef");
        String responseCode = allParams.get("vnp_ResponseCode");

        if (transactionCode == null || transactionCode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("RspCode", "99", "Message", "Missing vnp_TxnRef"));
        }

        // 1. Kiểm tra Chữ ký bảo mật HMAC-SHA512
        boolean isValidSignature = VnPayUtil.verifyCallbackSignature(allParams);
        if (!isValidSignature) {
            LOG.error("❌ [VNPay Callback] Invalid Secure Hash Signature for transaction: {}", transactionCode);
            return ResponseEntity.badRequest().body(Map.of("RspCode", "97", "Message", "Invalid Checksum Signature"));
        }

        // 2. Lấy Transaction với PESSIMISTIC LOCK (Khóa Bi Quan) để chống Race Condition khi có nhiều Webhook retry đồng thời
        PaymentTransaction txn = transactionRepository.findByTransactionCodeWithLock(transactionCode)
            .orElseThrow(() -> new BadRequestAlertException("Transaction not found", ENTITY_NAME, "txnotfound"));

        DoctorSubscription sub = subscriptionRepository.findByTransactionCode(transactionCode)
            .orElseThrow(() -> new BadRequestAlertException("Subscription not found", ENTITY_NAME, "subnotfound"));

        // 3. IDEMPOTENCY GUARD (Tính lũy đẳng): Nếu giao dịch ĐÃ THÀNH CÔNG từ trước, trả về kết quả ngay mà không cộng thêm ngày!
        if ("SUCCESS".equals(txn.getPaymentStatus())) {
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

        // 4. Xử lý Trạng thái Thanh toán từ VNPay
        Instant now = Instant.now();
        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            txn.setPaymentStatus("SUCCESS");
            txn.setPaidAt(now);
            txn.setResponseCode(responseCode);
            txn.setResponseMessage("Giao dich thanh toan thanh cong qua VNPay Sandbox");
            transactionRepository.save(txn);

            ServicePackage pkg = packageRepository.findById(sub.getServicePackageId()).orElse(null);
            int duration = pkg != null ? pkg.getDurationDays() : 30;

            sub.setStatus("ACTIVE");
            sub.setStartDate(now);
            sub.setExpirationDate(now.plus(duration, ChronoUnit.DAYS));
            subscriptionRepository.save(sub);

            LOG.info("🎉 [VNPay Success] Kích hoạt thành công gói '{}' cho bác sĩ '{}' (Hạn dùng: {} ngày, Đến: {})",
                sub.getServicePackageName(), sub.getDoctorLogin(), duration, sub.getExpirationDate());

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
        // Khóa giao dịch với Pessimistic Lock
        PaymentTransaction txn = transactionRepository.findByTransactionCodeWithLock(transactionCode)
            .orElseThrow(() -> new BadRequestAlertException("Transaction not found", ENTITY_NAME, "txnotfound"));

        DoctorSubscription sub = subscriptionRepository.findByTransactionCode(transactionCode)
            .orElseThrow(() -> new BadRequestAlertException("Subscription not found", ENTITY_NAME, "subnotfound"));

        // Idempotency Guard
        if ("SUCCESS".equals(txn.getPaymentStatus())) {
            LOG.warn("⚡ [IDEMPOTENCY GUARD] Mock Checkout gọi lặp lại cho mã '{}'. Trả về kết quả trước đó mà không cộng lặp ngày!", transactionCode);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "isIdempotentReplay", true,
                "status", "ACTIVE",
                "transactionCode", transactionCode,
                "message", "Giao dịch đã được thanh toán thành công trước đó (Idempotent Response).",
                "startDate", sub.getStartDate().toString(),
                "expirationDate", sub.getExpirationDate().toString()
            ));
        }

        ServicePackage pkg = packageRepository.findById(sub.getServicePackageId()).orElse(null);
        int duration = pkg != null ? pkg.getDurationDays() : 30;

        Instant now = Instant.now();
        txn.setPaymentStatus("SUCCESS");
        txn.setPaidAt(now);
        txn.setResponseCode("00");
        txn.setResponseMessage("Giao dich thanh toan thanh cong (Mock Sandbox)");
        transactionRepository.save(txn);

        sub.setStatus("ACTIVE");
        sub.setStartDate(now);
        sub.setExpirationDate(now.plus(duration, ChronoUnit.DAYS));
        subscriptionRepository.save(sub);

        LOG.info("🎉 [Payment Service] Mock Checkout FIRST RUN SUCCESS for Doctor '{}', Sub ID: {}, Package: '{}'",
            sub.getDoctorLogin(), sub.getId(), sub.getServicePackageName());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "isIdempotentReplay", false,
            "status", "ACTIVE",
            "transactionCode", transactionCode,
            "message", "Thanh toán gói dịch vụ thành công lần đầu!",
            "startDate", now.toString(),
            "expirationDate", sub.getExpirationDate().toString()
        ));
    }
}
