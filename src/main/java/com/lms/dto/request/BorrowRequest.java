package com.lms.dto.request;

import java.util.List;

public class BorrowRequest {
    private String memberEmail; // Dùng email để tìm Member như trong MemberRepository có sẵn
    private List<String> barcodes; // Danh sách mã vạch các cuốn sách muốn mượn

    public String getMemberEmail() { return memberEmail; }
    public void setMemberEmail(String memberEmail) { this.memberEmail = memberEmail; }
    public List<String> getBarcodes() { return barcodes; }
    public void setBarcodes(List<String> barcodes) { this.barcodes = barcodes; }
}