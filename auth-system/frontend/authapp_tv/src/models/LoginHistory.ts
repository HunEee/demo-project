export default interface LoginHistory {
  id: number;
  username: string;
  ip: string;
  userAgent: string;
  device: string;
  location?: string;
  loginAt: string;
  status: "SUCCESS" | "LOGOUT" | "EXPIRED" | "FAILED";
}
