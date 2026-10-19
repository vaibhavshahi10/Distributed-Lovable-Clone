package com.vaibhav.distributed_lovable.common_lib.enums;

public enum ChatEventType {
    THOUGHT,                     // thought for 2 sec
    MESSAGE,                     // Standard Conversational text
    FILE_EDIT,                   // Code generation <file>
    TOOL_LOG                     //Reading file ... <tool>
}
