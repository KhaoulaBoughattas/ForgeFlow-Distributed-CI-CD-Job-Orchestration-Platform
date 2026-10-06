import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiClient, ApiError } from '../api/client';
import type { LogUrlResponse, PipelineDetailResponse } from '../api/types';
import { isTerminalStatus } from '../api/types';
import { StatusBadge } from '../components/StatusBadge';

const POLL_INTERVAL_MS = 3000;

export function PipelineDetailPage() {
  const { projectId, pipelineId } = useParams<{ projectId: string; pipelineId: string }>();
  const [pipeline, setPipeline] = useState<PipelineDetailResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const pollRef = useRef<number | undefined>(undefined);

  async function load() {
    try {
      const data = await apiClient.get<PipelineDetailResponse>(
        `/api/v1/projects/${projectId}/pipelines/${pipelineId}`,
      );
      setPipeline(data);
      if (isTerminalStatus(data.status) && pollRef.current) {
        window.clearInterval(pollRef.current);
        pollRef.current = undefined;
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load pipeline.');
    }
  }

  useEffect(() => {
    load();
    pollRef.current = window.setInterval(load, POLL_INTERVAL_MS);
    return () => {
      if (pollRef.current) {
        window.clearInterval(pollRef.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId, pipelineId]);

  async function handleDownload() {
    setDownloadError(null);
    try {
      const { url } = await apiClient.get<LogUrlResponse>(
        `/api/v1/projects/${projectId}/pipelines/${pipelineId}/log-url`,
      );
      window.open(url, '_blank', 'noopener,noreferrer');
    } catch (err) {
      setDownloadError(err instanceof ApiError ? err.message : 'Log not available yet.');
    }
  }

  if (error) {
    return <p className="form-error">{error}</p>;
  }

  if (!pipeline) {
    return <p>Loading…</p>;
  }

  return (
    <div className="page">
      <Link to={`/projects/${projectId}`} className="back-link">
        ← Project
      </Link>
      <h1>
        Pipeline <code>{pipeline.commitSha.slice(0, 7)}</code>
      </h1>
      <div className="pipeline-meta">
        <StatusBadge status={pipeline.status} />
        <span>
          {pipeline.githubOwner}/{pipeline.githubRepo} @ {pipeline.branch}
        </span>
        <span className="muted">worker: {pipeline.workerId ?? '—'}</span>
        <span className="muted">retries: {pipeline.retryCount}</span>
        {pipeline.exitCode !== null && <span className="muted">exit code: {pipeline.exitCode}</span>}
      </div>

      <section>
        <div className="log-header">
          <h2>Log tail</h2>
          <button onClick={handleDownload} disabled={!pipeline.fullLogAvailable}>
            Download full log
          </button>
        </div>
        {downloadError && <p className="form-error">{downloadError}</p>}
        <pre className="log-view">{pipeline.logTail ?? 'No output yet.'}</pre>
      </section>
    </div>
  );
}
