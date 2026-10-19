package com.vaibhav.distributed_lovable.account_service.dto.auth;

public record AuthResponse(String token, UserProfileResponse userProfileResponse) {
}

// dummy: newAuthResponse("", new UserProfileResponse());
