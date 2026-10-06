// DTOs mirroring the forgeflow-api response/request records exactly.

export type MembershipRole = 'MEMBER' | 'ADMIN' | 'OWNER';

export type PipelineStatus =
  | 'PENDING'
  | 'QUEUED'
  | 'RUNNING'
  | 'RETRY_SCHEDULED'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED';

export function isTerminalStatus(status: PipelineStatus): boolean {
  return status === 'SUCCEEDED' || status === 'FAILED' || status === 'CANCELLED';
}

export interface UserSummary {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: UserSummary;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface OrganizationResponse {
  id: string;
  name: string;
  slug: string;
  yourRole: MembershipRole;
  createdAt: string;
}

export interface CreateOrganizationRequest {
  name: string;
  slug: string;
}

export interface MembershipResponse {
  userId: string;
  email: string;
  displayName: string;
  role: MembershipRole;
  since: string;
}

export interface AddMemberRequest {
  email: string;
  role: MembershipRole;
}

export interface ProjectResponse {
  id: string;
  organizationId: string;
  name: string;
  slug: string;
  createdAt: string;
}

export interface CreateProjectRequest {
  name: string;
  slug: string;
}

export interface RepositoryResponse {
  id: string;
  projectId: string;
  githubOwner: string;
  githubRepo: string;
  connected: boolean;
  createdAt: string;
}

export interface ConnectRepositoryRequest {
  githubOwner: string;
  githubRepo: string;
}

export interface ConnectRepositoryResponse {
  repository: RepositoryResponse;
  webhookSecret: string;
}

export interface PipelineResponse {
  id: string;
  repositoryId: string;
  githubOwner: string;
  githubRepo: string;
  commitSha: string;
  branch: string;
  status: PipelineStatus;
  workerId: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  exitCode: number | null;
  retryCount: number;
  createdAt: string;
}

export interface PipelineDetailResponse extends PipelineResponse {
  logTail: string | null;
  fullLogAvailable: boolean;
}

export interface LogUrlResponse {
  url: string;
  expiresInSeconds: number;
}
