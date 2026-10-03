package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class CatalogNotFoundException extends BaseBusinessException {

    public CatalogNotFoundException(String catalogName, Number id) {
        super(
            "No se encontró el catálogo '" + catalogName + "' con ID: " + id,
            "CATALOG_NOT_FOUND",
            HttpStatus.UNPROCESSABLE_ENTITY,
            catalogName
        );
    }
}
