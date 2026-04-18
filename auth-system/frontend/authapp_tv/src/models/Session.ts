export default interface Session {
  id: string;
  ip: string;
  device: string;
  createdAt: string;
  lastAccessAt: string;
  current: boolean;
}