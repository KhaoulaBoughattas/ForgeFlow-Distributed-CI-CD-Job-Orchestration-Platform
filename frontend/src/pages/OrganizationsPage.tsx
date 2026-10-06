import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient, ApiError } from '../api/client';
import type { CreateOrganizationRequest, OrganizationResponse } from '../api/types';

export function OrganizationsPage() {
  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      const data = await apiClient.get<OrganizationResponse[]>('/api/v1/organizations');
      setOrganizations(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load organizations.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const request: CreateOrganizationRequest = { name, slug };
      await apiClient.post<OrganizationResponse>('/api/v1/organizations', request);
      setName('');
      setSlug('');
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to create organization.');
    }
  }

  return (
    <div className="page">
      <h1>Organizations</h1>
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
        <button type="submit">Create organization</button>
      </form>

      {loading ? (
        <p>Loading…</p>
      ) : (
        <ul className="card-list">
          {organizations.map((org) => (
            <li key={org.id} className="card">
              <Link to={`/organizations/${org.id}/projects`}>
                <strong>{org.name}</strong>
                <span className="muted"> /{org.slug}</span>
              </Link>
              <span className="badge badge-role">{org.yourRole}</span>
            </li>
          ))}
          {organizations.length === 0 && <p className="muted">No organizations yet — create one above.</p>}
        </ul>
      )}
    </div>
  );
}
