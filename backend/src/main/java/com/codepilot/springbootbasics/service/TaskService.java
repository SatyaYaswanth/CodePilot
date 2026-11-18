
package com.codepilot.springbootbasics.service;

import com.codepilot.springbootbasics.dto.TaskCreateRequest;
import com.codepilot.springbootbasics.dto.TaskResponse;
import com.codepilot.springbootbasics.entity.CodePilotUser;
import com.codepilot.springbootbasics.entity.Project;
import com.codepilot.springbootbasics.entity.Task;
import com.codepilot.springbootbasics.entity.TaskPriority;
import com.codepilot.springbootbasics.entity.TaskStatus;
import com.codepilot.springbootbasics.exception.UserNotFoundException;
import com.codepilot.springbootbasics.repository.CodePilotUserRepository;
import com.codepilot.springbootbasics.repository.ProjectRepository;
import com.codepilot.springbootbasics.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.codepilot.springbootbasics.exception.ResourceNotFoundException;
import com.codepilot.springbootbasics.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.codepilot.springbootbasics.entity.WorkspaceMember;
import com.codepilot.springbootbasics.entity.WorkspaceRole;
import com.codepilot.springbootbasics.repository.WorkspaceMemberRepository;
import org.springframework.security.access.AccessDeniedException;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final CodePilotUserRepository codePilotUserRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            CodePilotUserRepository codePilotUserRepository,
            WorkspaceMemberRepository workspaceMemberRepository) {

        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.codePilotUserRepository = codePilotUserRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    private Project getAuthorizedProject(
            Long projectId,
            String authenticatedEmail) {

        CodePilotUser user = codePilotUserRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));

        return projectRepository
                .findByIdAndWorkspaceUserId(
                        projectId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found with id: " + projectId));
    }

    @Transactional
    public TaskResponse createTask(
            Long projectId,
            TaskCreateRequest request,
            String authenticatedEmail) {

        Project project = getAuthorizedProject(
                projectId, authenticatedEmail);

        TaskStatus status = request.status() != null
                ? request.status()
                : TaskStatus.TODO;

        TaskPriority priority = request.priority() != null
                ? request.priority()
                : TaskPriority.MEDIUM;

        Task task = new Task(
                request.title(),
                request.description(),
                status,
                priority,
                project
        );

        Task savedTask = taskRepository.save(task);

        return TaskResponse.from(savedTask);
    }


    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getProjectTasks(
            Long projectId,
            String authenticatedEmail,
            int page,
            int size,
            String sortBy,
            String direction,
            TaskStatus status,
            TaskPriority priority) {

        Project project = getAuthorizedProject(
                projectId, authenticatedEmail);

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100");
        }

        if (!List.of(
                "id", "title", "description", "status", "priority"
        ).contains(sortBy)) {
            throw new IllegalArgumentException("Invalid task sort field");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Direction must be 'asc' or 'desc'");
        }

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(sortDirection, sortBy));

        return PageResponse.from(
                taskRepository.findByProjectWithFilters(
                                project.getId(),
                                status,
                                priority,
                                pageable)
                        .map(TaskResponse::from)
        );
    }


    @Transactional(readOnly = true)
    public TaskResponse getTask(
            Long projectId,
            Long taskId,
            String authenticatedEmail) {

        Project project = getAuthorizedProject(
                projectId, authenticatedEmail);

        Task task = taskRepository
                .findByIdAndProject_Id(taskId, project.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found with id: " + taskId));

        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse updateTask(
            Long projectId,
            Long taskId,
            TaskCreateRequest request,
            String authenticatedEmail) {

        Project project = getAuthorizedProject(
                projectId, authenticatedEmail);

        Task task = taskRepository
                .findByIdAndProject_Id(taskId, project.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found with id: " + taskId));

        task.setTitle(request.title());
        task.setDescription(request.description());


        if (request.priority() != null) {
            task.setPriority(request.priority());
        }

        Task updatedTask = taskRepository.save(task);

        return TaskResponse.from(updatedTask);
    }


    @Transactional
    public TaskResponse assignTask(
            Long projectId,
            Long taskId,
            Long assigneeId,
            String authenticatedEmail) {

        // 1. Verify the project belongs to the authenticated user.
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found with id: " + projectId));

        Long workspaceId = project.getWorkspace().getId();

        // 2. Find the authenticated user.
        CodePilotUser currentUser = codePilotUserRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));

        // 3. Verify OWNER or ADMIN membership.
        WorkspaceMember currentMembership = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId, currentUser.getId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You are not a member of this workspace"));

        WorkspaceRole role = currentMembership.getRole();

        if (role != WorkspaceRole.OWNER
                && role != WorkspaceRole.ADMIN) {
            throw new AccessDeniedException(
                    "Only OWNER and ADMIN can assign tasks");
        }

        // 4. Find the task inside the specified project.
        Task task = taskRepository
                .findByIdAndProject_Id(taskId, project.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found with id: " + taskId));

        // 5. Clear assignment if assigneeId is null.
        if (assigneeId == null) {
            task.setAssignee(null);
            return TaskResponse.from(taskRepository.save(task));
        }

        // 6. Find the target user.
        CodePilotUser assignee = codePilotUserRepository
                .findById(assigneeId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Assignee not found with id: " + assigneeId));

        // 7. Verify the target user belongs to the same workspace.
        workspaceMemberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId, assigneeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Assignee must be a member of this workspace"));

        // 8. Assign the task.
        task.setAssignee(assignee);

        Task updatedTask = taskRepository.save(task);

        return TaskResponse.from(updatedTask);
    }



    @Transactional
    public TaskResponse updateTaskDueDate(
            Long projectId,
            Long taskId,
            java.time.LocalDate dueDate,
            String authenticatedEmail) {

        // 1. Find the project.
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found with id: " + projectId));

        // 2. Find the authenticated user.
        CodePilotUser currentUser = codePilotUserRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));

        // 3. Verify workspace membership.
        WorkspaceMember membership = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(
                        project.getWorkspace().getId(),
                        currentUser.getId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You are not a member of this workspace"));

        // 4. Only OWNER and ADMIN can update deadlines.
        WorkspaceRole role = membership.getRole();

        if (role != WorkspaceRole.OWNER
                && role != WorkspaceRole.ADMIN) {
            throw new AccessDeniedException(
                    "Only OWNER and ADMIN can update task deadlines");
        }

        // 5. Find the task within the project.
        Task task = taskRepository
                .findByIdAndProject_Id(taskId, project.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found with id: " + taskId));

        // 6. Update the deadline.
        // A null value removes the existing deadline.
        task.setDueDate(dueDate);

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTaskStatus(
            Long projectId,
            Long taskId,
            TaskStatus newStatus,
            String authenticatedEmail) {

        // 1. Find the project.
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found with id: " + projectId));

        // 2. Find the authenticated user.
        CodePilotUser currentUser = codePilotUserRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));

        // 3. Verify workspace membership.
        workspaceMemberRepository
                .findByWorkspaceIdAndUserId(
                        project.getWorkspace().getId(),
                        currentUser.getId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You are not a member of this workspace"));

        // 4. Find the task inside the project.
        Task task = taskRepository
                .findByIdAndProject_Id(taskId, project.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found with id: " + taskId));

        // 5. Validate the requested status.
        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "New task status cannot be null");
        }

        TaskStatus currentStatus = task.getStatus();

        boolean validTransition =
                (currentStatus == TaskStatus.TODO
                        && newStatus == TaskStatus.IN_PROGRESS)
                        ||
                        (currentStatus == TaskStatus.IN_PROGRESS
                                && newStatus == TaskStatus.COMPLETED)
                        ||
                        (currentStatus == TaskStatus.COMPLETED
                                && newStatus == TaskStatus.IN_PROGRESS);

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Invalid task status transition: "
                            + currentStatus + " -> " + newStatus);
        }

        // 6. Update status.
        task.setStatus(newStatus);

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(
            Long projectId,
            Long taskId,
            String authenticatedEmail) {

        Project project = getAuthorizedProject(
                projectId, authenticatedEmail);

        Task task = taskRepository
                .findByIdAndProject_Id(taskId, project.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found with id: " + taskId));

        taskRepository.delete(task);
    }
}
