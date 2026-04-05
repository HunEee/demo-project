export default interface SecurityStatus {
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
  lastRefreshedAt?: string;
  status: "SAFE" | "WARNING" | "DANGER";
}