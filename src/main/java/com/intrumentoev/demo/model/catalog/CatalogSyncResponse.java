package com.intrumentoev.demo.model.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogSyncResponse {

    private int estadosSincronizados;
    private int municipiosSincronizados;
    private String mensaje;
    private OffsetDateTime timestamp;
}
