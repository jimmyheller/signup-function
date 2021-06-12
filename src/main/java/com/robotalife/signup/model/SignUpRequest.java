package com.robotalife.signup.model;

import lombok.Data;

@Data
public class SignUpRequest {
    String username;
    String email;
    String password;
}
