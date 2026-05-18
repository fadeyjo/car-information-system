export interface PersonDto {
  personId: number;
  email: string;
  phone: string;
  lastName: string;
  firstName: string;
  patronymic: string | null | number;
  birth: string;
  driveLicense: string | null | number;
  roleId: number;
  avatarId: number;
}

export interface Person {
  personId: number;
  email: string;
  phone: string;
  lastName: string;
  firstName: string;
  patronymic: string | null | number;
  birth: Date;
  driveLicense: string | null | number;
  roleId: number;
  avatarId: number;
}