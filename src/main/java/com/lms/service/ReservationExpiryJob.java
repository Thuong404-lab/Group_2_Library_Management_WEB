package com.lms.service;

import com.lms.entity.Reservation;
import com.lms.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "lms.reservation-expiry.enabled", havingValue = "true")
public class ReservationExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);
    private static final int DEFAULT_HOLD_DAYS = 3;

    private final ReservationRepository reservationRepository;
    private final FinancialService financialService;

    public ReservationExpiryJob(ReservationRepository reservationRepository,
                                FinancialService financialService) {
        this.reservationRepository = reservationRepository;
        this.financialService = financialService;
    }

    @Scheduled(fixedDelayString = "${lms.reservation-expiry.fixed-delay-ms:60000}",
               initialDelayString = "${lms.reservation-expiry.initial-delay-ms:30000}")
    public void expireUncollectedReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(DEFAULT_HOLD_DAYS);
        List<Reservation> expired = reservationRepository
                .findByStatusIgnoreCaseAndReservationDateLessThanEqual("Active", cutoff);

        for (Reservation reservation : expired) {
            if (reservation.getMember() == null || reservation.getMember().getMemberId() == null) {
                log.warn("Cannot expire reservation {} because member data is missing",
                        reservation.getReservationId());
                continue;
            }
            try {
                Integer memberId = reservation.getMember().getMemberId();
                financialService.requestReservationDepositRefund(memberId, reservation.getReservationId());
                financialService.refundReservationDeposit(memberId, reservation.getReservationId());
                log.info("Expired reservation {} and refunded its deposit", reservation.getReservationId());
            } catch (Exception exception) {
                log.warn("Could not expire reservation {}: {}",
                        reservation.getReservationId(), exception.getMessage());
            }
        }
    }
}