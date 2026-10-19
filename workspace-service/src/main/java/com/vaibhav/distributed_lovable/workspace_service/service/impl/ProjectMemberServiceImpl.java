package com.vaibhav.distributed_lovable.workspace_service.service.impl;

import com.vaibhav.distributed_lovable.common_lib.dto.UserDto;
import com.vaibhav.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.vaibhav.distributed_lovable.common_lib.security.AuthUtil;
import com.vaibhav.distributed_lovable.workspace_service.client.AccountClient;
import com.vaibhav.distributed_lovable.workspace_service.dto.member.InviteMemberRequest;
import com.vaibhav.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.vaibhav.distributed_lovable.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.vaibhav.distributed_lovable.workspace_service.entity.Project;
import com.vaibhav.distributed_lovable.workspace_service.entity.ProjectMember;
import com.vaibhav.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.vaibhav.distributed_lovable.workspace_service.mapper.ProjectMemberMapper;
import com.vaibhav.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.vaibhav.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.vaibhav.distributed_lovable.workspace_service.service.ProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {

    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    ProjectMemberMapper projectMemberMapper;
    AuthUtil authUtil;
    AccountClient accountClient;

    @Override
    @PreAuthorize("@security.canViewMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {
       return projectMemberRepository.findByIdProjectId(projectId)
                .stream()
                .map(projectMemberMapper::toProjectMemberResponseFromProjectMember)
                .toList();
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request) {
        Long userId = authUtil.getCurrentUserId();

        Project project = getAccessibleProjectById(projectId, userId);

        UserDto invitee = accountClient.getUserByEmail(request.username()).orElseThrow(
                () -> new ResourceNotFoundException("User", request.username())
        );

        if(invitee.id().equals(userId)){
            throw new RuntimeException("Cannot Invite Yourself!");
        }
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, invitee.id());

        if(projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException("Cannot invite once again!");
        }

        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .role(request.role())
                .invitedAt(Instant.now())
                .build();

        projectMemberRepository.save(projectMember);

        return projectMemberMapper.toProjectMemberResponseFromProjectMember(projectMember);
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request) {
        Long userId = authUtil.getCurrentUserId();

        Project project = getAccessibleProjectById(projectId, userId);

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        ProjectMember projectMember = projectMemberRepository.findById(projectMemberId).orElseThrow();

        projectMember.setRole(request.role());

        projectMemberRepository.save(projectMember);

        return projectMemberMapper.toProjectMemberResponseFromProjectMember(projectMember);
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public void removeProjectMember(Long projectId, Long memberId) {
        Long userId = authUtil.getCurrentUserId();

        Project project = getAccessibleProjectById(projectId, userId);

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        if(!projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException("Member not found in Project!");
        }

        projectMemberRepository.deleteById(projectMemberId);
    }


    ///    Internal methods

    public Project getAccessibleProjectById(Long projectId, Long userId){
        return projectRepository.findAccessibleProjectById(projectId, userId).orElseThrow();
    }
}
