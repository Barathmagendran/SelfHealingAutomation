package api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for a user resource returned by the API.
 *
 * @author Enterprise QA Team
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {
    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String phone;
    private String createdAt;
    private String updatedAt;
}
