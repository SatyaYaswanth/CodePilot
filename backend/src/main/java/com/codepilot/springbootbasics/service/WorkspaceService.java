
package com.codepilot.springbootbasics.service;

import com.codepilot.springbootbasics.dto.WorkspaceCreateRequest;
import com.codepilot.springbootbasics.dto.WorkspaceResponse;
import com.codepilot.springbootbasics.entity.CodePilotUser;
import com.codepilot.springbootbasics.entity.Workspace;
import com.codepilot.springbootbasics.exception.UserNotFoundException;
import com.codepilot.springbootbasics.repository.CodePilotUserRepository;
import com.codepilot.springbootbasics.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.codepilot.springbootbasics.exception.ResourceNotFoundException;
import com.codepilot.springbootbasics.entity.WorkspaceMember;
import com.codepilot.springbootbasics.entity.WorkspaceRole;
import com.codepilot.springbootbasics.repository.WorkspaceMemberRepository;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final CodePilotUserRepository codePilotUserRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            CodePilotUserRepository codePilotUserRepository,
            WorkspaceMemberRepository workspaceMemberRepository) {

        this.workspaceRepository = workspaceRepository;
        this.codePilotUserRepository = codePilotUserRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public WorkspaceResponse createWorkspace(
            WorkspaceCreateRequest request,
            String authenticatedEmail) {

        CodePilotUser user = codePilotUserRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));

        Workspace workspace = new Workspace(
                request.getName(),
                user
        );

        Workspace savedWorkspace = workspaceRepository.save(workspace);

// Automatically register the creator as workspace OWNER
        WorkspaceMember ownerMembership = new WorkspaceMember(
                savedWorkspace,
                user,
                WorkspaceRole.OWNER
        );

        workspaceMemberRepository.save(ownerMembership);

        return new WorkspaceResponse(
                savedWorkspace.getId(),
                savedWorkspace.getName()
        );
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getMyWorkspaces(
            String authenticatedEmail) {

        CodePilotUser user = codePilotUserRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));

        return workspaceRepository.findByUserId(user.getId())
                .stream()
                .map(workspace -> new WorkspaceResponse(
                        workspace.getId(),
                        workspace.getName()
                ))
                .toList();
    }


    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(
            Long workspaceId,
            String authenticatedEmail) {

        CodePilotUser user = codePilotUserRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));

        Workspace workspace = workspaceRepository
                .findByIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace not found with id: " + workspaceId));

        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName()
        );
    }

    @Transactional
    public WorkspaceResponse updateWorkspace(
            Long workspaceId,
            WorkspaceCreateRequest request,
            String authenticatedEmail) {

        CodePilotUser user = codePilotUserRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));

        Workspace workspace = workspaceRepository
                .findByIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace not found with id: " + workspaceId));

        workspace.setName(request.getName());

        Workspace updatedWorkspace =
                workspaceRepository.save(workspace);

        return new WorkspaceResponse(
                updatedWorkspace.getId(),
                updatedWorkspace.getName()
        );
    }

    @Transactional
    public void deleteWorkspace(
            Long workspaceId,
            String authenticatedEmail) {

        CodePilotUser user = codePilotUserRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));

        Workspace workspace = workspaceRepository
                .findByIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace not found with id: " + workspaceId));

        workspaceRepository.delete(workspace);
    }

}
