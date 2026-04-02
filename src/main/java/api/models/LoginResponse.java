package api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload from the login endpoint.
 *
 * @author Enterprise QA Team
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {
    private String token;
    private String tokenType;
    private long expiresIn;
    private UserResponse user;
}
