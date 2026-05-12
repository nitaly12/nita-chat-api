package org.example.chat.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileSettingsForm {

    @Size(max = 80)
    private String displayName;

    @Size(min = 3, max = 30)
    @Pattern(regexp = "^[A-Za-z0-9._]+$",
            message = "username may contain letters, digits, dot and underscore only")
    private String username;

    @Size(max = 160)
    private String bio;

    @Pattern(regexp = "light|dark", message = "theme must be 'light' or 'dark'")
    private String theme;
}
