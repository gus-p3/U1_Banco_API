package com.intrumentoev.demo.entity.gestopago;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Table(name="\"producto\"")
@Entity
@Getter
@Setter
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="\"Id\"")
    private Integer Id;

    @Column(name="\"producto\"")
    private String producto;

    @Column(name="\"servicio\"")
    private String servicio;

    @Column(name="\"idServicio\"")
    private int idServicio;

    @Column(name="\"idProducto\"")
    private int idProducto;

    @Column(name="\"idCatTipoServicio\"")
    private int idCatTipoServicio;

    @Column(name="\"tipoFront\"")
    private int tipoFront;

    @Column(name="\"hasDigitoVerificador\"")
    private boolean hasDigitoVerificador;

    @Column(name="\"tipoReferencia\"")
    private String tipoReferencia;

    @Column(name="\"precio\"")
    private String precio;

    @Column(name="\"showAyuda\"")
    private boolean showAyuda;

    @Column(name="\"createdAt\"")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
    }
}
