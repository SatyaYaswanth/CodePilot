package com.codepilot.springbootbasics.controller;

import com.codepilot.springbootbasics.dto.AddWorkspaceMemberRequest;
import com.codepilot.springbootbasics.dto.WorkspaceMemberResponse;
import com.codepilot.springbootbasics.entity.CodePilotUser;
import com.codepilot.springbootbasics.entity.WorkspaceRole;
import com.codepilot.springbootbasics.service.WorkspaceMemberService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/members")
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;

    public WorkspaceMemberController(
            WorkspaceMemberService workspaceMemberService) {
        this.workspaceMemberService = workspaceMemberService;
    }

    // Add a member
    @PostMapping
    public ResponseEntity<WorkspaceMemberResponse> addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request,
            Authentication authentication) {

        CodePilotUser user =
                (CodePilotUser) authentication.getPrincipal();

        WorkspaceMemberResponse response =
                workspaceMemberService.addMember(
                        workspaceId,
                        request,
                        user.getEmail()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // List workspace members
    @GetMapping
    public ResponseEntity<List<WorkspaceMemberResponse>> getMembers(
            @PathVariable Long workspaceId,
            Authentication authentication) {

        CodePilotUser user =
                (CodePilotUser) authentication.getPrincipal();

        return ResponseEntity.ok(
                workspaceMemberService.getMembers(
                        workspaceId,
                        user.getEmail()
                )
        );
    }

    // Update a member's role
    @PatchMapping("/{userId}")
    public ResponseEntity<WorkspaceMemberResponse> updateMemberRole(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            @RequestBody WorkspaceRole role,
            Authentication authentication) {

        CodePilotUser user =
                (CodePilotUser) authentication.getPrincipal();

        return ResponseEntity.ok(
                workspaceMemberService.updateMemberRole(
                        workspaceId,
                        userId,
                        role,
                        user.getEmail()
                )
        );
    }

    // Remove a member
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            Authentication authentication) {

        CodePilotUser user =
                (CodePilotUser) authentication.getPrincipal();

        workspaceMemberService.removeMember(
                workspaceId,
                userId,
                user.getEmail()
        );

        return ResponseEntity.noContent().build();
    }
}