package com.robotalife.signup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.robotalife.signup.model.SignUpRequest;
import com.robotalife.signup.model.User;

import java.util.HashMap;
import java.util.UUID;


public class SignUpHandler implements RequestHandler<SignUpRequest, APIGatewayProxyResponseEvent> {
    private static final String FUNCTION_NAME = "signup";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(SignUpRequest signUpRequest, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setIsBase64Encoded(false);
        response.setStatusCode(400);
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);

        //validate

        //create User
        context.getLogger().log(String.format("received request :[%s] for function:[%s]", signUpRequest, FUNCTION_NAME));
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .password(signUpRequest.getPassword())
                .build();

        //save data
        //return response

        response.setStatusCode(200);
        response.setBody(new Gson().toJson(user));
        return response;
    }
}
