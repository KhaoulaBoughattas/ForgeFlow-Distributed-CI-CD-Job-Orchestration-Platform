import type { PipelineStatus } from '../api/types';

const STATUS_CLASS: Record<PipelineStatus, string> = {
  PENDING: 'badge badge-pending',
  QUEUED: 'badge badge-queued',
  RUNNING: 'badge badge-running',
  RETRY_SCHEDULED: 'badge badge-retry',
  SUCCEEDED: 'badge badge-succeeded',
  FAILED: 'badge badge-failed',
  CANCELLED: 'badge badge-cancelled',
};

export function StatusBadge({ status }: { status: PipelineStatus }) {
  return <span className={STATUS_CLASS[status]}>{status.replace('_', ' ')}</span>;
}
