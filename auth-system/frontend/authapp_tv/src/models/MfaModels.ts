export type MfaMethodType = "TOTP";
export type MfaPolicy = "OPTIONAL" | "OFF" | "REQUIRED_FOR_ADMIN" | "REQUIRED_FOR_ALL";

export type TotpSetupResponse = {
  methodId: number;
  secret: string;
  otpAuthUri: string;
  qrCodeDataUri: string;
};

export type MfaMethodResponse = {
  id: number;
  type: MfaMethodType;
  enabled: boolean;
  registeredAt: string | null;
  lastUsedAt: string | null;
};

export type AdminMfaUserResponse = {
  username: string;
  email: string | null;
  mfaEnabled: boolean;
  method: MfaMethodType | null;
  registeredAt: string | null;
  lastUsedAt: string | null;
  exceptionActive: boolean;
  exceptionExpiresAt: string | null;
  requiredByPolicy: boolean;
};

export type MfaPolicyResponse = {
  policy: MfaPolicy;
};
