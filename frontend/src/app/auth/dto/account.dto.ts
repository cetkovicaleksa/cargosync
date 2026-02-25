import { AccountAuthority } from './account-authority.type';
import { User } from '../../shared/model/user.model';

export interface AccountDto {
  username: string;
  owner: User;
  role?: AccountAuthority;
  requiresPasswordChange: boolean;
  accountNonLocked: boolean;
  authorities: (string | { authority: string } )[];
}
