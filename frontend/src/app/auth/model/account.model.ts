import { User } from "../../shared/model/user.model";
import { AccountAuthority } from "../dto/account-authority.type";

export interface Account {
  username: string;
  owner: User;
  role: AccountAuthority;
  authorities: string[];
}