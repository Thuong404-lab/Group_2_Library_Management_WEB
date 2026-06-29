package com.lms.service.impl;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.*;
import com.lms.repository.*;
import com.lms.service.BorrowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BorrowServiceImpl implements BorrowService {

    private final MemberRepository memberRepository;
    private final BookItemRepository bookItemRepository;
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;

    // Dependency Injection via Constructor Injection
    public BorrowServiceImpl(MemberRepository memberRepository,
                             BookItemRepository bookItemRepository,
                             BorrowRepository borrowRepository,
                             BorrowDetailRepository borrowDetailRepository) {
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // Ensures atomicity: rolls back all operations if any exception occurs
    public Borrow processBorrowing(BorrowRequest request, String librarianUsername) throws Exception {

        // 1. Verify if the member exists via Email
        Member member = memberRepository.findByUserEmail(request.getMemberEmail())
                .orElseThrow(() -> new Exception("Member with this email was not found!"));

        // Check if the member account status is active using Enum conversion
        if (!"Active".equalsIgnoreCase(member.getUser().getStatus().name())) {
            throw new Exception("This member account is currently locked or inactive!");
        }

        // 2. Retrieve BookItem records from the provided barcode list
        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : request.getBarcodes()) {
            BookItem item = bookItemRepository.findByBarcode(barcode)
                    .orElseThrow(() -> new Exception("Barcode " + barcode + " does not exist in the system!"));

            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new Exception("The book with barcode " + barcode + " is currently unavailable (Borrowed/Under repair)!");
            }
            bookItemsToBorrow.add(item);
        }

        // 3. Check if the total of newly requested books + currently borrowed books exceeds the tier limit
        int currentBorrowCount = 0;

        // Loop through all active records to count unreturned books (Status: 'Borrowed' or 'Overdue')
        List<BorrowDetail> allBorrowDetails = borrowDetailRepository.findAll();
        for (BorrowDetail detail : allBorrowDetails) {
            Borrow parentBorrow = detail.getBorrow();
            if (parentBorrow != null && parentBorrow.getMember().getMemberId().equals(member.getMemberId())) {
                if ("Borrowed".equalsIgnoreCase(detail.getStatus()) || "Overdue".equalsIgnoreCase(detail.getStatus())) {
                    currentBorrowCount++;
                }
            }
        }

        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3; // Default to 3 if tier data is missing
        int totalRequestedBooks = currentBorrowCount + bookItemsToBorrow.size();

        if (totalRequestedBooks > maxLimit) {
            throw new Exception("The number of requested books exceeds the member tier limit! " +
                    "Currently holding: " + currentBorrowCount + " books. " +
                    "Tier limit: " + maxLimit + " books.");
        }

        // 4. Initialize and save the Borrows master record (Parent Table)
        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Active");
        borrow.setStaff(null); // Temporarily set to null; map to actual Staff entity later if needed

        borrow = borrowRepository.save(borrow);

        // 5. Insert each book into BorrowDetails (Child Table) & Update BookItem status
        for (BookItem item : bookItemsToBorrow) {
            BorrowDetail detail = new BorrowDetail();
            detail.setBorrow(borrow);
            detail.setBook(item.getBook());
            detail.setBookItem(item);

            // Standard borrowing duration is 14 days based on System Settings
            detail.setDueDate(LocalDateTime.now().plusDays(14));
            detail.setStatus("Borrowed");
            detail.setRenewCount(0);

            borrowDetailRepository.save(detail);

            // Update physical book status to 'Borrowed' to prevent double borrowing
            item.setStatus("Borrowed");
            bookItemRepository.save(item);
        }

        return borrow;
    }
}