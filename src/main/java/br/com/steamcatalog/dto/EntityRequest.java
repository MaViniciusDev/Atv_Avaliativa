package br.com.steamcatalog.dto;

/** Corpo das requisicoes de cadastro/alteracao de Developer e Publisher. */
public record EntityRequest(String name, String country) {
}
