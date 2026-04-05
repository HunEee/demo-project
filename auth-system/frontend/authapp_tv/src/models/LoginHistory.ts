export default interface LoginHistory {
  id: number;
  ip: string;
  userAgent: string;
  loginAt: string;
  location?: string;
}