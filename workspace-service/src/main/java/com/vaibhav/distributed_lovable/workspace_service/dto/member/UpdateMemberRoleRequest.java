package com.vaibhav.distributed_lovable.workspace_service.dto.member;

import com.vaibhav.distributed_lovable.common_lib.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull ProjectRole role) {
}
