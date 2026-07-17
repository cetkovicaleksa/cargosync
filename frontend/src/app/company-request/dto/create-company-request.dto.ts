export interface CreateCompanyRequestDto {
  name: string;
  address: string;
  city: {
    countryCode: string;
    code: string;
  };
  location: {
    latitude: number;
    longitude: number;
  };
}
