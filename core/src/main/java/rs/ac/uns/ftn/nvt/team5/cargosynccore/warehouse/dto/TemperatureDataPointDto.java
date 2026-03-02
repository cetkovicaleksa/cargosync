package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public record TemperatureDataPointDto(Instant time, Double temperature) {}
