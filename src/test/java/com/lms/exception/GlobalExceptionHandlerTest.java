package com.lms.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rendersExpectedApplicationErrorAsHtml() throws Exception {
        mockMvc.perform(get("/test/view/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attribute("error", "Không tìm thấy dữ liệu"))
                .andExpect(model().attribute("errorMessage", "Không tìm thấy sách."));
    }

    @Test
    void rendersResponseBodyErrorAsJson() throws Exception {
        mockMvc.perform(get("/test/api/validation").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.message").value("Số tiền không hợp lệ."))
                .andExpect(jsonPath("$.path").value("/test/api/validation"));
    }

    @Test
    void mapsMissingRequestParameterToBadRequest() throws Exception {
        mockMvc.perform(get("/test/view/required"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 400));
    }

    @Test
    void mapsExternalServiceFailureToBadGateway() throws Exception {
        mockMvc.perform(get("/test/view/gateway"))
                .andExpect(status().isBadGateway())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 502));
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        mockMvc.perform(get("/test/view/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 500))
                .andExpect(model().attribute("error", "Lỗi hệ thống"))
                .andExpect(model().attribute("errorMessage",
                        "Đã xảy ra lỗi phía máy chủ. Vui lòng thử lại sau hoặc liên hệ quản trị viên."));
    }

    @Controller
    static class ThrowingController {
        @GetMapping("/test/view/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Không tìm thấy sách.");
        }

        @GetMapping("/test/api/validation")
        @ResponseBody
        Map<String, Object> validation() {
            throw new ValidationException("Số tiền không hợp lệ.");
        }

        @GetMapping("/test/view/required")
        String required(@RequestParam String keyword) {
            return keyword;
        }

        @GetMapping("/test/view/gateway")
        String gateway() {
            throw new ExternalServiceException("Không thể kết nối cổng thanh toán.");
        }

        @GetMapping("/test/view/unexpected")
        String unexpected() {
            throw new NullPointerException("secret database detail");
        }
    }
}
