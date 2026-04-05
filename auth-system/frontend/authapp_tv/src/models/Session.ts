export default interface Session {
  id: string;
  ip: string;
  userAgent: string;
  createdAt: string;
  lastAccessAt: string;
  current: boolean;
}