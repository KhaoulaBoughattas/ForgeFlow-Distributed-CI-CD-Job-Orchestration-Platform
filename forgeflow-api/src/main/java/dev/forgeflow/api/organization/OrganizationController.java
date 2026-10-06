package dev.forgeflow.api.organization;

import dev.forgeflow.api.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request, principal.id()));
    }

    @GetMapping
    public List<OrganizationResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return organizationService.listForUser(principal.id());
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse get(
            @PathVariable UUID organizationId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return organizationService.get(organizationId, principal.id());
    }

    @GetMapping("/{organizationId}/members")
    public List<MembershipResponse> listMembers(
            @PathVariable UUID organizationId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return organizationService.listMembers(organizationId, principal.id());
    }

    @PostMapping("/{organizationId}/members")
    public ResponseEntity<MembershipResponse> addMember(
            @PathVariable UUID organizationId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.addMember(organizationId, request, principal.id()));
    }

    @PutMapping("/{organizationId}/members/{userId}/role")
    public MembershipResponse updateRole(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId,
            @RequestBody Map<String, MembershipRole> body,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return organizationService.updateRole(organizationId, userId, body.get("role"), principal.id());
    }

    @DeleteMapping("/{organizationId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        organizationService.removeMember(organizationId, userId, principal.id());
        return ResponseEntity.noContent().build();
    }
}
