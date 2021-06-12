package com.robotalife.signup.model;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Builder
@Value
public class User {
    UUID id;
    String username;
    String email;
    String password;
    boolean isConfirmed;
}
