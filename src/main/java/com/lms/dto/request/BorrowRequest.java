package com.lms.dto.request;

import java.util.List;

public class BorrowRequest {
    private String memberIdentifier; // Email hoặc Số điện thoại
    private List<String> barcodes; // Danh sách mã vạch các cuốn sách muốn mượn

    public String getMemberIdentifier() { return memberIdentifier; }
    public void setMemberIdentifier(String memberIdentifier) { this.memberIdentifier = memberIdentifier; }
    public List<String> getBarcodes() { return barcodes; }
    public void setBarcodes(List<String> barcodes) { this.barcodes = barcodes; }
}