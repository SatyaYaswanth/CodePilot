
package com.codepilot.springbootbasics.service;

import com.codepilot.springbootbasics.dto.AddWorkspaceMemberRequest;
import com.codepilot.springbootbasics.dto.WorkspaceMemberResponse;
import com.codepilot.springbootbasics.entity.CodePilotUser;
import com.codepilot.springbootbasics.entity.Workspace;
import com.codepilot.springbootbasics.entity.WorkspaceMember;
import com.codepilot.springbootbasics.entity.WorkspaceRole;
import com.codepilot.springbootbasics.exception.ResourceNotFoundException;
import com.codepilot.springbootbasics.repository.CodePilotUserRepository;
import com.codepilot.springbootbasics.repository.WorkspaceMemberRepository;
import com.codepilot.springbootbasics.repository.WorkspaceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceMemberServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private CodePilotUserRepository codePilotUserRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private WorkspaceMemberService workspaceMemberService;

    private CodePilotUser owner;
    private CodePilotUser admin;
    private CodePilotUser member;
    private Workspace workspace;

    @BeforeEach
    void setUp() throws Exception {
        owner = createUser(1L, "owner@example.com");
        admin = createUser(2L, "admin@example.com");
        member = createUser(3L, "member@example.com");

        workspace = new Workspace("Test Workspace", owner);
        setGeneratedId(workspace, 10L);
    }

    private CodePilotUser createUser(Long id, String email)
            throws Exception {
        CodePilotUser user = new CodePilotUser(
                "Test User",
                email,
                "hashed-password",
                "USER"
        );
        setGeneratedId(user, id);
        return user;
    }

    private void setGeneratedId(Object entity, Long id)
            throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    private WorkspaceMember createMembership(
            CodePilotUser user,
            WorkspaceRole role) {
        return new WorkspaceMember(workspace, user, role);
    }

    private void mockCallerMembership(
            String email,
            CodePilotUser user,
            WorkspaceRole role) {

        when(codePilotUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(10L, user.getId()))
                .thenReturn(Optional.of(createMembership(user, role)));
    }

    private AddWorkspaceMemberRequest addRequest(
            String email,
            WorkspaceRole role) {
        AddWorkspaceMemberRequest request =
                new AddWorkspaceMemberRequest();
        request.setEmail(email);
        request.setRole(role);
        return request;
    }

    @Test
    void addMember_shouldAddMember_whenCallerIsOwner()
            throws Exception {

        mockCallerMembership(
                owner.getEmail(), owner, WorkspaceRole.OWNER);

        when(workspaceRepository.findById(10L))
                .thenReturn(Optional.of(workspace));

        when(codePilotUserRepository.findByEmail(member.getEmail()))
                .thenReturn(Optional.of(member));

        when(workspaceMemberRepository
                .existsByWorkspaceIdAndUserId(10L, 3L))
                .thenReturn(false);

        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceMemberResponse response =
                workspaceMemberService.addMember(
                        10L,
                        addRequest(member.getEmail(), WorkspaceRole.MEMBER),
                        owner.getEmail()
                );

        assertEquals(3L, response.getUserId());
        assertEquals(member.getEmail(), response.getEmail());
        assertEquals(WorkspaceRole.MEMBER, response.getRole());

        verify(workspaceMemberRepository)
                .save(any(WorkspaceMember.class));
    }

    @Test
    void addMember_shouldAllowOwnerToAddAdmin() {

        mockCallerMembership(
                owner.getEmail(), owner, WorkspaceRole.OWNER);

        when(workspaceRepository.findById(10L))
                .thenReturn(Optional.of(workspace));

        when(codePilotUserRepository.findByEmail(admin.getEmail()))
                .thenReturn(Optional.of(admin));

        when(workspaceMemberRepository
                .existsByWorkspaceIdAndUserId(10L, 2L))
                .thenReturn(false);

        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceMemberResponse response =
                workspaceMemberService.addMember(
                        10L,
                        addRequest(admin.getEmail(), WorkspaceRole.ADMIN),
                        owner.getEmail()
                );

        assertEquals(WorkspaceRole.ADMIN, response.getRole());
    }

    @Test
    void addMember_shouldRejectAdminAddingAnotherAdmin() {

        mockCallerMembership(
                admin.getEmail(), admin, WorkspaceRole.ADMIN);

        assertThrows(
                AccessDeniedException.class,
                () -> workspaceMemberService.addMember(
                        10L,
                        addRequest(
                                member.getEmail(),
                                WorkspaceRole.ADMIN),
                        admin.getEmail()
                )
        );

        verify(workspaceRepository, never()).findById(any());
    }

    @Test
    void addMember_shouldRejectDuplicateMembership() {

        mockCallerMembership(
                owner.getEmail(), owner, WorkspaceRole.OWNER);

        when(workspaceRepository.findById(10L))
                .thenReturn(Optional.of(workspace));

        when(codePilotUserRepository.findByEmail(member.getEmail()))
                .thenReturn(Optional.of(member));

        when(workspaceMemberRepository
                .existsByWorkspaceIdAndUserId(10L, 3L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> workspaceMemberService.addMember(
                        10L,
                        addRequest(member.getEmail(), WorkspaceRole.MEMBER),
                        owner.getEmail()
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(workspaceMemberRepository, never())
                .save(any(WorkspaceMember.class));
    }

    @Test
    void getMembers_shouldReturnMembers_whenCallerIsMember() {

        mockCallerMembership(
                member.getEmail(), member, WorkspaceRole.MEMBER);

        WorkspaceMember ownerMembership =
                createMembership(owner, WorkspaceRole.OWNER);

        WorkspaceMember memberMembership =
                createMembership(member, WorkspaceRole.MEMBER);

        when(workspaceMemberRepository.findByWorkspaceId(10L))
                .thenReturn(List.of(ownerMembership, memberMembership));

        List<WorkspaceMemberResponse> responses =
                workspaceMemberService.getMembers(
                        10L, member.getEmail());

        assertEquals(2, responses.size());
        assertEquals(owner.getEmail(), responses.get(0).getEmail());
        assertEquals(member.getEmail(), responses.get(1).getEmail());
    }

    @Test
    void getMembers_shouldRejectNonMember() {

        when(codePilotUserRepository.findByEmail(member.getEmail()))
                .thenReturn(Optional.of(member));

        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(10L, 3L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> workspaceMemberService.getMembers(
                        10L, member.getEmail())
        );

        verify(workspaceMemberRepository, never())
                .findByWorkspaceId(10L);
    }

    @Test
    void updateMemberRole_shouldRejectChangingOwnerRole() {

        mockCallerMembership(
                owner.getEmail(), owner, WorkspaceRole.OWNER);

        WorkspaceMember ownerMembership =
                createMembership(owner, WorkspaceRole.OWNER);

        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(ownerMembership));

        assertThrows(
                AccessDeniedException.class,
                () -> workspaceMemberService.updateMemberRole(
                        10L,
                        1L,
                        WorkspaceRole.MEMBER,
                        owner.getEmail())
        );

        verify(workspaceMemberRepository, never())
                .save(any(WorkspaceMember.class));
    }

    @Test
void removeMember_shouldRejectRemovingOwner() {

    mockCallerMembership(
            owner.getEmail(), owner, WorkspaceRole.OWNER);

    WorkspaceMember anotherOwner =
            createMembership(admin, WorkspaceRole.OWNER);

    when(workspaceMemberRepository
            .findByWorkspaceIdAndUserId(10L, 2L))
            .thenReturn(Optional.of(anotherOwner));

    assertThrows(
            AccessDeniedException.class,
            () -> workspaceMemberService.removeMember(
                    10L, 2L, owner.getEmail())
    );

    verify(workspaceMemberRepository, never())
            .delete(any(WorkspaceMember.class));
}

    @Test
    void removeMember_shouldRejectAdminRemovingAnotherAdmin() {

        mockCallerMembership(
                admin.getEmail(), admin, WorkspaceRole.ADMIN);

        WorkspaceMember targetAdmin =
                createMembership(owner, WorkspaceRole.ADMIN);

        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(targetAdmin));

        assertThrows(
                AccessDeniedException.class,
                () -> workspaceMemberService.removeMember(
                        10L, 1L, admin.getEmail())
        );

        verify(workspaceMemberRepository, never())
                .delete(any(WorkspaceMember.class));
    }

    @Test
    void updateMemberRole_shouldThrowWhenTargetMemberNotFound() {

        mockCallerMembership(
                owner.getEmail(), owner, WorkspaceRole.OWNER);

        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(10L, 3L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> workspaceMemberService.updateMemberRole(
                        10L,
                        3L,
                        WorkspaceRole.ADMIN,
                        owner.getEmail())
        );
    }
}
