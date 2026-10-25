package com.intrumentoev.demo.client.gestopago;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProductClient", url = "${gestopago.auth.url}")
public interface GestoPagoProductClient {

    @GetMapping(
            value = "/sistema/service/getProductList.do",
            produces = MediaType.APPLICATION_XML_VALUE
    )
    String getProductListXml(@RequestHeader("Authorization") String bearerToken);
}
