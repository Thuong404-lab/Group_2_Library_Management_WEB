package com.lms.service;

import com.lms.entity.Borrow;
import com.lms.entity.Member;
import com.lms.entity.PayOsPayment;
import com.lms.entity.PayOsPaymentFineItem;
import com.lms.entity.Staff;
import com.lms.entity.Transaction;
import com.lms.exception.ConflictException;
import com.lms.exception.ExternalServiceException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.BorrowRepository;
import com.lms.repository.PayOsPaymentRepository;
import com.lms.repository.PayOsPaymentFineItemRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PreDestroy;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

@Service
public class PayOsPaymentService {

    private final LocalizedMessageService messages;
    private static final Logger LOGGER = LoggerFactory.getLogger(PayOsPaymentService.class);
    public static final String TOP_UP = "TOP_UP";
    public static final String FINE = "FINE";
    public static final String FINE_BATCH = "FINE_BATCH";
    public static final String BORROW_FEE = "BORROW_FEE";
    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";

    private static final AtomicLong ORDER_CODES = new AtomicLong(System.currentTimeMillis() * 1000L);
    private static final BigDecimal MAX_PAYMENT = TopUpPolicy.MAX_AMOUNT;
    private static final int MAX_DESCRIPTION_LENGTH = 25;
    public static final int PAYMENT_EXPIRY_MINUTES = 5;

    private final PayOsPaymentRepository paymentRepository;
    private final PayOsPaymentFineItemRepository fineItemRepository;
    private final TransactionRepository transactionRepository;
    private final BorrowRepository borrowRepository;
    private final FinancialService financialService;
    private final PayOsSettlementService settlementService;
    private final PayOsPaymentAuditService auditService;
    private final WalletRepository walletRepository;
    private final String clientId;
    private final String apiKey;
    private final String checksumKey;
    private final String baseUrl;
    private final PayOS payOS;

