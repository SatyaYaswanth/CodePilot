
package com.codepilot.springbootbasics.dto;

import com.codepilot.springbootbasics.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(

        @NotNull(message = "Task status is required")
        TaskStatus status

) {
}
