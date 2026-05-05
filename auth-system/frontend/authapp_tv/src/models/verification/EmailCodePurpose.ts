export const EmailCodePurpose = {
  SIGNUP: "SIGNUP",
  RESET_PASSWORD: "RESET_PASSWORD",
  FIND_USERNAME: "FIND_USERNAME",
} as const;

export type EmailCodePurpose =
  (typeof EmailCodePurpose)[keyof typeof EmailCodePurpose];