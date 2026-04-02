package api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for creating or updating a user.
 *
 * <p>Lombok annotations auto-generate getters, setters, builder, and constructors.
 * Jackson's {@code @JsonIgnoreProperties} prevents failures on unknown response fields.
 *
 * @author Enterprise QA Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String phone;
}
