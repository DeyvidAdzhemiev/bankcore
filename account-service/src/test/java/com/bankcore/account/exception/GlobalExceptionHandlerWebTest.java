package com.bankcore.account.exception;

import com.bankcore.account.api.AccountController;
import com.bankcore.account.config.GatewayHeaderAuthenticationFilter;
import com.bankcore.account.service.AccountService;
import com.bankcore.common.exception.AccountNotFoundException;
import com.bankcore.common.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@MockBean(AccountService.class)
class GlobalExceptionHandlerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Test
    void accountNotFound_returns404WithErrorCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(id)).thenThrow(new AccountNotFoundException(id));

        mockMvc.perform(withGatewayUser(get("/api/v1/accounts/" + id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/accounts/" + id));
    }

    @Test
    void insufficientFunds_returns422WithErrorCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.debit(any(), any())).thenThrow(
                new InsufficientFundsException(id, "100.00 USD", "10.00 USD"));

        mockMvc.perform(withGatewayUser(post("/api/v1/accounts/" + id + "/debit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"currency\":\"USD\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void illegalState_returns422WithInvalidAccountState() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.debit(any(), any())).thenThrow(
                new IllegalStateException("Account " + id + " is FROZEN and does not accept transactions"));

        mockMvc.perform(withGatewayUser(post("/api/v1/accounts/" + id + "/debit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1,\"currency\":\"USD\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_ACCOUNT_STATE"));
    }

    @Test
    void validationError_returns400WithValidationErrorCode() throws Exception {
        mockMvc.perform(withGatewayUser(post("/api/v1/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":null,\"type\":\"CHECKING\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withGatewayUser(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
        return request.header(
                GatewayHeaderAuthenticationFilter.USER_ID_HEADER,
                "11111111-1111-1111-1111-111111111111");
    }
}
