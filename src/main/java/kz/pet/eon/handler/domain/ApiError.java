package kz.pet.eon.handler.domain;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiError {
    Integer status;
    String message;
    String details;
    Date timestamp;
    String path;
}
