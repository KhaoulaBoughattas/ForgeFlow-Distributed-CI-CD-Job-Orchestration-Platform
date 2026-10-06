import { FormEvent, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiClient, ApiError } from '../api/client';
import type { CreateProjectRequest, ProjectResponse } from '../api/types';

export function ProjectsPage() {
  const { organizationId } = useParams<{ organizationId: string }>();
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      const data = await apiClient.get<ProjectResponse[]>(`/api/v1/organizations/${organizationId}/projects`);
      setProjects(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load projects.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [organizationId]);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const request: CreateProjectRequest = { name, slug };
      await apiClient.post<ProjectResponse>(`/api/v1/organizations/${organizationId}/projects`, request);
      setName('');
      setSlug('');
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to create project.');
    }
  }

  return (
    <div className="page">
      <Link to="/organizations" className="back-link">
        ← Organizations
      </Link>
      <h1>Projects</h1>
      {error && <p className="form-error">{error}</p>}

      <form className="inline-form" onSubmit={handleCreate}>
        <input placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} required />
        <input
          placeholder="slug-like-this"
          value={slug}
          onChange={(e) => setSlug(e.target.value)}
          pattern="^[a-z0-9]+(-[a-z0-9]+)*$"
          required
        />
        <button type="submit">Create project</button>
      </form>

      {loading ? (
        <p>Loading…</p>
      ) : (
        <ul className="card-list">
          {projects.map((project) => (
            <li key={project.id} className="card">
              <Link to={`/projects/${project.id}`}>
                <strong>{project.name}</strong>
                <span className="muted"> /{project.slug}</span>
              </Link>
            </li>
          ))}
          {projects.length === 0 && <p className="muted">No projects yet — create one above.</p>}
        </ul>
      )}
    </div>
  );
}
