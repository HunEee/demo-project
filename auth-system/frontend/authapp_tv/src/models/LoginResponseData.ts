import type { MfaMethodType } from "./MfaModels";
import type User from "./User";

export default interface LoginResponseData {
  mfaRequired?: boolean;
  mfaRegistrationRequired?: boolean;
  challengeId?: string;
  mfaExpiresAt?: string;
  availableMethods?: MfaMethodType[];
  accessToken?: string;
  user?: User;
  expiresIn?: number;
}
