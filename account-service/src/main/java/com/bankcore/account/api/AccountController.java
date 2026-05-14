package com.bankcore.account.api;

import com.bankcore.account.api.dto.AccountResponse;
import com.bankcore.account.api.dto.CreateAccountRequest;
import com.bankcore.account.api.dto.MoneyOperationRequest;
import com.bankcore.account.api.dto.UpdateStatusRequest;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.service.AccountService;
import com.bankcore.common.util.Money;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for account management.
 *
 * <p>Base path: {@code /api/v1/accounts}
 *
 * <p>This controller is responsible for request parsing, input validation, and
 * response serialisation only. All business logic lives in {@link AccountService}.
 *
 * <p>Authentication is delegated to the API Gateway, which validates the JWT and
 * forwards a trusted {@code X-User-Id} header. This service does not perform its
 * own JWT validation — see {@code SecurityConfig} for the stub configuration.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(accountService.getAccountsByUser(userId));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse created = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        AccountStatus newStatus = request.status();
        return ResponseEntity.ok(accountService.updateStatus(id, newStatus));
    }

    @PostMapping("/{id}/debit")
    public ResponseEntity<AccountResponse> debit(
            @PathVariable UUID id,
            @Valid @RequestBody MoneyOperationRequest request) {
        Money amount = Money.of(request.amount(), request.currency());
        return ResponseEntity.ok(accountService.debit(id, amount));
    }

    @PostMapping("/{id}/credit")
    public ResponseEntity<AccountResponse> credit(
            @PathVariable UUID id,
            @Valid @RequestBody MoneyOperationRequest request) {
        Money amount = Money.of(request.amount(), request.currency());
        return ResponseEntity.ok(accountService.credit(id, amount));
    }
}