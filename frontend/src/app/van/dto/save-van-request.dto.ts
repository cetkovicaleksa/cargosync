export interface SaveVanRequestDto {
  registrationNumber: string;
  maxPayload: number;
  model: {
    wmi: string;
    vd: string;
  };
  version: number | null;
}
