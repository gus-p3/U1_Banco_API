package com.intrumentoev.demo.service.service.home;

import com.intrumentoev.demo.model.home.HomePatchRequest;
import com.intrumentoev.demo.model.home.HomeRequest;
import com.intrumentoev.demo.model.home.HomeResponse;

public interface HomeService {

    HomeResponse obtenerHomePorId(Long id);

    HomeResponse obtenerHomePorIdClient(Long idClient);

    HomeResponse reemplazarHome(Long id, com.intrumentoev.demo.model.home.HomeUpdateRequest request);

    HomeResponse reemplazarHome(Long id, HomeRequest request);

    HomeResponse actualizarParcialHome(Long id, HomePatchRequest request);

    void eliminarHome(Long id);
}
