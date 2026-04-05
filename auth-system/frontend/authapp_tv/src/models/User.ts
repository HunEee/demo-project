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
  roles: string[];
  securityAlert?: boolean; // 보안 상태
}