    public PayOsPaymentService(PayOsPaymentRepository paymentRepository,
                               PayOsPaymentFineItemRepository fineItemRepository,
                               TransactionRepository transactionRepository,
                               BorrowRepository borrowRepository,
                               FinancialService financialService,
                               PayOsSettlementService settlementService,
                               PayOsPaymentAuditService auditService,
                               WalletRepository walletRepository,
                               @Value("${PAYOS_CLIENT_ID:${payos.client-id:}}") String clientId,
                               @Value("${PAYOS_API_KEY:${payos.api-key:}}") String apiKey,
                               @Value("${PAYOS_CHECKSUM_KEY:${payos.checksum-key:}}") String checksumKey,
                               @Value("${APP_BASE_URL:${app.base-url:http://localhost:8080}}") String baseUrl,
                               LocalizedMessageService messages) {
        this.paymentRepository = paymentRepository;
        this.fineItemRepository = fineItemRepository;
        this.transactionRepository = transactionRepository;
        this.borrowRepository = borrowRepository;
        this.financialService = financialService;
        this.settlementService = settlementService;
        this.auditService = auditService;
        this.walletRepository = walletRepository;
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.checksumKey = checksumKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.messages = messages;
        this.payOS = clientId.isBlank() || apiKey.isBlank() || checksumKey.isBlank()
                ? null : new PayOS(clientId, apiKey, checksumKey);
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment createTopUp(Member member, BigDecimal amount) {
        BigDecimal validAmount = requireWholeVnd(amount);
        if (validAmount.compareTo(TopUpPolicy.MIN_AMOUNT) < 0) {
            throw new ValidationException(messages.get("backend.payment.minimumTopup"));
        }
        PayOsPayment activePayment = paymentRepository
                .findFirstByMemberMemberIdAndPurposeAndAmountAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        member.getMemberId(), TOP_UP, validAmount, PENDING,
                        LocalDateTime.now().minusMinutes(PAYMENT_EXPIRY_MINUTES))
                .orElse(null);
        if (activePayment != null) {
            return activePayment;
        }
        validateWalletCapacity(member, validAmount);
        return createPayment(member, TOP_UP, null, validAmount,
                descriptionWithId("LMW NAP VI ID", "LMW NAP ID", member.getMemberId()),
                "/member/payments/payos/return");
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment createTopUpForLibrarian(Member member, BigDecimal amount,
                                                String requestId, Staff initiatedBy) {
        BigDecimal validAmount = requireWholeVnd(amount);
        if (validAmount.compareTo(TopUpPolicy.MIN_AMOUNT) < 0) {
            throw new ValidationException(messages.get("backend.payment.minimumTopup"));
        }
        if (!isValidRequestId(requestId)) {
            throw new ValidationException(messages.get("backend.financial.invalidRequest"));
        }
        if (initiatedBy == null || initiatedBy.getStaffId() == null) {
            throw new ValidationException(messages.get("backend.financial.staffRequired"));
        }
        String normalizedRequestId = requestId.trim();
        PayOsPayment requestPayment = paymentRepository.findByRequestKey(normalizedRequestId).orElse(null);
        if (requestPayment != null) {
            validateExistingTopUpRequest(requestPayment, member, validAmount);
            return requestPayment;
        }
        validateWalletCapacity(member, validAmount);
        return createPayment(member, TOP_UP, null, validAmount,
                descriptionWithId("LMW NOP TAI QUAY ID", "LMW NOP QUAY ID", member.getMemberId()),
                "/librarian/payments/payos/return", normalizedRequestId, initiatedBy);
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment createFinePayment(Member member, Integer fineId) {
        return createFinePayment(member, fineId, "/member/payments/payos/return");
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment createFinePaymentForLibrarian(Integer fineId) {
        Transaction fine = transactionRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.payment.fineNotFound")));
        if (fine.getWallet() == null || fine.getWallet().getMember() == null) {
            throw new ResourceNotFoundException(messages.get("backend.member.currentNotFound"));
        }
        return createFinePayment(fine.getWallet().getMember(), fineId, "/librarian/payments/payos/return");
    }

    private PayOsPayment createFinePayment(Member member, Integer fineId, String returnPath) {
        Transaction fine = transactionRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.payment.fineNotFound")));
        if (fine.getWallet() == null || fine.getWallet().getMember() == null
                || !member.getMemberId().equals(fine.getWallet().getMember().getMemberId())) {
            throw new ForbiddenException(messages.get("backend.payment.fineOwnerMismatch"));
        }
        String type = normalize(fine.getTransactionType());
        if (!FINE.equals(type) && !"DAMAGE_FEE".equals(type)) {
            throw new ValidationException(messages.get("backend.financial.notFineTransaction"));
        }
        if (isCompleted(fine.getStatus())) {
            throw new ConflictException(messages.get("backend.financial.fineAlreadyPaid"));
        }
        PayOsPayment activePayment = findActivePayment(member.getMemberId(), FINE, fineId);
        if (activePayment != null) {
            return activePayment;
        }
        return createPayment(member, FINE, fineId, requireWholeVnd(fine.getAmount().abs()),
                descriptionWithId("LMW NOP PHAT ID", "LMW PHAT ID", fineId),
                returnPath);
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment createFinePaymentForLibrarian(Member member, Integer fineId) {
        Transaction fine = transactionRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.payment.fineNotFound")));
        if (fine.getWallet() == null || fine.getWallet().getMember() == null
                || !member.getMemberId().equals(fine.getWallet().getMember().getMemberId())) {
            throw new ForbiddenException(messages.get("backend.payment.fineOwnerMismatch"));
        }
        String type = normalize(fine.getTransactionType());
        if (!FINE.equals(type) && !"DAMAGE_FEE".equals(type)) {
            throw new ValidationException(messages.get("backend.financial.notFineTransaction"));
        }
        if (isCompleted(fine.getStatus())) {
            throw new ConflictException(messages.get("backend.financial.fineAlreadyPaid"));
        }
        PayOsPayment activePayment = findActivePayment(member.getMemberId(), FINE, fineId);
        if (activePayment != null) {
            return activePayment;
        }
        return createPayment(member, FINE, fineId, requireWholeVnd(fine.getAmount().abs()),
                descriptionWithId("LMW NOP PHAT ID", "LMW PHAT ID", fineId),
                "/librarian/payments/payos/return");
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment createFineBatchPayment(Member member) {
        List<Transaction> fines = transactionRepository.findUnpaidFineTransactions(
                member.getMemberId(), List.of(FINE, "DAMAGE_FEE"));
        return createFineBatchPayment(member, fines, "/member/payments/payos/return");
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment createFineBatchPaymentForLibrarian(Integer borrowId) {
        List<Transaction> fines = transactionRepository.findPendingFineTransactionsByBorrowId(
                borrowId, List.of(FINE, "DAMAGE_FEE"));
        if (fines.isEmpty()) {
            throw new ConflictException(messages.get("backend.payment.noFinesDue"));
        }
        Transaction first = fines.get(0);
        if (first.getWallet() == null || first.getWallet().getMember() == null) {
            throw new ResourceNotFoundException(messages.get("backend.member.currentNotFound"));
        }
        return createFineBatchPayment(first.getWallet().getMember(), fines,
                "/librarian/payments/payos/return");
    }

    private PayOsPayment createFineBatchPayment(Member member, List<Transaction> fines, String returnPath) {
        if (fines.isEmpty()) {
            throw new ConflictException(messages.get("backend.payment.noFinesDue"));
        }

        PayOsPayment activePayment = paymentRepository
                .findFirstByMemberMemberIdAndPurposeAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        member.getMemberId(), FINE_BATCH, PENDING,
                        LocalDateTime.now().minusMinutes(PAYMENT_EXPIRY_MINUTES))
                .orElse(null);
        if (activePayment != null) {
            if (sameFineSnapshot(activePayment, fines)) {
                return activePayment;
            }
            cancelStaleBatchPayment(activePayment);
        }

        BigDecimal total = fines.stream()
                .map(fine -> fine.getAmount() == null ? BigDecimal.ZERO : fine.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PayOsPayment payment = createPayment(member, FINE_BATCH, null, requireWholeVnd(total),
                descriptionWithId("LMW NOP PHAT ID", "LMW PHAT ID", member.getMemberId()),
                returnPath);
        for (Transaction fine : fines) {
            PayOsPaymentFineItem item = new PayOsPaymentFineItem();
            item.setPayment(payment);
            item.setFineTransaction(fine);
            item.setAmountSnapshot(fine.getAmount().abs());
            fineItemRepository.save(item);
        }
        return payment;
    }

    boolean sameFineSnapshot(PayOsPayment payment, List<Transaction> currentFines) {
        List<PayOsPaymentFineItem> savedItems = fineItemRepository
                .findByPaymentPaymentIdOrderByFineTransactionTransactionId(payment.getPaymentId());
        if (savedItems.size() != currentFines.size()) {
            return false;
        }

        Map<Integer, BigDecimal> currentAmounts = new HashMap<>();
        for (Transaction fine : currentFines) {
            if (fine.getTransactionId() == null || fine.getAmount() == null) {
                return false;
            }
            currentAmounts.put(fine.getTransactionId(), fine.getAmount().abs());
        }
        for (PayOsPaymentFineItem item : savedItems) {
            if (item.getFineTransaction() == null || item.getAmountSnapshot() == null) {
                return false;
            }
            BigDecimal currentAmount = currentAmounts.remove(item.getFineTransaction().getTransactionId());
            if (currentAmount == null || currentAmount.compareTo(item.getAmountSnapshot().abs()) != 0) {
                return false;
            }
        }
        return currentAmounts.isEmpty();
    }

    private void cancelStaleBatchPayment(PayOsPayment payment) {
        try {
            client().paymentRequests().cancel(payment.getOrderCode(), "Danh sach phi phat da thay doi");
        } catch (Exception e) {
            throw new ExternalServiceException(
                    messages.get("backend.payment.fineListChangedCancelFailed"), e);
        }
        String oldStatus = payment.getStatus();
        payment.setStatus("CANCELLED");
        paymentRepository.save(payment);
        auditService.record(payment, "PAYMENT_CANCELLED", "MEMBER", oldStatus, payment.getStatus(), true,
                messages.get("backend.payment.audit.cancelledChangedFineList"));
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment createBorrowFeePayment(Member member, Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.loan.notFoundById", borrowId)));
        if (borrow.getMember() == null || !member.getMemberId().equals(borrow.getMember().getMemberId())) {
            throw new ForbiddenException(messages.get("backend.financial.loanOwnerMismatch"));
        }
        String status = normalize(borrow.getStatus());
        if (!"ACTIVE".equals(status) && !"BORROWING".equals(status) && !"OVERDUE".equals(status)
                && !"PAYMENT_PENDING".equals(status)) {
            throw new ConflictException(messages.get("backend.payment.loanNotPayable"));
        }
        if (financialService.hasPaidBorrowingFee(member.getMemberId(), borrowId)) {
            throw new ConflictException(messages.get("backend.financial.borrowFeeAlreadyPaid"));
        }
        PayOsPayment activePayment = findActivePayment(member.getMemberId(), BORROW_FEE, borrowId);
        if (activePayment != null) {
            return activePayment;
        }
        BigDecimal amount = requireWholeVnd(financialService.calculateBorrowingFeeAmount(borrowId));
        return createPayment(member, BORROW_FEE, borrowId, amount,
                descriptionWithId("LMW NOP PHI MUON ID", "LMW MUON ID", borrowId),
                "/member/payments/payos/return");
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment createBorrowFeeForLibrarian(Member member, Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.loan.notFoundById", borrowId)));
        if (borrow.getMember() == null || !member.getMemberId().equals(borrow.getMember().getMemberId())) {
            throw new ForbiddenException(messages.get("backend.payment.loanMemberMismatch"));
        }
        String status = normalize(borrow.getStatus());
        if (!"ACTIVE".equals(status) && !"BORROWING".equals(status) && !"OVERDUE".equals(status)
                && !"PAYMENT_PENDING".equals(status)) {
            throw new ConflictException(messages.get("backend.payment.loanNotPayable"));
        }
        if (financialService.hasPaidBorrowingFee(member.getMemberId(), borrowId)) {
            throw new ConflictException(messages.get("backend.financial.borrowFeeAlreadyPaid"));
        }
        PayOsPayment activePayment = findActivePayment(member.getMemberId(), BORROW_FEE, borrowId);
        if (activePayment != null) {
            return activePayment;
        }
        BigDecimal amount = requireWholeVnd(financialService.calculateBorrowingFeeAmount(borrowId));
        return createPayment(member, BORROW_FEE, borrowId, amount,
                descriptionWithId("LMW NOP PHI MUON ID", "LMW MUON ID", borrowId),
                "/librarian/payments/payos/return");
    }

    @Transactional(readOnly = true)
    public PayOsPayment getForMember(Long orderCode, Integer memberId) {
        return paymentRepository.findByOrderCodeAndMemberMemberId(orderCode, memberId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.payment.notFound")));
    }

    @Transactional(readOnly = true)
    public PayOsPayment getForStaff(Long orderCode) {
        return paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.payment.notFound")));
    }

    @Transactional(readOnly = true)
    public List<PayOsPaymentFineItem> getFineItems(PayOsPayment payment) {
        if (payment == null || !FINE_BATCH.equals(payment.getPurpose()) || payment.getPaymentId() == null) {
            return List.of();
        }
        return fineItemRepository.findByPaymentPaymentIdOrderByFineTransactionTransactionId(payment.getPaymentId());
    }

    public long getExpiryEpochMillis(PayOsPayment payment) {
        if (payment == null || payment.getCreatedAt() == null) {
            return 0L;
        }
        return payment.getCreatedAt().plusMinutes(PAYMENT_EXPIRY_MINUTES)
                .atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .toInstant().toEpochMilli();
    }

    /**
     * Local-friendly fallback: the browser polls this method and the server asks
     * PayOS for the authoritative payment status. Production still uses webhook.
     */
    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment refreshForMember(Long orderCode, Integer memberId) {
        return refreshPayment(orderCode, memberId, false, "MEMBER_POLL");
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment refreshForStaff(Long orderCode) {
        return refreshPayment(orderCode, null, false, "STAFF_POLL");
    }

    @Transactional(rollbackFor = Exception.class)
    public PayOsPayment reconcileForStaff(Long orderCode) {
        return refreshPayment(orderCode, null, true, "RECONCILIATION");
    }

    private PayOsPayment refreshPayment(Long orderCode, Integer expectedMemberId,
                                        boolean failOnGatewayError, String source) {
        PayOsPayment current = expectedMemberId == null
                ? getForStaff(orderCode)
                : getForMember(orderCode, expectedMemberId);
        if (PAID.equalsIgnoreCase(current.getStatus()) && current.getTransaction() != null) {
            return current;
        }
        if (payOS == null) {
            if (failOnGatewayError) {
                throw new ExternalServiceException(messages.get("backend.payment.reconcileNotConfigured"));
            }
            return current;
        }

        PaymentLink remote;
        try {
            remote = client().paymentRequests().get(orderCode);
        } catch (Exception exception) {
            if (failOnGatewayError) {
                throw new ExternalServiceException(messages.get("backend.payment.statusFetchFailed"), exception);
            }
            LOGGER.warn("Không thể đồng bộ trạng thái đơn KQPay {} trong lượt polling.", orderCode, exception);
            return current;
        }
        if (remote == null || remote.getStatus() == null) {
            return current;
        }

        PayOsPayment payment = paymentRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.payment.notFound")));
        if ((expectedMemberId != null && !expectedMemberId.equals(payment.getMember().getMemberId()))
                || (PAID.equalsIgnoreCase(payment.getStatus()) && payment.getTransaction() != null)) {
            return payment;
        }

        String oldStatus = payment.getStatus();
        if (remote.getStatus() == PaymentLinkStatus.PAID) {
            validateRemotePayment(payment, remote);
            Transaction transaction = settlementService.settle(payment);
            payment.setTransaction(transaction);
            payment.setPaidAt(LocalDateTime.now());
            payment.setStatus(PAID);
        } else if (remote.getStatus() == PaymentLinkStatus.CANCELLED
                || remote.getStatus() == PaymentLinkStatus.EXPIRED
                || remote.getStatus() == PaymentLinkStatus.FAILED) {
            settlementService.cancelPendingBorrow(payment, remote.getStatus().name());
            payment.setStatus(remote.getStatus().name());
        }
        PayOsPayment saved = paymentRepository.save(payment);
        if (!normalize(oldStatus).equals(normalize(saved.getStatus()))) {
            auditService.record(saved, "STATUS_CHANGED", source, oldStatus, saved.getStatus(), true,
                    messages.get("backend.payment.audit.statusSynchronized"));
        }
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleWebhook(Webhook webhook) {
        requireConfigured();
        WebhookData data;
        try {
            data = client().webhooks().verify(webhook);
        } catch (Exception ex) {
            throw new ValidationException(messages.get("backend.payment.invalidWebhookSignature"), ex);
        }
        if (data == null || data.getOrderCode() == null || !"00".equals(data.getCode())) {
            return;
        }

        PayOsPayment payment = paymentRepository.findByOrderCodeForUpdate(data.getOrderCode()).orElse(null);
        // payOS sends a sample payload while confirming the webhook URL.
        if (payment == null || (PAID.equalsIgnoreCase(payment.getStatus()) && payment.getTransaction() != null)) {
            return;
        }
        if (data.getAmount() == null || payment.getAmount().compareTo(BigDecimal.valueOf(data.getAmount())) != 0) {
            throw new ConflictException(messages.get("backend.payment.webhookAmountMismatch"));
        }
        if (payment.getPaymentLinkId() != null && data.getPaymentLinkId() != null
                && !payment.getPaymentLinkId().equals(data.getPaymentLinkId())) {
            throw new ConflictException(messages.get("backend.payment.linkIdMismatch"));
        }

        String oldStatus = payment.getStatus();
        Transaction transaction = settlementService.settle(payment);
        payment.setTransaction(transaction);
        payment.setBankReference(data.getReference());
        payment.setPaidAt(LocalDateTime.now());
        payment.setStatus(PAID);
        paymentRepository.save(payment);
        auditService.record(payment, "WEBHOOK_SETTLED", "WEBHOOK", oldStatus, payment.getStatus(), true,
                messages.get("backend.payment.audit.webhookSettled"));
    }

    private PayOsPayment createPayment(Member member, String purpose, Integer referenceId,
                                       BigDecimal amount, String description, String returnPath) {
        return createPayment(member, purpose, referenceId, amount, description, returnPath, null, null);
    }

    private PayOsPayment createPayment(Member member, String purpose, Integer referenceId,
                                       BigDecimal amount, String description, String returnPath,
                                       String requestKey, Staff initiatedBy) {
        requireConfigured();
        if (member == null || member.getMemberId() == null) {
            throw new ResourceNotFoundException(messages.get("backend.member.currentNotFound"));
        }

        long orderCode = ORDER_CODES.incrementAndGet();
        PayOsPayment payment = new PayOsPayment();
        payment.setMember(member);
        payment.setPurpose(purpose);
        payment.setReferenceId(referenceId);
        payment.setAmount(amount);
        payment.setOrderCode(orderCode);
        payment.setRequestKey(requestKey);
        payment.setInitiatedByStaff(initiatedBy);
        payment.setStatus(PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        String returnUrl = baseUrl + returnPath;
        String cancelUrl = baseUrl + returnPath + "?cancel=true";
        CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount.longValueExact())
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .expiredAt(LocalDateTime.now().plusMinutes(PAYMENT_EXPIRY_MINUTES)
                        .atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toEpochSecond())
                .build();
        CreatePaymentLinkResponse response;
        try {
            response = client().paymentRequests().create(request);
        } catch (Exception ex) {
            throw new ExternalServiceException(messages.get("backend.payment.linkCreateFailed"), ex);
        }
        if (response == null) {
            throw new ExternalServiceException(messages.get("backend.payment.emptyGatewayResponse"));
        }
        payment.setPaymentLinkId(response.getPaymentLinkId());
        payment.setCheckoutUrl(response.getCheckoutUrl());
        payment.setQrCode(response.getQrCode());
        PayOsPayment saved = paymentRepository.save(payment);
        auditService.record(saved, "PAYMENT_CREATED", "APPLICATION", null, saved.getStatus(), true,
                messages.get("backend.payment.audit.created", saved.getPurpose()));
        return saved;
    }

    private String descriptionWithId(String preferredPrefix, String compactPrefix, Integer id) {
        String suffix = id == null ? "" : String.valueOf(id);
        String preferred = preferredPrefix + suffix;
        if (preferred.length() <= MAX_DESCRIPTION_LENGTH) {
            return preferred;
        }
        String compact = compactPrefix + suffix;
        if (compact.length() <= MAX_DESCRIPTION_LENGTH) {
            return compact;
        }
        int suffixLength = Math.min(suffix.length(), MAX_DESCRIPTION_LENGTH - 1);
        return "I" + suffix.substring(suffix.length() - suffixLength);
    }

    private PayOsPayment findActivePayment(Integer memberId, String purpose, Integer referenceId) {
        return paymentRepository
                .findFirstByMemberMemberIdAndPurposeAndReferenceIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        memberId, purpose, referenceId, PENDING,
                        LocalDateTime.now().minusMinutes(PAYMENT_EXPIRY_MINUTES))
                .orElse(null);
    }

    private void validateRemotePayment(PayOsPayment payment, PaymentLink remote) {
        BigDecimal expected = payment.getAmount();
        if (remote.getOrderCode() == null || !payment.getOrderCode().equals(remote.getOrderCode())
                || remote.getAmount() == null || expected.compareTo(BigDecimal.valueOf(remote.getAmount())) != 0
                || remote.getAmountPaid() == null || expected.compareTo(BigDecimal.valueOf(remote.getAmountPaid())) != 0) {
            throw new ConflictException(messages.get("backend.payment.orderDataMismatch"));
        }
        if (payment.getPaymentLinkId() != null && remote.getId() != null
                && !payment.getPaymentLinkId().equals(remote.getId())) {
            throw new ConflictException(messages.get("backend.payment.linkIdMismatch"));
        }
    }

    private PayOS client() {
        return payOS;
    }

    private void requireConfigured() {
        if (clientId.isBlank() || apiKey.isBlank() || checksumKey.isBlank()) {
            throw new ExternalServiceException(
                    messages.get("backend.payment.notConfigured"));
        }
    }

    @PreDestroy
    void closeClient() {
        if (payOS != null) {
            payOS.close();
        }
    }

    private BigDecimal requireWholeVnd(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException(messages.get("backend.payment.invalidAmount"));
        }
        BigDecimal value = amount.stripTrailingZeros();
        if (value.signum() <= 0 || value.scale() > 0 || value.compareTo(MAX_PAYMENT) > 0) {
            throw new ValidationException(messages.get("backend.payment.amountRange"));
        }
        return value;
    }

    private void validateWalletCapacity(Member member, BigDecimal amount) {
        if (member == null || member.getMemberId() == null) {
            throw new ResourceNotFoundException(messages.get("backend.member.currentNotFound"));
        }
        BigDecimal balance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                .orElse(BigDecimal.ZERO);
        if (balance.add(amount).compareTo(TopUpPolicy.MAX_WALLET_BALANCE) > 0) {
            throw new ValidationException(messages.get("backend.financial.walletLimit", TopUpPolicy.MAX_WALLET_BALANCE));
        }
    }

    private void validateExistingTopUpRequest(PayOsPayment payment, Member member, BigDecimal amount) {
        boolean sameRequest = payment.getMember() != null
                && member.getMemberId().equals(payment.getMember().getMemberId())
                && TOP_UP.equalsIgnoreCase(payment.getPurpose())
                && payment.getAmount() != null
                && payment.getAmount().compareTo(amount) == 0;
        if (!sameRequest) {
            throw new ConflictException(messages.get("backend.payment.requestKeyConflict"));
        }
        String status = normalize(payment.getStatus());
        if (!PENDING.equals(status) && !PAID.equals(status)) {
            throw new ConflictException(messages.get("backend.financial.invalidRequest"));
        }
    }

    private boolean isValidRequestId(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 48) {
            return false;
        }
        try {
            UUID.fromString(requestId.trim());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private boolean isCompleted(String status) {
        String value = normalize(status);
        return "COMPLETED".equals(value) || "PAID".equals(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
