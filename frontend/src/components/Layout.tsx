import { Link, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export function Layout() {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/organizations" className="brand">
          ForgeFlow
        </Link>
        <nav className="app-nav">
          {user && (
            <>
              <span className="app-user">{user.displayName}</span>
              <button className="btn-link" onClick={logout}>
                Log out
              </button>
            </>
          )}
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
