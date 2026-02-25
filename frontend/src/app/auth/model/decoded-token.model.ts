export interface DecodedToken {
  sub: string;
  exp: number;
  iat: number;
  iss: string;
  requiresPasswordChange: boolean;
  authorities: string[];
  email: string;
  avatar: string;
}
