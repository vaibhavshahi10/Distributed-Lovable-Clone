package com.vaibhav.distributed_lovable.workspace_service.service;

import com.vaibhav.distributed_lovable.common_lib.dto.FileTreeDto;

public interface ProjectFileService {
    FileTreeDto getFileTree(Long projectId);

    String getFileContent(Long projectId, String path);

    void saveFiles(Long projectId, String filePath, String fileContent);
}
