package dev.forgeflow.api.organization;

import dev.forgeflow.api.user.User;
import dev.forgeflow.api.user.UserNotFoundException;
import dev.forgeflow.api.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Owns the organization/membership RBAC rules: every member has exactly one role
 * (MEMBER/ADMIN/OWNER), and an organization is never left without at least one OWNER.
 */
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request, UUID creatorUserId) {
        if (organizationRepository.existsBySlug(request.slug())) {
            throw new SlugAlreadyExistsException(request.slug());
        }

        Organization organization = Organization.create(request.name().trim(), request.slug().trim());
        organizationRepository.save(organization);

        OrganizationMembership membership = OrganizationMembership.create(
                organization.getId(), creatorUserId, MembershipRole.OWNER);
        membershipRepository.save(membership);

        return OrganizationResponse.from(organization, MembershipRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listForUser(UUID userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(membership -> {
                    Organization organization = organizationRepository.findById(membership.getOrganizationId())
                            .orElseThrow(() -> new OrganizationNotFoundException(membership.getOrganizationId()));
                    return OrganizationResponse.from(organization, membership.getRole());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID organizationId, UUID requestingUserId) {
        Organization organization = requireOrganization(organizationId);
        MembershipRole role = requireMembership(organizationId, requestingUserId).getRole();
        return OrganizationResponse.from(organization, role);
    }

    /** Verifies the user belongs to the organization at all, returning their membership. */
    @Transactional(readOnly = true)
    public OrganizationMembership requireMembership(UUID organizationId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(NotOrganizationMemberException::new);
    }

    /** Verifies the user belongs to the organization AND holds at least the given role. */
    @Transactional(readOnly = true)
    public OrganizationMembership requireRole(UUID organizationId, UUID userId, MembershipRole minRole) {
        OrganizationMembership membership = requireMembership(organizationId, userId);
        if (!membership.getRole().isAtLeast(minRole)) {
            throw new InsufficientRoleException(minRole);
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> listMembers(UUID organizationId, UUID requestingUserId) {
        requireMembership(organizationId, requestingUserId);
        return membershipRepository.findByOrganizationId(organizationId).stream()
                .map(this::toMembershipResponse)
                .toList();
    }

    @Transactional
    public MembershipResponse addMember(UUID organizationId, AddMemberRequest request, UUID requestingUserId) {
        requireRole(organizationId, requestingUserId, MembershipRole.ADMIN);
        requireOrganization(organizationId);

        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> UserNotFoundException.forEmail(normalizedEmail));

        if (membershipRepository.existsByOrganizationIdAndUserId(organizationId, user.getId())) {
            throw new AlreadyAMemberException();
        }

        OrganizationMembership membership = OrganizationMembership.create(organizationId, user.getId(), request.role());
        membershipRepository.save(membership);
        return toMembershipResponse(membership);
    }

    @Transactional
    public void removeMember(UUID organizationId, UUID targetUserId, UUID requestingUserId) {
        requireRole(organizationId, requestingUserId, MembershipRole.ADMIN);
        OrganizationMembership target = membershipRepository.findByOrganizationIdAndUserId(organizationId, targetUserId)
                .orElseThrow(NotOrganizationMemberException::new);

        if (target.getRole() == MembershipRole.OWNER && countOwners(organizationId) <= 1) {
            throw new LastOwnerException();
        }
        membershipRepository.delete(target);
    }

    @Transactional
    public MembershipResponse updateRole(UUID organizationId, UUID targetUserId, MembershipRole newRole, UUID requestingUserId) {
        requireRole(organizationId, requestingUserId, MembershipRole.OWNER);
        OrganizationMembership target = membershipRepository.findByOrganizationIdAndUserId(organizationId, targetUserId)
                .orElseThrow(NotOrganizationMemberException::new);

        if (target.getRole() == MembershipRole.OWNER && newRole != MembershipRole.OWNER && countOwners(organizationId) <= 1) {
            throw new LastOwnerException();
        }

        target.setRole(newRole);
        membershipRepository.save(target);
        return toMembershipResponse(target);
    }

    private long countOwners(UUID organizationId) {
        return membershipRepository.countByOrganizationIdAndRole(organizationId, MembershipRole.OWNER);
    }

    private Organization requireOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private MembershipResponse toMembershipResponse(OrganizationMembership membership) {
        User user = userRepository.findById(membership.getUserId())
                .orElseThrow(() -> new UserNotFoundException(membership.getUserId()));
        return new MembershipResponse(user.getId(), user.getEmail(), user.getDisplayName(), membership.getRole(), membership.getCreatedAt());
    }
}
