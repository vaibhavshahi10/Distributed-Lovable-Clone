package com.vaibhav.distributed_lovable.workspace_service.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMemberId {

    Long projectId;
    Long userId;
}
