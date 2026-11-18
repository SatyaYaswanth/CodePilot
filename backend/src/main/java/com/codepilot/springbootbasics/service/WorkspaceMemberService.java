package com.codepilot.springbootbasics.service;

import com.codepilot.springbootbasics.dto.AddWorkspaceMemberRequest;
import com.codepilot.springbootbasics.dto.WorkspaceMemberResponse;
import com.codepilot.springbootbasics.entity.CodePilotUser;
import com.codepilot.springbootbasics.entity.Workspace;
import com.codepilot.springbootbasics.entity.WorkspaceMember;
import com.codepilot.springbootbasics.entity.WorkspaceRole;
import com.codepilot.springbootbasics.exception.ResourceNotFoundException;
import com.codepilot.springbootbasics.exception.UserNotFoundException;
import com.codepilot.springbootbasics.repository.CodePilotUserRepository;
import com.codepilot.springbootbasics.repository.WorkspaceMemberRepository;
import com.codepilot.springbootbasics.repository.WorkspaceRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WorkspaceMemberService {

    private final WorkspaceRepository workspaceRepository;
    private final CodePilotUserRepository codePilotUserRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceMemberService(
            WorkspaceRepository workspaceRepository,
            CodePilotUserRepository codePilotUserRepository,
            WorkspaceMemberRepository workspaceMemberRepository) {

        this.workspaceRepository = workspaceRepository;
        this.codePilotUserRepository = codePilotUserRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    // Add a member to a workspace
    @Transactional
    public WorkspaceMemberResponse addMember(
            Long workspaceId,
            AddWorkspaceMemberRequest request,
            String authenticatedEmail) {

        WorkspaceMember caller = requireManager(workspaceId, authenticatedEmail);

        if (request.getRole() == WorkspaceRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OWNER role cannot be assigned through this operation");
        }

        // ADMIN can add MEMBER only
        if (caller.getRole() == WorkspaceRole.ADMIN
                && request.getRole() != WorkspaceRole.MEMBER) {
            throw new AccessDeniedException(
                    "ADMIN can add MEMBER accounts only");
        }

        Workspace workspace = getWorkspace(workspaceId);

        CodePilotUser user = codePilotUserRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                workspaceId, user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User is already a workspace member");
        }

        WorkspaceMember member = new WorkspaceMember(
                workspace,
                user,
                request.getRole()
        );

        return toResponse(workspaceMemberRepository.save(member));
    }

    // List workspace members
    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getMembers(
            Long workspaceId,
            String authenticatedEmail) {

        requireMembership(workspaceId, authenticatedEmail);

        return workspaceMemberRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Change a member's role
    @Transactional
    public WorkspaceMemberResponse updateMemberRole(
            Long workspaceId,
            Long userId,
            WorkspaceRole newRole,
            String authenticatedEmail) {

        WorkspaceMember caller = requireManager(workspaceId, authenticatedEmail);

        if (newRole == null || newRole == WorkspaceRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid workspace role");
        }

        WorkspaceMember target = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace member not found"));

        if (target.getRole() == WorkspaceRole.OWNER) {
            throw new AccessDeniedException(
                    "OWNER role cannot be changed");
        }

        if (caller.getRole() == WorkspaceRole.ADMIN
                && target.getRole() != WorkspaceRole.MEMBER) {
            throw new AccessDeniedException(
                    "ADMIN can manage MEMBER accounts only");
        }

        if (caller.getRole() == WorkspaceRole.ADMIN
                && newRole != WorkspaceRole.MEMBER) {
            throw new AccessDeniedException(
                    "ADMIN cannot assign ADMIN role");
        }

        target.setRole(newRole);

        return toResponse(workspaceMemberRepository.save(target));
    }

    // Remove a member
    @Transactional
    public void removeMember(
            Long workspaceId,
            Long userId,
            String authenticatedEmail) {

        WorkspaceMember caller = requireManager(workspaceId, authenticatedEmail);

        if (caller.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot remove yourself from the workspace");
        }

        WorkspaceMember target = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace member not found"));

        if (target.getRole() == WorkspaceRole.OWNER) {
            throw new AccessDeniedException(
                    "OWNER cannot be removed");
        }

        if (caller.getRole() == WorkspaceRole.ADMIN
                && target.getRole() != WorkspaceRole.MEMBER) {
            throw new AccessDeniedException(
                    "ADMIN can remove MEMBER accounts only");
        }

        workspaceMemberRepository.delete(target);
    }

    // Verify that the workspace exists
    private Workspace getWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace not found with id: " + workspaceId));
    }

    // Verify that the caller belongs to the workspace
    private WorkspaceMember requireMembership(
            Long workspaceId,
            String authenticatedEmail) {

        CodePilotUser user = getAuthenticatedUser(authenticatedEmail);

        return workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You are not a member of this workspace"));
    }

    // Verify that the caller is OWNER or ADMIN
    private WorkspaceMember requireManager(
            Long workspaceId,
            String authenticatedEmail) {

        WorkspaceMember caller =
                requireMembership(workspaceId, authenticatedEmail);

        if (caller.getRole() != WorkspaceRole.OWNER
                && caller.getRole() != WorkspaceRole.ADMIN) {
            throw new AccessDeniedException(
                    "Only OWNER or ADMIN can manage workspace members");
        }

        return caller;
    }

    private CodePilotUser getAuthenticatedUser(String email) {
        return codePilotUserRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));
    }

    private WorkspaceMemberResponse toResponse(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}