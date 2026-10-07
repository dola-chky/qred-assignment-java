package com.dola.qredpaymentbackend.payment.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dola.qredpaymentbackend.payment.dto.PaymentResponse;
import com.dola.qredpaymentbackend.payment.service.PaymentService;
import com.dola.qredpaymentbackend.payment.web.error.NotFoundException;
import com.dola.qredpaymentbackend.payment.web.error.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

  private MockMvc mockMvc;

  @Mock
  private PaymentService paymentService;

  @InjectMocks
  private PaymentController paymentController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void createPaymentReturnsCreatedPayment() throws Exception {
    when(paymentService.createPayment(any())).thenReturn(
        new PaymentResponse(UUID.randomUUID(), "12345", "54321", LocalDate.of(2024, 1, 30),
            new BigDecimal("1000.00"), "incoming"));

    String idempotencyKey = UUID.randomUUID().toString();
    mockMvc.perform(post("/api/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format("""
                {
                  "idempotencyKey": "%s",
                  "clientNumber": "12345",
                  "contractNumber": "54321",
                  "paymentDate": "2024-01-30",
                  "amount": 1000.00,
                  "type": "incoming"
                }
                """, idempotencyKey)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.clientNumber").value("12345"))
        .andExpect(jsonPath("$.contractNumber").value("54321"))
        .andExpect(jsonPath("$.type").value("incoming"));
  }

  @Test
  void getPaymentsReturnsList() throws Exception {
    when(paymentService.getPayments("12345", null)).thenReturn(List.of(
        new PaymentResponse(UUID.randomUUID(), "12345", "54321", LocalDate.of(2024, 1, 30),
            new BigDecimal("1000.00"), "incoming")));

    mockMvc.perform(get("/api/payments").param("clientNumber", "12345"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].clientNumber").value("12345"))
        .andExpect(jsonPath("$[0].contractNumber").value("54321"));
  }

  @Test
  void getPaymentsWithoutClientNumberReturnsBadRequest() throws Exception {
    mockMvc.perform(get("/api/payments"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Required request parameter 'clientNumber' for method parameter type String is not present"));
  }

  @Test
  void validationErrorReturnsBadRequest() throws Exception {
    String idempotencyKey = UUID.randomUUID().toString();
    mockMvc.perform(post("/api/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format("""
                {
                  "idempotencyKey": "%s",
                  "clientNumber": "",
                  "contractNumber": "54321",
                  "paymentDate": "2024-01-30",
                  "amount": 1000.00,
                  "type": "incoming"
                }
                """, idempotencyKey)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void createPaymentReturnsNotFoundWhenServiceThrowsNotFoundException() throws Exception {
    when(paymentService.createPayment(any())).thenThrow(
        new NotFoundException("Client 12345 does not exist"));

    String idempotencyKey = UUID.randomUUID().toString();
    mockMvc.perform(post("/api/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format("""
                {
                  "idempotencyKey": "%s",
                  "clientNumber": "12345",
                  "contractNumber": "54321",
                  "paymentDate": "2024-01-30",
                  "amount": 1000.00,
                  "type": "incoming"
                }
                """, idempotencyKey)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Client 12345 does not exist"));
  }

  @Test
  void createPaymentReturnsBadRequestForMalformedJson() throws Exception {
    mockMvc.perform(post("/api/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "idempotencyKey": "abc",
                  "clientNumber": "12345",
                  "contractNumber": "54321",
                  "paymentDate": "2024-01-30",
                  "amount": 1000.00,
                  "type": "incoming",
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Malformed request body"));

    verifyNoInteractions(paymentService);
  }

  @Test
  void createPaymentReturnsBadRequestForInvalidDateFormat() throws Exception {
    mockMvc.perform(post("/api/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "idempotencyKey": "abc",
                  "clientNumber": "12345",
                  "contractNumber": "54321",
                  "paymentDate": "30-01-2024",
                  "amount": 1000.00,
                  "type": "incoming"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Malformed request body"));

    verifyNoInteractions(paymentService);
  }

  @Test
  void createPaymentReturnsBadRequestForNonNumericAmount() throws Exception {
    mockMvc.perform(post("/api/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "idempotencyKey": "abc",
                  "clientNumber": "12345",
                  "contractNumber": "54321",
                  "paymentDate": "2024-01-30",
                  "amount": "one-thousand",
                  "type": "incoming"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Malformed request body"));

    verifyNoInteractions(paymentService);
  }

  @Test
  void createPaymentReturnsBadRequestForEmptyRequestBody() throws Exception {
    mockMvc.perform(post("/api/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Malformed request body"));

    verifyNoInteractions(paymentService);
  }
}

