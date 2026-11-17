package com.codepilot.springbootbasics.dto;

import com.codepilot.springbootbasics.entity.WorkspaceRole;

import java.time.Instant;

public class WorkspaceMemberResponse {

    private final Long id;
    private final Long userId;
    private final String email;
    private final WorkspaceRole role;
    private final Instant joinedAt;

    public WorkspaceMemberResponse(
            Long id,
            Long userId,
            String email,
            WorkspaceRole role,
            Instant joinedAt) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public WorkspaceRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}