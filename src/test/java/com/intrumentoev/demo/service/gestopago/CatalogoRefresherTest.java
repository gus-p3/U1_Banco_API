package com.intrumentoev.demo.service.gestopago;

import com.intrumentoev.demo.service.impl.gestopago.CatalogoRefresher;
import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoRefresherTest {

    @Mock
    private ProductoServiceRedis productoServiceRedis;

    @InjectMocks
    private CatalogoRefresher catalogoRefresher;

    @Test
    @DisplayName("Refresca catálogo en segundo plano invocando sincronizarCatalogo")
    void testRefrescarEnSegundoPlano() {
        catalogoRefresher.refrescarEnSegundoPlano();
        verify(productoServiceRedis).sincronizarCatalogo();
    }

    @Test
    @DisplayName("Maneja excepciones al refrescar en segundo plano sin propagar error")
    void testRefrescarEnSegundoPlano_ConError() {
        doThrow(new RuntimeException("Redis down")).when(productoServiceRedis).sincronizarCatalogo();
        catalogoRefresher.refrescarEnSegundoPlano();
        verify(productoServiceRedis).sincronizarCatalogo();
    }
}

