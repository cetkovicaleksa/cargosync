package rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.influx")
public record InfluxProperties(String url, String token, String org, String bucket) {}
