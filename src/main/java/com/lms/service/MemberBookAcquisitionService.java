package com.lms.service;

import com.lms.dto.request.MemberBookAcquisitionRequest;

public interface MemberBookAcquisitionService {

    void submitRequest(String username, MemberBookAcquisitionRequest request);
}