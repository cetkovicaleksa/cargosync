export interface DecodedToken {
  sub: string;
  exp: number;
  iat: number;
  iss: string;
  credentialsNonExpired: boolean;
  authorities: string[];
  email: string;
  avatar: string;
}
