package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model;

public enum ClimateZone {
    /** 15–25°C, general dry goods */
    AMBIENT,
    /** 2–8°C, fresh produce, dairy, pharmaceuticals */
    REFRIGERATED,
    /** ≤ −18°C, frozen goods */
    FROZEN,
    /** Precision climate control, electronics, sensitive equipment */
    CONTROLLED,
    /** Flammable or hazardous materials, separate ventilation */
    HAZARDOUS
}
