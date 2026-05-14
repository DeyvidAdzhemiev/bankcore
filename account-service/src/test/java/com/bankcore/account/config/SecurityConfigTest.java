package com.bankcore.account.config;

import com.bankcore.account.api.AccountController;
import com.bankcore.account.api.dto.AccountResponse;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.domain.AccountType;
import com.bankcore.account.service.AccountService;
import com.bankcore.common.util.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@Import({SecurityConfig.class, GatewayHeaderAuthenticationFilter.class})
@MockBean(AccountService.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Test
    void accountEndpointsRejectMissingGatewayUserHeader() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountEndpointsRejectInvalidGatewayUserHeader() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + UUID.randomUUID())
                        .header(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, "not-a-uuid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountEndpointsAllowValidGatewayUserHeader() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.getAccount(accountId)).thenReturn(accountResponse(accountId));

        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .header(GatewayHeaderAuthenticationFilter.USER_ID_HEADER,
                                "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isOk());
    }

    private static AccountResponse accountResponse(UUID accountId) {
        BigDecimal amount = new BigDecimal("0.00");
        return new AccountResponse(
                accountId,
                UUID.randomUUID(),
                "BC12345678901234",
                AccountType.CHECKING,
                Currency.USD,
                amount,
                amount,
                AccountStatus.ACTIVE,
                0L,
                Instant.parse("2026-05-14T14:00:00Z"),
                Instant.parse("2026-05-14T14:00:01Z")
        );
    }
}
