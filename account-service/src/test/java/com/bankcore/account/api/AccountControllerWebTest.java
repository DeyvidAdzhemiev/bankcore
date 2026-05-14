package com.bankcore.account.api;

import com.bankcore.account.api.dto.AccountResponse;
import com.bankcore.account.config.GatewayHeaderAuthenticationFilter;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.domain.AccountType;
import com.bankcore.account.service.AccountService;
import com.bankcore.common.util.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@MockBean(AccountService.class)
class AccountControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Test
    void getAccountReturnsAccountResponse() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.getAccount(accountId)).thenReturn(accountResponse(accountId));

        mockMvc.perform(withGatewayUser(get("/api/v1/accounts/" + accountId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.accountNumber").value("BC12345678901234"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getAccountsByUserReturnsAccounts() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(accountService.getAccountsByUser(userId)).thenReturn(List.of(accountResponse(accountId, userId)));

        mockMvc.perform(withGatewayUser(get("/api/v1/accounts/user/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(accountId.toString()))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void createAccountReturnsCreatedAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.createAccount(any())).thenReturn(accountResponse(accountId));

        mockMvc.perform(withGatewayUser(post("/api/v1/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"11111111-1111-1111-1111-111111111111","type":"CHECKING","currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void updateStatusReturnsUpdatedAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountResponse frozen = accountResponse(accountId, UUID.randomUUID(), AccountStatus.FROZEN);
        when(accountService.updateStatus(eq(accountId), eq(AccountStatus.FROZEN))).thenReturn(frozen);

        mockMvc.perform(withGatewayUser(patch("/api/v1/accounts/" + accountId + "/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FROZEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void creditReturnsUpdatedAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountResponse credited = accountResponse(accountId, UUID.randomUUID(), AccountStatus.ACTIVE, "25.00");
        when(accountService.credit(eq(accountId), any())).thenReturn(credited);

        mockMvc.perform(withGatewayUser(post("/api/v1/accounts/" + accountId + "/credit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":25.00,\"currency\":\"USD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(25.00));
    }

    @Test
    void debitReturnsUpdatedAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountResponse debited = accountResponse(accountId, UUID.randomUUID(), AccountStatus.ACTIVE, "5.00");
        when(accountService.debit(eq(accountId), any())).thenReturn(debited);

        mockMvc.perform(withGatewayUser(post("/api/v1/accounts/" + accountId + "/debit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5.00,\"currency\":\"USD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5.00));
    }

    private static AccountResponse accountResponse(UUID accountId) {
        return accountResponse(accountId, UUID.randomUUID());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withGatewayUser(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
        return request.header(
                GatewayHeaderAuthenticationFilter.USER_ID_HEADER,
                "11111111-1111-1111-1111-111111111111");
    }

    private static AccountResponse accountResponse(UUID accountId, UUID userId) {
        return accountResponse(accountId, userId, AccountStatus.ACTIVE);
    }

    private static AccountResponse accountResponse(UUID accountId, UUID userId, AccountStatus status) {
        return accountResponse(accountId, userId, status, "0.00");
    }

    private static AccountResponse accountResponse(
            UUID accountId, UUID userId, AccountStatus status, String balance) {
        BigDecimal amount = new BigDecimal(balance);
        return new AccountResponse(
                accountId,
                userId,
                "BC12345678901234",
                AccountType.CHECKING,
                Currency.USD,
                amount,
                amount,
                status,
                0L,
                Instant.parse("2026-05-14T14:00:00Z"),
                Instant.parse("2026-05-14T14:00:01Z")
        );
    }
}
