import { FormEvent, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiClient, ApiError } from '../api/client';
import type { ConnectRepositoryResponse, PipelineResponse, RepositoryResponse } from '../api/types';
import { StatusBadge } from '../components/StatusBadge';

const POLL_INTERVAL_MS = 4000;

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const [repositories, setRepositories] = useState<RepositoryResponse[]>([]);
  const [pipelines, setPipelines] = useState<PipelineResponse[]>([]);
  const [githubOwner, setGithubOwner] = useState('');
  const [githubRepo, setGithubRepo] = useState('');
  const [revealedSecret, setRevealedSecret] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const pollRef = useRef<number | undefined>(undefined);

  async function loadRepositories() {
    const data = await apiClient.get<RepositoryResponse[]>(`/api/v1/projects/${projectId}/repositories`);
    setRepositories(data);
  }

  async function loadPipelines() {
    const data = await apiClient.get<PipelineResponse[]>(`/api/v1/projects/${projectId}/pipelines`);
    setPipelines(data);
  }

  useEffect(() => {
    loadRepositories().catch((err) => setError(err instanceof ApiError ? err.message : 'Failed to load repositories.'));
    loadPipelines().catch(() => undefined);

    pollRef.current = window.setInterval(() => {
      loadPipelines().catch(() => undefined);
    }, POLL_INTERVAL_MS);

    return () => {
      if (pollRef.current) {
        window.clearInterval(pollRef.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId]);

  async function handleConnect(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const response = await apiClient.post<ConnectRepositoryResponse>(`/api/v1/projects/${projectId}/repositories`, {
        githubOwner,
        githubRepo,
      });
      setRevealedSecret(response.webhookSecret);
      setGithubOwner('');
      setGithubRepo('');
      await loadRepositories();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to connect repository.');
    }
  }

  return (
    <div className="page">
      <Link to="/organizations" className="back-link">
        ← Organizations
      </Link>
      <h1>Project</h1>
      {error && <p className="form-error">{error}</p>}

      <section>
        <h2>Connect a repository</h2>
        <form className="inline-form" onSubmit={handleConnect}>
          <input
            placeholder="github-owner"
            value={githubOwner}
            onChange={(e) => setGithubOwner(e.target.value)}
            required
          />
          <input
            placeholder="github-repo"
            value={githubRepo}
            onChange={(e) => setGithubRepo(e.target.value)}
            required
          />
          <button type="submit">Connect</button>
        </form>

        {revealedSecret && (
          <div className="secret-banner">
            <strong>Webhook secret (shown once — copy it now):</strong>
            <code>{revealedSecret}</code>
            <p className="muted">
              Configure this as the secret for a GitHub webhook pointed at
              <code> POST /api/v1/webhooks/github</code> on the <code>push</code> event. It cannot be retrieved again.
            </p>
          </div>
        )}

        <ul className="card-list">
          {repositories.map((repo) => (
            <li key={repo.id} className="card">
              <strong>
                {repo.githubOwner}/{repo.githubRepo}
              </strong>
              <span className={repo.connected ? 'badge badge-succeeded' : 'badge badge-pending'}>
                {repo.connected ? 'connected' : 'pending'}
              </span>
            </li>
          ))}
          {repositories.length === 0 && <p className="muted">No repositories connected yet.</p>}
        </ul>
      </section>

      <section>
        <h2>Pipelines</h2>
        <table className="data-table">
          <thead>
            <tr>
              <th>Commit</th>
              <th>Branch</th>
              <th>Status</th>
              <th>Worker</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {pipelines.map((pipeline) => (
              <tr key={pipeline.id}>
                <td>
                  <Link to={`/projects/${projectId}/pipelines/${pipeline.id}`}>{pipeline.commitSha.slice(0, 7)}</Link>
                </td>
                <td>{pipeline.branch}</td>
                <td>
                  <StatusBadge status={pipeline.status} />
                </td>
                <td>{pipeline.workerId ?? '—'}</td>
                <td>{new Date(pipeline.createdAt).toLocaleString()}</td>
              </tr>
            ))}
            {pipelines.length === 0 && (
              <tr>
                <td colSpan={5} className="muted">
                  No pipelines yet — push a commit to a connected repository to trigger one.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  );
}
