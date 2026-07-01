package com.lms.service.impl;

import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.AccountRepository;
import com.lms.service.MembershipService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MembershipServiceImpl implements MembershipService {
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final AccountRepository accountRepository;

    public MembershipServiceImpl(MemberRepository memberRepository,
                                 MembershipTierRepository membershipTierRepository,
                                 AccountRepository accountRepository) {
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public MembershipTier getBenefits(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        return memberOpt.map(Member::getTier).orElse(null);
    }

    @Override
    public Member getMembershipTier(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        return memberOpt.orElse(null);
    }

    @Override
    public Member getMemberByUsername(String username) {
        return memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Member not found for: " + username));
    }

    // --- BỔ SUNG XỬ LÝ LOGIC TIẾN TRÌNH HẠNG ĐỘNG CHO VIEW ---

    @Override
    public List<MembershipTier> getAllTiers() {
        // Lấy tất cả hạng thẻ và sắp xếp theo điều kiện (condition - kiểu BigDecimal) tăng dần
        return membershipTierRepository.findAll().stream()
                .sorted((t1, t2) -> {
                    if (t1.getCondition() == null) return -1;
                    if (t2.getCondition() == null) return 1;
                    return t1.getCondition().compareTo(t2.getCondition());
                })
                .toList();
    }

    @Override
    public double getAccumulatedSpending(Member member) {
        // Vì Entity Member của bạn chưa có trường tích lũy spending,
        // tạm thời trả về con số mặc định (ví dụ: 150000.0) để đổ lên HTML không bị trống.
        // Sau này bạn có thể liên kết qua bảng hóa đơn hoặc Wallet tùy cấu trúc nhóm.
        return 150000.0;
    }

    @Override
    public MembershipTier getNextTier(MembershipTier currentTier) {
        if (currentTier == null) return null;
        List<MembershipTier> tiers = getAllTiers();

        // Sử dụng .compareTo() của BigDecimal thay cho dấu '>'
        return tiers.stream()
                .filter(t -> t.getCondition() != null && currentTier.getCondition() != null
                        && t.getCondition().compareTo(currentTier.getCondition()) > 0)
                .findFirst()
                .orElse(null); // Trả về null nếu đã ở hạng cao nhất
    }
    @Override
    public List<Member> getTopMembersBySpending() {
        // Lấy tất cả thành viên, tạm thời trả về danh sách để hiển thị làm bảng xếp hạng trên Dashboard
        return memberRepository.findAll().stream()
                .limit(5) // Lấy top 5 thành viên
                .toList();
    }
}