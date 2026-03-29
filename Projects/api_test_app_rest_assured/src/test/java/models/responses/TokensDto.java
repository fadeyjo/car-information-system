package models.responses;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokensDto {
    private String accessToken;
    private String refreshToken;
}
