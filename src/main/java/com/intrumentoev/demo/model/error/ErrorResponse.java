package com.intrumentoev.demo.model.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse {

    private ErrorDetail error;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDetail {
        private String code;
        private String message;
        private String target;
        private List<ErrorDetailItem> details;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDetailItem {
        private String code;
        private String target;
        private String message;
    }
}