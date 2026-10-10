package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class CatalogNotFoundException extends BaseBusinessException {

    public CatalogNotFoundException(String catalogName, Number id) {
        super(
            "No se encontró el catálogo '" + catalogName + "' con ID: " + id,
            "CATALOG_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            catalogName
        );
    }

    public CatalogNotFoundException(String catalogName, String id) {
        super(
            "No se encontró el catálogo '" + catalogName + "' con clave/ID: " + id,
            "CATALOG_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            catalogName
        );
    }
}
