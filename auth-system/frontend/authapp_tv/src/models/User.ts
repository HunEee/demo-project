export default interface User {
  id: string;
  username: string;
  nickname?: string;
  email: string;
  enabled: boolean;
  image?: string;
  updatedAt?: string;
  createdAt?: string;
  provider: string;
}