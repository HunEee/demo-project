// 회원가입
export interface SignupRequest {
  username: string;
  password: string;
  email: string;
  verificationCode: string;
  nickname: string;
  profileImage?: string;
}

// username 중복 체크
export interface CheckUsernameRequest {
  username: string;
}

// 유저 수정
export interface UpdateUserRequest {
  nickname?: string;
  profileImage?: string;
}

// 아이디 찾기
export interface FindUsernameRequest {
  email: string;
  verificationCode: string;
}

// ====================================================================================================================
// 유저 응답
export interface UserResponse {
  id: number;
  username: string;
  email: string;
  nickname: string;
  profileImage?: string;
}
