package com.robotalife.signup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.robotalife.signup.model.SignUpRequest;
import com.robotalife.signup.model.SignupException;
import com.robotalife.signup.model.User;
import org.springframework.security.crypto.bcrypt.BCrypt;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SignUpHandler implements RequestHandler<SignUpRequest, APIGatewayProxyResponseEvent> {
    private static final String FUNCTION_NAME = "signup";
    private static final String BASIC_INFO_TABLE = "UserBasicInfo";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(SignUpRequest signUpRequest, Context context) {
        context.getLogger().log(String.format("received request :[%s] for function:[%s]", signUpRequest, FUNCTION_NAME));
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        HashMap<String, String> headers = new HashMap<>();
        response.setIsBase64Encoded(false);
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);
        //validate
        if (signUpRequest == null) {
            context.getLogger().log(String.format("[BadRequest] signup request is null for function:[%s]", FUNCTION_NAME));
            throw new SignupException("signup request can not be null");
        }
        String email = validateEmail(signUpRequest.getEmail());
        String username = validateUsername(signUpRequest.getUsername());
        String hashedPassword = hashPassword(signUpRequest.getPassword());
        //create User
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .password(hashedPassword)
                .build();
        //save to database
        Region region = Region.EU_WEST_1;
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(region)
                .build();
        queryForEmail(dynamoDbClient, email, context);
        putItemInTable(dynamoDbClient,
                user, context);
        //return response
        response.setStatusCode(201); //created
        return response;
    }

    public static void queryForEmail(DynamoDbClient dynamoDbClient,
                                     String email, Context context) {
        String partitionKeyName = "Email";
        // Set up an alias for the partition key name in case it's a reserved word
        HashMap<String, String> attrNameAlias = new HashMap<>();
        String partitionAlias = "#a";
        attrNameAlias.put(partitionAlias, partitionKeyName);

        // Set up mapping of the partition name with the value
        HashMap<String, AttributeValue> attrValues =
                new HashMap<>();

        attrValues.put(":" + partitionKeyName, AttributeValue.builder()
                .s(email)
                .build());

        String indexName = "Email-index";
        QueryRequest queryReq = QueryRequest.builder()
                .tableName(BASIC_INFO_TABLE)
                .indexName(indexName)
                .keyConditionExpression(partitionAlias + " = :" + partitionKeyName)
                .expressionAttributeNames(attrNameAlias)
                .expressionAttributeValues(attrValues)
                .build();

        try {
            QueryResponse response = dynamoDbClient.query(queryReq);
            if (response.count() > 0) {
                context.getLogger().log(String.format("email: [%s] is already exist in database%n", email));
                throw new SignupException("[BadRequest] There is already one user with this email in the database");
            }
        } catch (DynamoDbException e) {
            context.getLogger().log(String.format("there was a problem in retrieving the email: [%s] and the message is [%s]"
                    , email, e.getMessage()));
            throw new SignupException("[InternalServerError] unhandled error in connecting to datasource.");
        }

    }

    private void putItemInTable(DynamoDbClient ddb, User user, Context context) {
        var itemValues = new HashMap<String, AttributeValue>();
        itemValues.put("Id", AttributeValue.builder().s(user.getId().toString()).build());
        itemValues.put("Username", AttributeValue.builder().s(user.getUsername()).build());
        itemValues.put("Email", AttributeValue.builder().s(user.getEmail()).build());
        itemValues.put("Password", AttributeValue.builder().s(user.getPassword()).build());
        itemValues.put("IsConfirmed", AttributeValue.builder().bool(user.isConfirmed()).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(BASIC_INFO_TABLE)
                .item(itemValues)
                .build();

        try {
            ddb.putItem(request);
        } catch (ResourceNotFoundException e) {
            context.getLogger().log(String.format("Error: The Amazon DynamoDB table [%s] can't be found.", BASIC_INFO_TABLE));
            throw new SignupException(String.format("[InternalServerError] could not save user to [%s]" +
                    ", ResourceNotFoundException", BASIC_INFO_TABLE));
        } catch (DynamoDbException e) {
            context.getLogger().log(String.format("there was a dynamodb exception in putting item: [%s]", e.getMessage()));
            throw new SignupException(String.format("[InternalServerError] could not save user to [%s]",
                    BASIC_INFO_TABLE));
        }
    }

    private String hashPassword(String password) {
        if (password == null || password.isEmpty() || password.length() < 8) {
            throw new SignupException("[BadRequest] password can not be null, empty or less than 8 characters");
        }
        //Minimum eight characters, at least one uppercase letter, one lowercase letter, one number and one special character:
        String regex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[a-zA-Z]).{8,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(password);
        if (matcher.matches()) {
            return BCrypt.hashpw(password, BCrypt.gensalt());
        }
        throw new SignupException("[BadRequest] password is not strong enough");


    }

    private String validateUsername(String username) {
        if (username == null || username.isEmpty() || username.length() < 4) {
            throw new SignupException("[BadRequest] username can not be null or empty and also less than 3 characters");
        }
        return username;
    }

    private String validateEmail(String email) {
        String regex = "^(.+)@(.+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        if (matcher.matches()) {
            return email;
        }
        throw new SignupException("[BadRequest] email is not valid");
    }

}
